package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.domain.repository.TripRepository
import com.example.busbooking.presentation.adapter.SeatAdapter
import com.example.busbooking.presentation.adapter.TripAdapter
import com.example.busbooking.presentation.viewmodel.SeatSelectionViewModel
import com.example.busbooking.presentation.viewmodel.TripDetailsViewModel
import com.example.busbooking.presentation.viewmodel.TripListViewModel
import com.example.busbooking.presentation.viewmodel.TripListViewModelFactory
import com.example.busbooking.presentation.viewmodel.ViewModelFactory
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// 1. TripListFragment
// ─────────────────────────────────────────────────────────────────────────────

class TripListFragment : Fragment() {

    private val viewModel: TripListViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        TripListViewModelFactory(
            TripRepository(
                tripDAO  = db.tripDao(),
                routeDAO = db.routeDao(),
                seatDAO  = db.seatDao()
            )
        )
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: TripAdapter

    private lateinit var originText: TextView
    private lateinit var destinationText: TextView
    private lateinit var tripDateText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var prevDayButton: ImageButton
    private lateinit var nextDayButton: ImageButton

    private var origin: String = ""
    private var destination: String = ""
    private var currentDate: Long = System.currentTimeMillis()

    private val dateFmt = SimpleDateFormat("EEE, dd/MM/yyyy", Locale("vi"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_trip_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView    = view.findViewById(R.id.tripsRecyclerView)
        emptyText       = view.findViewById(R.id.emptyText)
        originText      = view.findViewById(R.id.originText)
        destinationText = view.findViewById(R.id.destinationText)
        tripDateText    = view.findViewById(R.id.tripDateText)
        backButton      = view.findViewById(R.id.backButton)
        prevDayButton   = view.findViewById(R.id.prevDayButton)
        nextDayButton   = view.findViewById(R.id.nextDayButton)

        adapter = TripAdapter { trip ->
            val bundle = Bundle().apply { putLong("tripId", trip.trip.id) }
            findNavController().navigate(
                R.id.action_tripListFragment_to_tripDetailsFragment, bundle
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        origin      = arguments?.getString("origin") ?: ""
        destination = arguments?.getString("destination") ?: ""
        currentDate = arguments?.getLong("tripDate") ?: System.currentTimeMillis()

        originText.text      = origin
        destinationText.text = destination
        tripDateText.text    = dateFmt.format(Date(currentDate))

        backButton.setOnClickListener { findNavController().popBackStack() }

        prevDayButton.setOnClickListener {
            currentDate -= 86400000L
            tripDateText.text = dateFmt.format(Date(currentDate))
            viewModel.searchTrips(origin, destination, currentDate)
        }

        nextDayButton.setOnClickListener {
            currentDate += 86400000L
            tripDateText.text = dateFmt.format(Date(currentDate))
            viewModel.searchTrips(origin, destination, currentDate)
        }

        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips)
            val isEmpty = trips.isEmpty()
            emptyText.visibility    = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        android.util.Log.d("TripList", "origin='$origin' destination='$destination' date=$currentDate")
        viewModel.searchTrips(origin, destination, currentDate)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. TripDetailsFragment
// ─────────────────────────────────────────────────────────────────────────────

class TripDetailsFragment : Fragment() {

    private val viewModel: TripDetailsViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            TripDetailsViewModel(
                TripRepository(
                    tripDAO  = db.tripDao(),
                    routeDAO = db.routeDao(),
                    seatDAO  = db.seatDao()
                )
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_trip_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tripId = arguments?.getLong("tripId") ?: -1L
        if (tripId == -1L) {
            Toast.makeText(requireContext(), "Chuyến xe không hợp lệ", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val routeText        = view.findViewById<TextView>(R.id.routeText)
        val dateText         = view.findViewById<TextView>(R.id.dateText)
        val departureText    = view.findViewById<TextView>(R.id.departureText)
        val arrivalText      = view.findViewById<TextView>(R.id.arrivalText)
        val priceText        = view.findViewById<TextView>(R.id.priceText)
        val busText          = view.findViewById<TextView>(R.id.busText)
        val statusText       = view.findViewById<TextView>(R.id.statusText)
        val selectSeatButton = view.findViewById<Button>(R.id.selectSeatButton)

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip ?: return@observe
            routeText.text     = "${trip.route.origin} → ${trip.route.destination}"
            dateText.text      = dateFmt.format(Date(trip.trip.tripDate))
            departureText.text = "Khởi hành: ${timeFmt.format(Date(trip.trip.departureTime))}"
            arrivalText.text   = "Đến nơi: ${timeFmt.format(Date(trip.trip.arrivalTime))}"
            priceText.text     = "Giá: ${String.format("%,.0f", trip.trip.price)} VNĐ"
            busText.text       = "Xe: ${trip.bus.busName} (${trip.bus.licensePlate})"
        }

        viewModel.availableSeats.observe(viewLifecycleOwner) { available ->
            when {
                available < 0 -> {
                    statusText.text = "Trạng thái: Đang tải..."
                }
                available == 0 -> {
                    statusText.text = "Trạng thái: Hết chỗ"
                    statusText.setTextColor(
                        requireContext().getColor(android.R.color.holo_red_dark)
                    )
                    selectSeatButton.isEnabled = false
                    selectSeatButton.alpha = 0.5f
                }
                else -> {
                    statusText.text = "Trạng thái: Còn chỗ ($available ghế trống)"
                    statusText.setTextColor(
                        requireContext().getColor(android.R.color.holo_green_dark)
                    )
                    selectSeatButton.isEnabled = true
                    selectSeatButton.alpha = 1.0f
                }
            }
        }

        selectSeatButton.setOnClickListener {
            val currentTrip = viewModel.trip.value
            if (currentTrip != null) {
                val bundle = Bundle().apply {
                    putLong("tripId", tripId)
                    putDouble("tripPrice", currentTrip.trip.price)
                }
                findNavController().navigate(
                    R.id.action_tripDetailsFragment_to_seatSelectionFragment,
                    bundle
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Không tải được dữ liệu chuyến",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.loadTrip(tripId)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. SeatSelectionFragment
// ─────────────────────────────────────────────────────────────────────────────

class SeatSelectionFragment : Fragment() {

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private val viewModel: SeatSelectionViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            SeatSelectionViewModel(
                seatRepository   = SeatRepository(db.seatDao()),
                ticketRepository = TicketRepository(db.ticketDao())
            )
        }
    }

    // ── Views ─────────────────────────────────────────────────────────────────

    private lateinit var seatsFloor1RecyclerView: RecyclerView
    private lateinit var seatsFloor2RecyclerView: RecyclerView
    private lateinit var selectedSeatText: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var confirmButton: MaterialButton

    // ── Adapters ──────────────────────────────────────────────────────────────

    private lateinit var adapterFloor1: SeatAdapter
    private lateinit var adapterFloor2: SeatAdapter

    // ── Args ──────────────────────────────────────────────────────────────────

    private var tripId: Long      = -1L
    private var tripPrice: Double = 0.0

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_seat_selection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripId    = arguments?.getLong("tripId")      ?: -1L
        tripPrice = arguments?.getDouble("tripPrice") ?: 0.0

        if (tripId == -1L) {
            Toast.makeText(requireContext(), "Chuyến xe không hợp lệ", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        bindViews(view)
        setupRecyclerViews()
        observeViewModel()

        viewModel.setTripPrice(tripPrice)
        viewModel.loadSeats(tripId)

        confirmButton.setOnClickListener {
            viewModel.bookSeats(tripId)
        }
    }

    // ── Bind views ────────────────────────────────────────────────────────────

    private fun bindViews(view: View) {
        seatsFloor1RecyclerView = view.findViewById(R.id.seatsFloor1RecyclerView)
        seatsFloor2RecyclerView = view.findViewById(R.id.seatsFloor2RecyclerView)
        selectedSeatText        = view.findViewById(R.id.selectedSeatText)
        totalPriceText          = view.findViewById(R.id.totalPriceText)
        confirmButton           = view.findViewById(R.id.confirmButton)
    }

    // ── Setup RecyclerViews ───────────────────────────────────────────────────

    private fun setupRecyclerViews() {
        adapterFloor1 = SeatAdapter { seat -> viewModel.toggleSeat(seat) }
        seatsFloor1RecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3).also {
                it.spanSizeLookup = adapterFloor1.makeSpanSizeLookup()
            }
            adapter = adapterFloor1
            isNestedScrollingEnabled = false
        }

        adapterFloor2 = SeatAdapter { seat -> viewModel.toggleSeat(seat) }
        seatsFloor2RecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3).also {
                it.spanSizeLookup = adapterFloor2.makeSpanSizeLookup()
            }
            adapter = adapterFloor2
            isNestedScrollingEnabled = false
        }
    }

    // ── Observe ViewModel ─────────────────────────────────────────────────────

    private fun observeViewModel() {

        // Danh sách ghế → tách tầng → submit
        viewModel.seats.observe(viewLifecycleOwner) { allSeats ->
            val floor1 = allSeats
                .filter { it.floor == 1 }
                .sortedBy { it.seatNumber }

            val floor2 = allSeats
                .filter { it.floor == 2 }
                .sortedBy { it.seatNumber }

            adapterFloor1.submitSeats(floor1)
            adapterFloor2.submitSeats(floor2)
        }

        // Ghế đang chọn → update icon + bottom bar
        viewModel.selectedSeats.observe(viewLifecycleOwner) { selected ->
            adapterFloor1.updateSelectedSeats(selected)
            adapterFloor2.updateSelectedSeats(selected)
            selectedSeatText.text = "x${selected.size}"
        }

        // Tổng tiền
        viewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            val formatted = NumberFormat
                .getNumberInstance(Locale("vi", "VN"))
                .format(total.toLong())
            totalPriceText.text = "  $formatted vnđ"
        }

        // Đặt vé thành công
        viewModel.bookingResult.observe(viewLifecycleOwner) { ticketIds ->
            if (!ticketIds.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Đặt vé thành công! Mã vé: ${ticketIds.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()

                // TODO: navigate sang màn xác nhận
                // val bundle = Bundle().apply {
                //     putLongArray("ticketIds", ticketIds.toLongArray())
                // }
                // findNavController().navigate(
                //     R.id.action_seatSelection_to_confirmation, bundle
                // )

                findNavController().popBackStack()
            }
        }

        // Lỗi
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
}