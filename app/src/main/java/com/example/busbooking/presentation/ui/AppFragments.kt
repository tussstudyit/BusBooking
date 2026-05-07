package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.models.BookingResult
import com.example.busbooking.domain.models.Result
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.BusRepository
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.domain.repository.TripRepository
import com.example.busbooking.domain.repository.UserRepository
import com.example.busbooking.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private open class BaseRepoFragment : Fragment() {
    protected val db by lazy { BusBookingDatabase.getInstance(requireContext()) }
    protected val authRepository by lazy { AuthRepository(db.userDao()) }
    protected val userRepository by lazy { UserRepository(db.userDao()) }
    protected val routeRepository by lazy { RouteRepository(db.routeDao()) }
    protected val tripRepository by lazy { TripRepository(db.tripDao(), db.routeDao()) }
    protected val seatRepository by lazy { SeatRepository(db.seatDao()) }
    protected val ticketRepository by lazy { TicketRepository(db.ticketDao(), db.seatDao()) }
    protected val busRepository by lazy { BusRepository(db.busDao()) }

    protected fun screen(title: String): LinearLayout {
        return LinearLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(TextView(context).apply {
                text = title
                textSize = 22f
            })
        }
    }

    protected fun actionButton(text: String, action: () -> Unit): Button {
        return Button(requireContext()).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    protected fun info(text: String): TextView = TextView(requireContext()).apply { this.text = text }
}

class UserDashboardFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("User Dashboard")
        root.addView(actionButton("Search Routes") { findNavController().navigate(R.id.action_userDashboardFragment_to_routeSearchFragment) })
        root.addView(actionButton("My Tickets") { findNavController().navigate(R.id.action_userDashboardFragment_to_myTicketsFragment) })
        root.addView(actionButton("Booking History") { findNavController().navigate(R.id.action_userDashboardFragment_to_bookingHistoryFragment) })
        root.addView(actionButton("Profile") { findNavController().navigate(R.id.action_userDashboardFragment_to_userProfileFragment) })
        root.addView(actionButton("Logout") {
            SessionManager.clearSession()
            findNavController().navigate(R.id.loginFragment)
        })
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class RouteSearchFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Search Trips")
        val originSpinner = Spinner(requireContext())
        val destinationSpinner = Spinner(requireContext())
        val resultList = ListView(requireContext())
        val emptyText = info("Select origin and destination, then search.")
        root.addView(originSpinner)
        root.addView(destinationSpinner)
        root.addView(actionButton("Search") {
            val origin = originSpinner.selectedItem?.toString().orEmpty()
            val destination = destinationSpinner.selectedItem?.toString().orEmpty()
            if (origin.isBlank() || destination.isBlank()) {
                Toast.makeText(context, "Please select route", Toast.LENGTH_SHORT).show()
                return@actionButton
            }
            viewLifecycleOwner.lifecycleScope.launch {
                when (val routeResult = routeRepository.getRouteByOriginDestination(origin, destination)) {
                    is Result.Success -> {
                        when (val tripsResult = tripRepository.getUpcomingTripsForRoute(routeResult.data.id, System.currentTimeMillis())) {
                            is Result.Success -> {
                                if (tripsResult.data.isEmpty()) {
                                    emptyText.text = "No trips found."
                                } else {
                                    val rows = tripsResult.data.map { trip ->
                                        "Trip #${trip.trip.id} | ${trip.route.origin} → ${trip.route.destination} | ${trip.trip.price}"
                                    }
                                    resultList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, rows)
                                    resultList.setOnItemClickListener { _, _, position, _ ->
                                        val tripId = tripsResult.data[position].trip.id
                                        findNavController().navigate(R.id.tripDetailsFragment, bundleOf("tripId" to tripId))
                                    }
                                }
                            }
                            is Result.Error -> emptyText.text = tripsResult.message
                        }
                    }
                    is Result.Error -> emptyText.text = routeResult.message
                }
            }
        })
        root.addView(emptyText)
        root.addView(resultList)

        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val origins = (routeRepository.getAllOrigins() as? Result.Success)?.data.orEmpty()
                val destinations = (routeRepository.getAllDestinations() as? Result.Success)?.data.orEmpty()
                originSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, origins)
                destinationSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, destinations)
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class TripListFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return screen("Trip List").apply { addView(info("Use Search Trips to choose and open trip details.")) }
    }
}

class TripDetailsFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Trip Details")
        val details = info("Loading...")
        root.addView(details)
        root.addView(actionButton("Select Seat") {
            val tripId = arguments?.getLong("tripId", -1L) ?: -1L
            if (tripId > 0) {
                findNavController().navigate(R.id.seatSelectionFragment, bundleOf("tripId" to tripId))
            }
        })
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val tripId = arguments?.getLong("tripId", -1L) ?: -1L
                if (tripId <= 0) {
                    details.text = "Invalid trip."
                    return@launch
                }
                when (val result = tripRepository.getTripById(tripId)) {
                    is Result.Success -> {
                        val t = result.data
                        details.text = "Route: ${t.route.origin} → ${t.route.destination}\nBus: ${t.bus.busName}\nPrice: ${t.trip.price}"
                    }
                    is Result.Error -> details.text = result.message
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class SeatSelectionFragment : BaseRepoFragment() {
    private var selectedSeatId: Long? = null
    private var loadedSeats = emptyList<com.example.busbooking.data.entity.Seat>()

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Seat Selection")
        val list = ListView(requireContext())
        val info = info("Loading seats...")
        root.addView(info)
        root.addView(list)
        root.addView(actionButton("Confirm Booking") {
            val seatId = selectedSeatId
            if (seatId == null || seatId <= 0) {
                Toast.makeText(context, "Please select a seat", Toast.LENGTH_SHORT).show()
                return@actionButton
            }
            val tripId = arguments?.getLong("tripId", -1L) ?: -1L
            val userId = SessionManager.getCurrentUserId()
            if (tripId <= 0 || userId <= 0) return@actionButton
            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = ticketRepository.bookTicket(userId, tripId, seatId)) {
                    is BookingResult.Success -> findNavController().navigate(
                        R.id.bookingConfirmationFragment,
                        bundleOf("ticketId" to result.ticketId)
                    )
                    BookingResult.AlreadyBooked -> Toast.makeText(context, "Seat already booked", Toast.LENGTH_SHORT).show()
                    BookingResult.InvalidSeat -> Toast.makeText(context, "Invalid seat", Toast.LENGTH_SHORT).show()
                    BookingResult.InvalidTrip -> Toast.makeText(context, "Invalid trip", Toast.LENGTH_SHORT).show()
                    is BookingResult.Failure -> Toast.makeText(context, result.exception.message ?: "Booking failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val tripId = arguments?.getLong("tripId", -1L) ?: -1L
                if (tripId <= 0) {
                    info.text = "Invalid trip."
                    return@launch
                }
                when (val seats = seatRepository.getFreeSeatsForTrip(tripId)) {
                    is Result.Success -> {
                        loadedSeats = seats.data
                        val values = loadedSeats.map { "Seat ${it.seatNumber}" }
                        list.choiceMode = ListView.CHOICE_MODE_SINGLE
                        list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_single_choice, values)
                        list.setOnItemClickListener { _, _, position, _ ->
                            list.setItemChecked(position, true)
                            selectedSeatId = loadedSeats[position].id
                        }
                        info.text = "Available seats: ${loadedSeats.size}"
                    }
                    is Result.Error -> info.text = seats.message
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class BookingConfirmationFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Booking Confirmation")
        val details = info("Loading...")
        root.addView(details)
        root.addView(actionButton("Back to My Tickets") { findNavController().navigate(R.id.myTicketsFragment) })
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val ticketId = arguments?.getLong("ticketId", -1L) ?: -1L
                if (ticketId <= 0) {
                    details.text = "Booking completed."
                    return@launch
                }
                when (val result = ticketRepository.getTicketById(ticketId)) {
                    is Result.Success -> {
                        val ticket = result.data
                        details.text = "Ticket #${ticket.ticket.id}\n" +
                            "${ticket.tripWithRouteAndBus.route.origin} → ${ticket.tripWithRouteAndBus.route.destination}\n" +
                            "Seat: ${ticket.seat.seatNumber}\nStatus: ${ticket.ticket.status}"
                    }
                    is Result.Error -> details.text = result.message
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class MyTicketsFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("My Tickets")
        val list = ListView(requireContext())
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val userId = SessionManager.getCurrentUserId()
                if (userId <= 0) return@launch
                when (val result = ticketRepository.getUserActiveTickets(userId)) {
                    is Result.Success -> {
                        val rows = result.data.map { t ->
                            "Ticket #${t.ticket.id} | ${t.tripWithRouteAndBus.route.origin} → ${t.tripWithRouteAndBus.route.destination} | ${t.ticket.status}"
                        }
                        list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, rows)
                        list.setOnItemClickListener { _, _, position, _ ->
                            findNavController().navigate(R.id.ticketDetailsFragment, bundleOf("ticketId" to result.data[position].ticket.id))
                        }
                    }
                    is Result.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class TicketDetailsFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Ticket Details")
        val details = info("Loading...")
        root.addView(details)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val ticketId = arguments?.getLong("ticketId", -1L) ?: -1L
                if (ticketId <= 0) {
                    details.text = "No ticket selected."
                    return@launch
                }
                when (val result = ticketRepository.getTicketById(ticketId)) {
                    is Result.Success -> {
                        val t = result.data
                        details.text = "Ticket #${t.ticket.id}\nUser: ${t.user.email}\nSeat: ${t.seat.seatNumber}\nStatus: ${t.ticket.status}"
                    }
                    is Result.Error -> details.text = result.message
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class BookingHistoryFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Booking History")
        val list = ListView(requireContext())
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val userId = SessionManager.getCurrentUserId()
                if (userId <= 0) return@launch
                val history = db.ticketDao().getUserTicketHistory(userId).first()
                val rows = history.map { "Ticket #${it.ticket.id} - ${it.ticket.status}" }
                list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, rows)
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class UserProfileFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("User Profile")
        val details = info("Loading profile...")
        root.addView(details)
        root.addView(actionButton("Logout") {
            SessionManager.clearSession()
            findNavController().navigate(R.id.action_userProfileFragment_to_loginFragment)
        })
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val userId = SessionManager.getCurrentUserId()
                if (userId <= 0) return@launch
                when (val result = authRepository.getUserProfile(userId)) {
                    is Result.Success -> details.text = "${result.data.name}\n${result.data.email}\n${result.data.phone}\nRole: ${result.data.role}"
                    is Result.Error -> details.text = result.message
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class AdminDashboardFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Admin Dashboard")
        root.addView(actionButton("Routes") { findNavController().navigate(R.id.routeListAdminFragment) })
        root.addView(actionButton("Buses") { findNavController().navigate(R.id.busListAdminFragment) })
        root.addView(actionButton("Trips") { findNavController().navigate(R.id.tripListAdminFragment) })
        root.addView(actionButton("Tickets") { findNavController().navigate(R.id.ticketListAdminFragment) })
        root.addView(actionButton("Users") { findNavController().navigate(R.id.userListAdminFragment) })
        root.addView(actionButton("Analytics") { findNavController().navigate(R.id.analyticsFragment) })
        root.addView(actionButton("Profile") { findNavController().navigate(R.id.adminProfileFragment) })
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class RouteListAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Route Management")
        val list = ListView(requireContext())
        root.addView(actionButton("Create Route") { findNavController().navigate(R.id.routeFormAdminFragment) })
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val routes = db.routeDao().getAllRoutes().first()
                list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, routes.map { "${it.id}: ${it.origin} → ${it.destination}" })
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class RouteFormAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Create Route")
        val origin = EditText(requireContext()).apply { hint = "Origin" }
        val destination = EditText(requireContext()).apply { hint = "Destination" }
        val distance = EditText(requireContext()).apply {
            hint = "Distance (km)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        root.addView(origin)
        root.addView(destination)
        root.addView(distance)
        root.addView(actionButton("Save") {
            viewLifecycleOwner.lifecycleScope.launch {
                val d = distance.text.toString().toIntOrNull() ?: 0
                when (val result = routeRepository.createRoute(origin.text.toString(), destination.text.toString(), d)) {
                    is Result.Success -> {
                        Toast.makeText(context, "Route created", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is Result.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class BusListAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Bus Management")
        val list = ListView(requireContext())
        root.addView(actionButton("Create Bus") { findNavController().navigate(R.id.busFormAdminFragment) })
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val buses = db.busDao().getAllBuses().first()
                list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, buses.map { "${it.id}: ${it.busName} (${it.licensePlate})" })
                list.setOnItemClickListener { _, _, position, _ ->
                    findNavController().navigate(R.id.seatManagementFragment, bundleOf("busId" to buses[position].id))
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class BusFormAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Create Bus")
        val name = EditText(requireContext()).apply { hint = "Bus Name" }
        val seats = EditText(requireContext()).apply {
            hint = "Total Seats"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val plate = EditText(requireContext()).apply { hint = "License Plate" }
        root.addView(name)
        root.addView(seats)
        root.addView(plate)
        root.addView(actionButton("Save") {
            viewLifecycleOwner.lifecycleScope.launch {
                val totalSeats = seats.text.toString().toIntOrNull() ?: 0
                when (val result = busRepository.createBus(name.text.toString(), totalSeats, plate.text.toString())) {
                    is Result.Success -> {
                        Toast.makeText(context, "Bus created", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is Result.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class SeatManagementFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Seat Management")
        val list = ListView(requireContext())
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val busId = arguments?.getLong("busId", -1L) ?: -1L
                if (busId <= 0) return@launch
                val seats = db.seatDao().getSeatsByBus(busId)
                list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, seats.map { "${it.id}: ${it.seatNumber}" })
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class TripListAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Trip Management")
        val list = ListView(requireContext())
        root.addView(actionButton("Create Trip") { findNavController().navigate(R.id.tripFormAdminFragment) })
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val trips = db.tripDao().getAllTrips().first()
                list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, trips.map { "${it.trip.id}: ${it.route.origin} → ${it.route.destination}" })
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class TripFormAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return screen("Trip Form").apply {
            addView(info("Trip creation is available through domain repository and can be expanded from this form."))
        }
    }
}

class TicketListAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Ticket Management")
        val list = ListView(requireContext())
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val tickets = db.ticketDao().getAllTickets().first()
                list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tickets.map { "Ticket #${it.ticket.id} - ${it.ticket.status}" })
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class UserListAdminFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("User Management")
        val list = ListView(requireContext())
        root.addView(list)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                when (val users = userRepository.getAllUsers()) {
                    is Result.Success -> list.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, users.data.map { "${it.id} - ${it.email} (${it.role})" })
                    is Result.Error -> Toast.makeText(context, users.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class AnalyticsFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Analytics")
        val details = info("Loading analytics...")
        root.addView(details)
        viewLifecycleOwnerLiveData.observe(this) {
            it?.lifecycleScope?.launch {
                val confirmed = db.ticketDao().countTicketsByStatus("CONFIRMED")
                val pending = db.ticketDao().countTicketsByStatus("PENDING")
                val cancelled = db.ticketDao().countTicketsByStatus("CANCELLED")
                details.text = "Confirmed: $confirmed\nPending: $pending\nCancelled: $cancelled"
            }
        }
        return ScrollView(requireContext()).apply { addView(root) }
    }
}

class AdminProfileFragment : BaseRepoFragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = screen("Admin Profile")
        root.addView(info("Current role: ${SessionManager.getCurrentUserRole()}"))
        root.addView(actionButton("Logout") {
            SessionManager.clearSession()
            findNavController().navigate(R.id.action_adminProfileFragment_to_loginFragment)
        })
        return ScrollView(requireContext()).apply { addView(root) }
    }
}
