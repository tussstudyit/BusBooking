package com.example.busbooking.presentation.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.presentation.adapter.SeatAdapter
import com.example.busbooking.presentation.ui.state.BookingState
import com.example.busbooking.presentation.ui.state.UserState
import com.example.busbooking.presentation.viewmodel.BookingConfirmationViewModel
import com.example.busbooking.presentation.viewmodel.BookingViewModel
import com.example.busbooking.presentation.viewmodel.RouteSearchViewModel
import com.example.busbooking.presentation.viewmodel.SeatSelectionViewModel
import com.example.busbooking.presentation.viewmodel.UserProfileViewModel
import com.example.busbooking.presentation.viewmodel.UserViewModel
import com.example.busbooking.presentation.viewmodel.ViewModelFactory
import com.example.busbooking.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// BASE: Tái sử dụng UI loading/empty/error + RecyclerView chung cho ticket list
// internal thay vì private để subclass public có thể kế thừa
// ─────────────────────────────────────────────────────────────────────────────

internal abstract class BaseTicketListFragment : Fragment() {

    protected val viewModel: UserViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            UserViewModel(
                AuthRepository(db.userDao()),
                TicketRepository(db.ticketDao(), db.seatDao())
            )
        }
    }
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var progressBar: ProgressBar
    protected lateinit var emptyText: TextView
    protected lateinit var errorText: TextView

    abstract val layoutResId: Int
    abstract val recyclerViewId: Int

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layoutResId, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(recyclerViewId)
        progressBar  = view.findViewById(R.id.progressBar)
        emptyText    = view.findViewById(R.id.emptyText)
        errorText    = view.findViewById(R.id.errorText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.userState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UserState.Loading       -> showLoading()
                is UserState.TicketsLoaded -> onTicketsLoaded(state)
                is UserState.Error         -> showError(state.message)
                else                       -> progressBar.visibility = View.GONE
            }
        }

        viewModel.loadMyTickets()
    }

    abstract fun onTicketsLoaded(state: UserState.TicketsLoaded)

    private fun showLoading() {
        progressBar.visibility  = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyText.visibility    = View.GONE
        errorText.visibility    = View.GONE
    }

    protected fun showList() {
        progressBar.visibility  = View.GONE
        errorText.visibility    = View.GONE
        emptyText.visibility    = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    protected fun showEmpty() {
        progressBar.visibility  = View.GONE
        errorText.visibility    = View.GONE
        recyclerView.visibility = View.GONE
        emptyText.visibility    = View.VISIBLE
    }

    private fun showError(message: String) {
        progressBar.visibility  = View.GONE
        recyclerView.visibility = View.GONE
        emptyText.visibility    = View.GONE
        errorText.text          = message
        errorText.visibility    = View.VISIBLE
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. MyTicketsFragment
// ─────────────────────────────────────────────────────────────────────────────

// Dòng 117
internal class MyTicketsFragment : BaseTicketListFragment() {

    override val layoutResId    = R.layout.fragment_my_tickets
    override val recyclerViewId = R.id.ticketsRecyclerView

    override fun onTicketsLoaded(state: UserState.TicketsLoaded) {
        if (state.tickets.isEmpty()) {
            showEmpty()
        } else {
            showList()
            // TODO: set adapter
            // recyclerView.adapter = TicketAdapter(state.tickets) { ticket ->
            //     val bundle = Bundle().apply { putLong("ticketId", ticket.ticket.id) }
            //     findNavController().navigate(
            //         R.id.action_myTicketsFragment_to_ticketDetailsFragment, bundle)
            // }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. BookingHistoryFragment
// ─────────────────────────────────────────────────────────────────────────────

// Dòng 141
internal class BookingHistoryFragment : BaseTicketListFragment() {

    override val layoutResId    = R.layout.fragment_booking_history
    override val recyclerViewId = R.id.historyRecyclerView

    override fun onTicketsLoaded(state: UserState.TicketsLoaded) {
        if (state.tickets.isEmpty()) {
            showEmpty()
        } else {
            showList()
            // TODO: set adapter
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. TicketDetailsFragment — nhận ticketId qua arguments (không dùng Safe Args)
// ─────────────────────────────────────────────────────────────────────────────

class TicketDetailsFragment : Fragment() {

    private val viewModel: BookingViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            BookingViewModel(
                SeatRepository(db.seatDao()),
                TicketRepository(db.ticketDao(), db.seatDao())
            )
        }
    }
    // Đọc ticketId từ Bundle thay vì navArgs() để tránh lỗi Safe Args
    private val ticketId: Long by lazy {
        arguments?.getLong("ticketId", -1L) ?: -1L
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var routeText: TextView
    private lateinit var dateText: TextView
    private lateinit var seatText: TextView
    private lateinit var statusText: TextView
    private lateinit var priceText: TextView
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_ticket_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ticketId == -1L) {
            Toast.makeText(requireContext(), "Vé không hợp lệ", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        progressBar  = view.findViewById(R.id.progressBar)
        errorText    = view.findViewById(R.id.errorText)
        routeText    = view.findViewById(R.id.routeText)
        dateText     = view.findViewById(R.id.dateText)
        seatText     = view.findViewById(R.id.seatText)
        statusText   = view.findViewById(R.id.statusText)
        priceText    = view.findViewById(R.id.priceText)
        cancelButton = view.findViewById(R.id.cancelButton)

        cancelButton.setOnClickListener { showCancelDialog() }

        viewModel.bookingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookingState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    cancelButton.isEnabled = false
                }
                is BookingState.TicketLoaded -> bindTicket(state)
                is BookingState.CancelSuccess -> {
                    progressBar.visibility = View.GONE
                    findNavController().popBackStack()
                }
                is BookingState.Error -> {
                    progressBar.visibility = View.GONE
                    cancelButton.isEnabled = true
                    errorText.text         = state.message
                    errorText.visibility   = View.VISIBLE
                }
                else -> progressBar.visibility = View.GONE
            }
        }

        viewModel.loadTicket(ticketId)
    }

    private fun bindTicket(state: BookingState.TicketLoaded) {
        progressBar.visibility = View.GONE
        errorText.visibility   = View.GONE

        // TicketLoaded chứa TicketDetails — truy cập qua các relation
        val details = state.ticket                          // TicketDetails
        val ticket  = details.ticket                        // Ticket entity
        val trip    = details.tripWithRouteAndBus.trip      // Trip
        val route   = details.tripWithRouteAndBus.route     // Route
        val seat    = details.seat                          // Seat

        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        routeText.text  = "${route.origin} → ${route.destination}"
        dateText.text   = dateFmt.format(Date(trip.tripDate))
        seatText.text   = "Ghế: ${seat.seatNumber}"
        statusText.text = "Trạng thái: ${ticket.status}"
        priceText.text  = "Giá: ${String.format("%,.0f", trip.price)} VNĐ"

        val cancellable = ticket.status == "CONFIRMED" || ticket.status == "PENDING"
        cancelButton.isEnabled  = cancellable
        cancelButton.visibility = if (cancellable) View.VISIBLE else View.GONE
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hủy vé")
            .setMessage("Bạn có chắc muốn hủy vé này không?")
            .setPositiveButton("Hủy vé") { _, _ -> viewModel.cancelTicket(ticketId) }
            .setNegativeButton("Không", null)
            .show()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. UserDashboardFragment
// ─────────────────────────────────────────────────────────────────────────────

class UserDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_user_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nav = findNavController()
        view.findViewById<Button>(R.id.searchRoutesButton).setOnClickListener {
            nav.navigate(R.id.action_userDashboardFragment_to_routeSearchFragment)
        }
        view.findViewById<Button>(R.id.myTicketsButton).setOnClickListener {
            nav.navigate(R.id.action_userDashboardFragment_to_myTicketsFragment)
        }
        view.findViewById<Button>(R.id.bookingHistoryButton).setOnClickListener {
            nav.navigate(R.id.action_userDashboardFragment_to_bookingHistoryFragment)
        }
        view.findViewById<Button>(R.id.profileButton).setOnClickListener {
            nav.navigate(R.id.action_userDashboardFragment_to_userProfileFragment)
        }
        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            SessionManager.clearSession()
            nav.navigate(
                R.id.loginFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.userDashboardFragment, true).build()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. UserProfileFragment
// ─────────────────────────────────────────────────────────────────────────────

class UserProfileFragment : Fragment() {

    private val viewModel: UserProfileViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            UserProfileViewModel(AuthRepository(db.userDao()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameInput    = view.findViewById<EditText>(R.id.nameInput)
        val emailInput   = view.findViewById<EditText>(R.id.emailInput)
        val phoneInput   = view.findViewById<EditText>(R.id.phoneInput)
        val saveButton   = view.findViewById<Button>(R.id.saveButton)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            nameInput.setText(user.name)
            emailInput.setText(user.email)
            phoneInput.setText(user.phone)
            emailInput.isEnabled = false
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { success ->
            success ?: return@observe
            Toast.makeText(
                requireContext(),
                if (success) "Cập nhật thành công" else "Cập nhật thất bại",
                Toast.LENGTH_SHORT
            ).show()
        }

        saveButton.setOnClickListener {
            val name  = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.updateProfile(name, phone)
        }

        logoutButton.setOnClickListener {
            SessionManager.clearSession()
            findNavController().navigate(
                R.id.action_userProfileFragment_to_loginFragment
            )
        }

        viewModel.loadProfile(SessionManager.getCurrentUserId())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. RouteSearchFragment
// ─────────────────────────────────────────────────────────────────────────────

class RouteSearchFragment : Fragment() {

    private val viewModel: RouteSearchViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            RouteSearchViewModel(RouteRepository(db.routeDao()))
        }
    }
    private lateinit var originInput: AutoCompleteTextView
    private lateinit var destinationInput: AutoCompleteTextView
    private lateinit var dateButton: Button
    private lateinit var searchButton: Button

    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_route_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        originInput      = view.findViewById(R.id.originInput)
        destinationInput = view.findViewById(R.id.destinationInput)
        dateButton       = view.findViewById(R.id.dateButton)
        searchButton     = view.findViewById(R.id.searchButton)

        updateDateLabel()
        dateButton.setOnClickListener { showDatePicker() }

        searchButton.setOnClickListener {
            val origin      = originInput.text.toString().trim()
            val destination = destinationInput.text.toString().trim()
            if (origin.isEmpty() || destination.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập điểm đi và điểm đến", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bundle = Bundle().apply {
                putString("origin", origin)
                putString("destination", destination)
                putLong("tripDate", selectedDate)
            }
            findNavController().navigate(R.id.action_routeSearchFragment_to_tripListFragment, bundle)
        }

        viewModel.origins.observe(viewLifecycleOwner) { origins ->
            originInput.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, origins)
            )
        }

        viewModel.destinations.observe(viewLifecycleOwner) { destinations ->
            destinationInput.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, destinations)
            )
        }

        viewModel.loadLocations()
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            cal.set(year, month, day, 0, 0, 0)
            selectedDate = cal.timeInMillis
            updateDateLabel()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateLabel() {
        dateButton.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. SeatSelectionFragment
// ─────────────────────────────────────────────────────────────────────────────

class SeatSelectionFragment : Fragment() {

    private val viewModel: SeatSelectionViewModel by viewModels()
    private lateinit var adapter: SeatAdapter
    private var tripId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_seat_selection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripId = arguments?.getLong("tripId", -1L) ?: -1L
        if (tripId == -1L) {
            Toast.makeText(requireContext(), "Invalid trip", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val seatsRecyclerView: RecyclerView = view.findViewById(R.id.seatsRecyclerView)
        val selectedSeatText: TextView      = view.findViewById(R.id.selectedSeatText)
        val confirmButton: Button           = view.findViewById(R.id.confirmButton)

        // SeatAdapter nhận callback khi user chọn ghế
        adapter = SeatAdapter { seat -> viewModel.selectSeat(seat) }

        seatsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        seatsRecyclerView.adapter = adapter

        viewModel.seats.observe(viewLifecycleOwner) { seats ->
            adapter.submitList(seats)
        }

        viewModel.selectedSeat.observe(viewLifecycleOwner) { seat ->
            adapter.setSelectedSeat(seat)
            selectedSeatText.text = if (seat != null) "Đã chọn: ${seat.seatNumber}" else "Chưa chọn ghế"
        }

        viewModel.bookingResult.observe(viewLifecycleOwner) { newTicketId ->
            newTicketId ?: return@observe
            val bundle = Bundle().apply { putLong("ticketId", newTicketId) }
            findNavController().navigate(R.id.action_seatSelectionFragment_to_bookingConfirmationFragment, bundle)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        confirmButton.setOnClickListener {
            if (viewModel.selectedSeat.value == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn ghế", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.bookSeat(tripId)
        }

        viewModel.loadSeats(tripId)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. BookingConfirmationFragment
// ─────────────────────────────────────────────────────────────────────────────

class BookingConfirmationFragment : Fragment() {

    private val viewModel: BookingConfirmationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_booking_confirmation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ticketId = arguments?.getLong("ticketId", -1L) ?: -1L
        if (ticketId == -1L) {
            Toast.makeText(requireContext(), "Invalid ticket", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val ticketIdText: TextView    = view.findViewById(R.id.ticketIdText)
        val routeText: TextView       = view.findViewById(R.id.routeText)
        val seatText: TextView        = view.findViewById(R.id.seatText)
        val priceText: TextView       = view.findViewById(R.id.priceText)
        val statusText: TextView      = view.findViewById(R.id.statusText)
        val bookingTimeText: TextView = view.findViewById(R.id.bookingTimeText)
        val viewTicketsButton: Button = view.findViewById(R.id.viewTicketsButton)

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        viewModel.ticket.observe(viewLifecycleOwner) { details ->
            details ?: return@observe
            // details là TicketDetails — truy cập qua các relation
            val ticket = details.ticket
            val route  = details.tripWithRouteAndBus.route
            val trip   = details.tripWithRouteAndBus.trip
            val seat   = details.seat

            ticketIdText.text    = "Vé #${ticket.id}"
            routeText.text       = "${route.origin} → ${route.destination}"
            seatText.text        = "Ghế: ${seat.seatNumber}"
            priceText.text       = "Giá: ${String.format("%,.0f", trip.price)} VNĐ"
            statusText.text      = "Trạng thái: ${ticket.status}"
            bookingTimeText.text = "Đặt lúc: ${sdf.format(Date(ticket.bookingTime))}"
        }

        viewTicketsButton.setOnClickListener {
            findNavController().navigate(R.id.action_bookingConfirmationFragment_to_myTicketsFragment)
        }

        viewModel.loadTicket(ticketId)
    }
}