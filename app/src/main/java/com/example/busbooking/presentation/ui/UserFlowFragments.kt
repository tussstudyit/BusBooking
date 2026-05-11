package com.example.busbooking.presentation.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.adapter.SeatAdapter
import com.example.busbooking.presentation.ui.state.BookingState
import com.example.busbooking.presentation.ui.state.UserState
import com.example.busbooking.presentation.viewmodel.BookingConfirmationViewModel
import com.example.busbooking.presentation.viewmodel.BookingViewModel
import com.example.busbooking.presentation.viewmodel.RouteSearchViewModel
import com.example.busbooking.presentation.viewmodel.SeatSelectionViewModel
import com.example.busbooking.presentation.viewmodel.UserProfileViewModel
import com.example.busbooking.presentation.viewmodel.UserViewModel
import com.example.busbooking.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// BASE: Tái sử dụng UI loading/empty/error + RecyclerView chung cho ticket list
// ─────────────────────────────────────────────────────────────────────────────

private abstract class BaseTicketListFragment : Fragment() {

    protected val viewModel: UserViewModel by viewModels()

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var progressBar: ProgressBar
    protected lateinit var emptyText: TextView
    protected lateinit var errorText: TextView

    /** Mỗi subclass cung cấp layout ID riêng */
    abstract val layoutResId: Int

    /** ID của RecyclerView trong layout */
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
                is UserState.Loading -> showLoading()
                is UserState.TicketsLoaded -> onTicketsLoaded(state)
                is UserState.Error -> showError(state.message)
                else -> progressBar.visibility = View.GONE
            }
        }

        viewModel.loadMyTickets()
    }

    /** Subclass override để filter/bind adapter theo nghiệp vụ riêng */
    abstract fun onTicketsLoaded(state: UserState.TicketsLoaded)

    // ── Helpers UI ────────────────────────────────────────────────────────────

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
// 1. MyTicketsFragment — Vé đang active, có thể navigate đến chi tiết
// ─────────────────────────────────────────────────────────────────────────────

class MyTicketsFragment : BaseTicketListFragment() {

    override val layoutResId    = R.layout.fragment_my_tickets
    override val recyclerViewId = R.id.ticketsRecyclerView

