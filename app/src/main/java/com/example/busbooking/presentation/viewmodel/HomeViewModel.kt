package com.example.busbooking.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result
import com.example.busbooking.data.relations.TicketDetails
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.launch

data class PopularRoute(
    val origin: String,
    val destination: String
)

data class Banner(
    val imageRes: Int,          // drawable res id
    val title: String,
    val subtitle: String,
    val actionLabel: String = "Xem ngay"
)

data class Promotion(
    val id: Int,
    val title: String,
    val description: String,
    val badgeLabel: String,     // "HOT", "MỚI", "-20%"...
    val expiresLabel: String    // "Còn 2 ngày"
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
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _homeState = MutableLiveData<HomeState>(HomeState.Loading)
    val homeState: LiveData<HomeState> = _homeState

    fun loadHome() {
        _homeState.value = HomeState.Loading

        viewModelScope.launch {
            // 1. Lấy user từ Session (không cần network)
            val currentUser = SessionManager.getCurrentUser()

            // 2. Lấy vé sắp tới (active tickets)
            val upcomingTickets = mutableListOf<TicketDetails>()
            val userId = SessionManager.getCurrentUserId()
            if (userId != null) {
                when (val result = ticketRepository.getUserActiveTickets(userId)) {
                    is Result.Success -> upcomingTickets.addAll(result.data)
                    else -> { /* bỏ qua lỗi, hiển thị rỗng */ }
                }
            }

            // 3. Lấy tuyến phổ biến — ghép origins + destinations
            val popularRoutes = mutableListOf<PopularRoute>()
            val originsResult = routeRepository.getAllOrigins()
            val destsResult   = routeRepository.getAllDestinations()
            if (originsResult is Result.Success && destsResult is Result.Success) {
                val origins = originsResult.data.take(5)
                val dests   = destsResult.data.take(5)
                origins.forEachIndexed { i, origin ->
                    val dest = dests.getOrNull(i) ?: return@forEachIndexed
                    if (origin != dest) popularRoutes.add(PopularRoute(origin, dest))
                }
            }

            // 4. Banners tĩnh (thay bằng API sau nếu cần)
            val banners = listOf(
                Banner(
                    imageRes    = com.example.busbooking.R.drawable.banner_1,
                    title       = "Ưu đãi hè 2026",
                    subtitle    = "Giảm 20% cho tất cả tuyến miền Trung",
                    actionLabel = "Đặt ngay"
                ),
                Banner(
                    imageRes    = com.example.busbooking.R.drawable.banner_2,
                    title       = "Thành viên VIP",
                    subtitle    = "Tích điểm - Nhận ưu đãi mỗi chuyến đi",
                    actionLabel = "Tìm hiểu"
                ),
                Banner(
                    imageRes    = com.example.busbooking.R.drawable.banner_3,
                    title       = "Trung chuyển miễn phí",
                    subtitle    = "Tích điểm - Nhận ưu đãi mỗi chuyến đi",
                    actionLabel = "Đặt ngay"
                )
            )

            // 5. Khuyến mãi tĩnh
            val promotions = listOf(
                Promotion(1, "Flash Sale cuối tuần",  "Giảm 30% tuyến HCM - Đà Lạt",   "HOT",  "Còn 1 ngày"),
                Promotion(2, "Miễn phí hành lý 20kg", "Áp dụng mọi tuyến tháng 7",      "MỚI",  "Còn 5 ngày"),
                Promotion(3, "Combo 2 vé khứ hồi",    "Tiết kiệm thêm 10% khi đặt cặp", "-10%", "Còn 3 ngày")
            )

            _homeState.value = HomeState.Success(
                user           = currentUser,
                upcomingTickets = upcomingTickets,
                popularRoutes  = popularRoutes,
                banners        = banners,
                promotions     = promotions
            )
        }
    }

    fun refresh() = loadHome()
}