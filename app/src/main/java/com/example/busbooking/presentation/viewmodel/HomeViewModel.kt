package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.R
import com.example.busbooking.data.entity.User
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.data.relations.isUpcomingTicket
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.ApiTicketRepository
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

data class PopularRoute(
    val origin: String,
    val destination: String
)

data class Banner(
    val imageRes: Int,
    val title: String,
    val subtitle: String,
    val actionLabel: String = "Xem ngay"
)

data class Promotion(
    val id: Int,
    val title: String,
    val description: String,
    val badgeLabel: String,
    val expiresLabel: String
)

sealed class HomeState {
    object Loading : HomeState()
    data class Success(
        val user: User?,
        val upcomingTickets: List<TicketDetails>,
        val popularRoutes: List<PopularRoute>,
        val banners: List<Banner>,
        val promotions: List<Promotion>
    ) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val ticketRepository: TicketRepository,
    private val routeRepository: RouteRepository,
    private val ApiTicketRepository: ApiTicketRepository = ApiTicketRepository()
) : ViewModel() {

    private val _homeState = MutableLiveData<HomeState>(HomeState.Loading)
    val homeState: LiveData<HomeState> = _homeState

    fun loadHome() {
        _homeState.value = HomeState.Loading

        viewModelScope.launch {
            val currentUser = SessionManager.getCurrentUser()
            val upcomingTickets = loadUpcomingTickets()
            val popularRoutes = loadPopularRoutes()

            _homeState.value = HomeState.Success(
                user = currentUser,
                upcomingTickets = upcomingTickets,
                popularRoutes = popularRoutes,
                banners = staticBanners(),
                promotions = staticPromotions()
            )
        }
    }

    fun refresh() = loadHome()

    private suspend fun loadUpcomingTickets(): List<TicketDetails> {
        val userId = SessionManager.getCurrentUserId()
        if (userId <= 0L) return emptyList()

        return when (val result = ApiTicketRepository.getUserActiveTickets(userId)) {
            is Result.Success -> result.data.toReminderTickets()
            is Result.Error -> loadLocalUpcomingTickets(userId)
            Result.Loading -> emptyList()
        }
    }

    private suspend fun loadLocalUpcomingTickets(userId: Long): List<TicketDetails> {
        return when (val result = ticketRepository.getUserActiveTickets(userId)) {
            is Result.Success -> result.data.toReminderTickets()
            else -> emptyList()
        }
    }

    private suspend fun loadPopularRoutes(): List<PopularRoute> {
        val originsResult = routeRepository.getAllOrigins()
        val destsResult = routeRepository.getAllDestinations()
        if (originsResult !is Result.Success || destsResult !is Result.Success) return emptyList()

        val origins = originsResult.data.take(5)
        val destinations = destsResult.data.take(5)
        return origins.mapIndexedNotNull { index, origin ->
            val destination = destinations.getOrNull(index) ?: return@mapIndexedNotNull null
            if (origin == destination) null else PopularRoute(origin, destination)
        }
    }

    private fun List<TicketDetails>.toReminderTickets(): List<TicketDetails> {
        val now = System.currentTimeMillis()
        return filter { it.isUpcomingTicket(now) }.sortedWith(
            compareBy<TicketDetails> { it.reminderTime() < now }
                .thenBy { it.reminderTime() }
                .thenByDescending { it.ticket.bookingTime }
        ).take(MAX_REMINDER_TICKETS)
    }

    private fun TicketDetails.reminderTime(): Long {
        val trip = tripWithRouteAndBus.trip
        return when {
            trip.departureTime > 0L -> trip.departureTime
            trip.tripDate > 0L -> trip.tripDate
            else -> Long.MAX_VALUE
        }
    }

    private fun staticBanners(): List<Banner> {
        return listOf(
            Banner(
                imageRes = R.drawable.banner_1,
                title = "\u01afu \u0111\u00e3i h\u00e8 2026",
                subtitle = "Gi\u1ea3m 20% cho t\u1ea5t c\u1ea3 tuy\u1ebfn mi\u1ec1n Trung",
                actionLabel = "\u0110\u1eb7t ngay"
            ),
            Banner(
                imageRes = R.drawable.banner_2,
                title = "Th\u00e0nh vi\u00ean VIP",
                subtitle = "T\u00edch \u0111i\u1ec3m - Nh\u1eadn \u01b0u \u0111\u00e3i m\u1ed7i chuy\u1ebfn \u0111i",
                actionLabel = "T\u00ecm hi\u1ec3u"
            ),
            Banner(
                imageRes = R.drawable.banner_3,
                title = "Trung chuy\u1ec3n mi\u1ec5n ph\u00ed",
                subtitle = "T\u00edch \u0111i\u1ec3m - Nh\u1eadn \u01b0u \u0111\u00e3i m\u1ed7i chuy\u1ebfn \u0111i",
                actionLabel = "\u0110\u1eb7t ngay"
            )
        )
    }

    private fun staticPromotions(): List<Promotion> {
        return listOf(
            Promotion(
                id = 1,
                title = "Flash Sale cu\u1ed1i tu\u1ea7n",
                description = "Gi\u1ea3m 30% tuy\u1ebfn HCM - \u0110\u00e0 L\u1ea1t",
                badgeLabel = "HOT",
                expiresLabel = "C\u00f2n 1 ng\u00e0y"
            ),
            Promotion(
                id = 2,
                title = "Mi\u1ec5n ph\u00ed h\u00e0nh l\u00fd 20kg",
                description = "\u00c1p d\u1ee5ng m\u1ecdi tuy\u1ebfn th\u00e1ng 7",
                badgeLabel = "M\u1edaI",
                expiresLabel = "C\u00f2n 5 ng\u00e0y"
            ),
            Promotion(
                id = 3,
                title = "Combo 2 v\u00e9 kh\u1ee9 h\u1ed3i",
                description = "Ti\u1ebft ki\u1ec7m th\u00eam 10% khi \u0111\u1eb7t c\u1eb7p",
                badgeLabel = "-10%",
                expiresLabel = "C\u00f2n 3 ng\u00e0y"
            )
        )
    }

    private companion object {
        const val MAX_REMINDER_TICKETS = 5
    }
}

