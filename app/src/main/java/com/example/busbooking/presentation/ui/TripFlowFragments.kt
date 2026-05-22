package com.example.busbooking.presentation.ui

import android.content.Intent
import android.net.Uri
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
import com.example.busbooking.domain.models.SeatDisplay
import com.example.busbooking.domain.repository.FirebaseTripRepository
import com.example.busbooking.domain.repository.FirebaseSeatRepository
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// 1. TripListFragment
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

class TripListFragment : Fragment() {

    private val viewModel: TripListViewModel by viewModels {
        TripListViewModelFactory(
            FirebaseTripRepository()
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// 2. TripDetailsFragment
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

class TripDetailsFragment : Fragment() {

    private val viewModel: TripDetailsViewModel by viewModels {
        ViewModelFactory {
            TripDetailsViewModel(
                FirebaseTripRepository()
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

        viewModel.seatAvailability.observe(viewLifecycleOwner) { availability ->
            val tripStatus = viewModel.trip.value?.trip?.status ?: "SCHEDULED"
            val available = availability?.availableSeats ?: -1
            val total = availability?.totalSeats ?: 0
            when {
                available < 0 -> {
                    statusText.text = "Trạng thái: Đang tải..."
                }
                tripStatus != "SCHEDULED" -> {
                    statusText.text = "Trạng thái: ${tripStatus}"
                    statusText.setTextColor(
                        requireContext().getColor(android.R.color.holo_red_dark)
                    )
                    selectSeatButton.isEnabled = false
                    selectSeatButton.alpha = 0.5f
                }
                total <= 0 -> {
                    statusText.text = "Trạng thái: Chưa có sơ đồ ghế"
                    statusText.setTextColor(
                        requireContext().getColor(android.R.color.holo_red_dark)
                    )
                    selectSeatButton.isEnabled = false
                    selectSeatButton.alpha = 0.5f
                }
                available <= 0 -> {
                    statusText.text = "Trạng thái: Hết chỗ (0/$total giường)"
                    statusText.setTextColor(
                        requireContext().getColor(android.R.color.holo_red_dark)
                    )
                    selectSeatButton.isEnabled = false
                    selectSeatButton.alpha = 0.5f
                }
                else -> {
                    statusText.text = "Trạng thái: Còn chỗ ($available/$total giường)"
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
                    putFloat("tripPrice", currentTrip.trip.price.toFloat())
                    putString("origin", currentTrip.route.origin)
                    putString("destination", currentTrip.route.destination)
                    putLong("tripDate", currentTrip.trip.tripDate)
                    putLong("departureTime", currentTrip.trip.departureTime)
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

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// 3. SeatSelectionFragment
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

class SeatSelectionFragment : Fragment() {

    // â”€â”€ ViewModel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private val viewModel: SeatSelectionViewModel by viewModels {
        ViewModelFactory {
            SeatSelectionViewModel(
                seatRepository = FirebaseSeatRepository()
            )
        }
    }

    // â”€â”€ Views â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private lateinit var seatsFloor1RecyclerView: RecyclerView
    private lateinit var seatsFloor2RecyclerView: RecyclerView
    private lateinit var emptySeatText: TextView
    private lateinit var selectedSeatText: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var confirmButton: MaterialButton

    // â”€â”€ Adapters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private lateinit var adapterFloor1: SeatAdapter
    private lateinit var adapterFloor2: SeatAdapter

    // â”€â”€ Args â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private var tripId: Long      = -1L
    private var tripPrice: Double = 0.0
    private var origin: String = ""
    private var destination: String = ""
    private var tripDate: Long = 0L
    private var departureTime: Long = 0L

    // â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_seat_selection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripId    = arguments?.getLong("tripId")      ?: -1L
        tripPrice = arguments?.getFloat("tripPrice")?.toDouble() ?: 0.0
        origin = arguments?.getString("origin").orEmpty()
        destination = arguments?.getString("destination").orEmpty()
        tripDate = arguments?.getLong("tripDate") ?: 0L
        departureTime = arguments?.getLong("departureTime") ?: 0L

        if (tripId == -1L) {
            Toast.makeText(requireContext(), "Chuyến xe không hợp lệ", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        bindViews(view)
        confirmButton.isEnabled = false
        confirmButton.alpha = 0.55f
        setupRecyclerViews()
        observeViewModel()

        viewModel.setTripPrice(tripPrice)
        viewModel.loadSeats(tripId)

        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            findNavController().popBackStack()
        }
        view.findViewById<ImageButton>(R.id.homeButton).setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }
        confirmButton.setOnClickListener {
            viewModel.bookSeats(tripId)
        }
    }

    // â”€â”€ Bind views â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun bindViews(view: View) {
        seatsFloor1RecyclerView = view.findViewById(R.id.seatsFloor1RecyclerView)
        seatsFloor2RecyclerView = view.findViewById(R.id.seatsFloor2RecyclerView)
        emptySeatText           = view.findViewById(R.id.emptySeatText)
        selectedSeatText        = view.findViewById(R.id.selectedSeatText)
        totalPriceText          = view.findViewById(R.id.totalPriceText)
        confirmButton           = view.findViewById(R.id.confirmButton)
    }

    // â”€â”€ Setup RecyclerViews â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Observe ViewModel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun observeViewModel() {

        // Danh sách ghế -> tách tầng -> submit
        viewModel.seats.observe(viewLifecycleOwner) { allSeats ->
            android.util.Log.d("SeatSelection", "Loaded seats=${allSeats.size} tripId=$tripId")
            val floor1: List<SeatDisplay> = allSeats
                .filter { it.seat.floor == 1 }
                .sortedBy { it.seat.id }

            val floor2: List<SeatDisplay> = allSeats
                .filter { it.seat.floor == 2 }
                .sortedBy { it.seat.id }

            emptySeatText.text = if (allSeats.isEmpty()) {
                "Không có sơ đồ ghế cho chuyến này"
            } else {
                ""
            }
            emptySeatText.visibility = if (allSeats.isEmpty()) View.VISIBLE else View.GONE
            adapterFloor1.submitSeats(floor1)
            adapterFloor2.submitSeats(floor2)
        }

        // Ghế đang chọn -> update icon + bottom bar
        viewModel.selectedSeats.observe(viewLifecycleOwner) { selected ->
            adapterFloor1.updateSelectedSeats(selected)
            adapterFloor2.updateSelectedSeats(selected)
            selectedSeatText.text = "x${selected.size}"
            confirmButton.isEnabled = selected.isNotEmpty()
            confirmButton.alpha = if (selected.isNotEmpty()) 1f else 0.55f
        }

        // Tổng tiền
        viewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            val formatted = NumberFormat
                .getNumberInstance(Locale("vi", "VN"))
                .format(total.toLong())
            totalPriceText.text = "  $formatted vnđ"
        }

        viewModel.checkout.observe(viewLifecycleOwner) { checkout ->
            checkout ?: return@observe
            if (checkout.ticketIds.isNotEmpty()) {
                val bundle = Bundle().apply {
                    putLong("ticketId", checkout.ticketIds.first())
                    putLongArray("ticketIds", checkout.ticketIds.toLongArray())
                    putLong("tripId", tripId)
                    putStringArrayList("seatNumbers", ArrayList(checkout.seatNumbers))
                    putDouble("totalPrice", checkout.totalPrice)
                    putString("paymentId", checkout.paymentId)
                    putString("paymentUrl", checkout.paymentUrl)
                    putString("qrContent", checkout.qrContent)
                    putString("qrImageBase64", checkout.qrImageBase64)
                    putString("qrMimeType", checkout.qrMimeType)
                    putLong("paymentExpiresAt", checkout.paymentExpiresAt ?: 0L)
                    putString("paymentError", checkout.paymentError)
                    putString("origin", origin)
                    putString("destination", destination)
                    putLong("tripDate", tripDate)
                    putLong("departureTime", departureTime)
                }
                viewModel.clearCheckout()
                findNavController().navigate(
                    R.id.action_seatSelectionFragment_to_bookingConfirmationFragment,
                    bundle
                )
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