    override fun onTicketsLoaded(state: UserState.TicketsLoaded) {
        if (state.tickets.isEmpty()) {
            showEmpty()
        } else {
            showList()
            // TODO: set adapter
            // recyclerView.adapter = TicketAdapter(state.tickets) { ticket ->
            //     val action = MyTicketsFragmentDirections
            //         .actionMyTicketsFragmentToTicketDetailsFragment(ticket.ticket.id)
            //     findNavController().navigate(action)
            // }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. BookingHistoryFragment — Toàn bộ lịch sử, kể cả vé đã hủy (read-only)
// ─────────────────────────────────────────────────────────────────────────────

class BookingHistoryFragment : BaseTicketListFragment() {

    override val layoutResId    = R.layout.fragment_booking_history
    override val recyclerViewId = R.id.historyRecyclerView

    override fun onTicketsLoaded(state: UserState.TicketsLoaded) {
        if (state.tickets.isEmpty()) {
            showEmpty()
        } else {
            showList()
            // TODO: set adapter — hiển thị tất cả lịch sử, kể cả đã hủy
            // recyclerView.adapter = TicketAdapter(state.tickets)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. TicketDetailsFragment — Chi tiết 1 vé + hủy vé
//    (Dùng BookingViewModel riêng nên không extend base)
// ─────────────────────────────────────────────────────────────────────────────

class TicketDetailsFragment : Fragment() {

    private val viewModel: BookingViewModel by viewModels()
    private val args: TicketDetailsFragmentArgs by navArgs()

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

        viewModel.loadTicket(args.ticketId)
    }

    private fun bindTicket(state: BookingState.TicketLoaded) {
        progressBar.visibility = View.GONE
        errorText.visibility   = View.GONE

        val ticket = state.ticket
        routeText.text  = "${ticket.originCity} → ${ticket.destinationCity}"
        dateText.text   = ticket.tripDate.toString()
        seatText.text   = "Ghế: ${ticket.seatNumber}"
        statusText.text = "Trạng thái: ${ticket.status}"
        priceText.text  = "Giá: ${ticket.price} VNĐ"

        val cancellable = ticket.status == "CONFIRMED" || ticket.status == "PENDING"
        cancelButton.isEnabled  = cancellable
        cancelButton.visibility = if (cancellable) View.VISIBLE else View.GONE
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Hủy vé")
            .setMessage("Bạn có chắc muốn hủy vé này không?")
            .setPositiveButton("Hủy vé") { _, _ -> viewModel.cancelTicket(args.ticketId) }
            .setNegativeButton("Không", null)
            .show()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. UserDashboardFragment — Màn hình chính của user, điều hướng các chức năng
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
// 5. UserProfileFragment — Xem và chỉnh sửa thông tin cá nhân + đăng xuất
// ─────────────────────────────────────────────────────────────────────────────

class UserProfileFragment : Fragment() {

    private val viewModel: UserProfileViewModel by viewModels()

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
            emailInput.isEnabled = false // Email không cho sửa
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
                R.id.loginFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.userDashboardFragment, true).build()
            )
        }

        viewModel.loadProfile(SessionManager.getCurrentUserId())
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. RouteSearchFragment — Tìm kiếm tuyến đường theo điểm đi, đến và ngày
// ─────────────────────────────────────────────────────────────────────────────

class RouteSearchFragment : Fragment() {

    private val viewModel: RouteSearchViewModel by viewModels()

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
                Toast.makeText(requireContext(), "Please enter origin and destination", Toast.LENGTH_SHORT).show()
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
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, origins)
            originInput.setAdapter(adapter)
        }

        viewModel.destinations.observe(viewLifecycleOwner) { destinations ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, destinations)
            destinationInput.setAdapter(adapter)
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
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateButton.text = sdf.format(Date(selectedDate))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. SeatSelectionFragment — Chọn ghế cho chuyến đi, xác nhận đặt vé
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

        tripId = arguments?.getLong("tripId") ?: -1L
        if (tripId == -1L) {
            Toast.makeText(requireContext(), "Invalid trip", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val seatsRecyclerView: RecyclerView = view.findViewById(R.id.seatsRecyclerView)
        val selectedSeatText: TextView      = view.findViewById(R.id.selectedSeatText)
        val confirmButton: Button           = view.findViewById(R.id.confirmButton)

        adapter = SeatAdapter { seat -> viewModel.selectSeat(seat) }

        seatsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        seatsRecyclerView.adapter = adapter

        viewModel.seats.observe(viewLifecycleOwner) { seats ->
            adapter.submitList(seats)
        }

        viewModel.selectedSeat.observe(viewLifecycleOwner) { seat ->
            selectedSeatText.text = if (seat != null) "Selected: ${seat.seatNumber}" else "No seat selected"
        }

        viewModel.bookingResult.observe(viewLifecycleOwner) { ticketId ->
            ticketId ?: return@observe
            val bundle = Bundle().apply { putLong("ticketId", ticketId) }
            findNavController().navigate(R.id.action_seatSelectionFragment_to_bookingConfirmationFragment, bundle)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        confirmButton.setOnClickListener {
            if (viewModel.selectedSeat.value == null) {
                Toast.makeText(requireContext(), "Please select a seat", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.bookSeat(tripId)
        }

        viewModel.loadSeats(tripId)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. BookingConfirmationFragment — Hiển thị thông tin vé sau khi đặt thành công
// ─────────────────────────────────────────────────────────────────────────────

class BookingConfirmationFragment : Fragment() {

    private val viewModel: BookingConfirmationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_booking_confirmation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ticketId = arguments?.getLong("ticketId") ?: -1L
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

        viewModel.ticket.observe(viewLifecycleOwner) { ticket ->
            ticket ?: return@observe
            ticketIdText.text    = "Ticket #${ticket.ticket.id}"
            routeText.text       = "${ticket.route.origin} → ${ticket.route.destination}"
            seatText.text        = "Seat: ${ticket.seat.seatNumber}"
            priceText.text       = "Price: ${String.format("%,.0f", ticket.trip.price)} VND"
            statusText.text      = "Status: ${ticket.ticket.status}"
            bookingTimeText.text = "Booked: ${sdf.format(Date(ticket.ticket.bookingTime))}"
        }

        viewTicketsButton.setOnClickListener {
            findNavController().navigate(R.id.action_bookingConfirmationFragment_to_myTicketsFragment)
        }

        viewModel.loadTicket(ticketId)
    }
}