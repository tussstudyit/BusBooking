package com.example.busbooking.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
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
import com.example.busbooking.domain.repository.ApiTripRepository
import com.example.busbooking.domain.repository.ApiSeatRepository
import com.example.busbooking.domain.repository.TripRepository
import com.example.busbooking.presentation.adapter.SeatAdapter
import com.example.busbooking.presentation.adapter.TripAdapter
import com.example.busbooking.presentation.viewmodel.SeatSelectionViewModel
import com.example.busbooking.presentation.viewmodel.TripDetailsViewModel
import com.example.busbooking.presentation.viewmodel.TripListViewModel
import com.example.busbooking.presentation.viewmodel.TripListViewModelFactory
import com.example.busbooking.presentation.viewmodel.ViewModelFactory
import com.example.busbooking.utils.StatusLabels
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private fun Bundle.copyRoundTripArgsFrom(source: Bundle?) {
    source ?: return
    putBoolean("isRoundTrip", source.getBoolean("isRoundTrip", false))
    putBoolean("isReturnLeg", source.getBoolean("isReturnLeg", false))
    putLong("returnDate", source.getLong("returnDate", 0L))
    putLong("outboundTripId", source.getLong("outboundTripId", -1L))
    putDouble("outboundTripPrice", source.getDouble("outboundTripPrice", 0.0))
    putLong("outboundTripDate", source.getLong("outboundTripDate", 0L))
    putLong("outboundDepartureTime", source.getLong("outboundDepartureTime", 0L))
    putString("outboundOrigin", source.getString("outboundOrigin").orEmpty())
    putString("outboundDestination", source.getString("outboundDestination").orEmpty())
    source.getLongArray("outboundSeatIds")?.let { putLongArray("outboundSeatIds", it) }
    source.getStringArrayList("outboundSeatNumbers")
        ?.let { putStringArrayList("outboundSeatNumbers", it) }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 1. TripListFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class TripListFragment : Fragment() {

    private val viewModel: TripListViewModel by viewModels {
        TripListViewModelFactory(
            ApiTripRepository()
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
    private lateinit var filterButton: ImageButton
    private lateinit var seatTypeFilterText: TextView

    private var origin: String = ""
    private var destination: String = ""
    private var currentDate: Long = System.currentTimeMillis()
    private var selectedTotalSeats: Int? = null

    private val dateFmt = SimpleDateFormat("EEE, dd/MM/yy", Locale("vi"))

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
        filterButton    = view.findViewById(R.id.filterButton)
        seatTypeFilterText = view.findViewById(R.id.seatTypeFilterText)

        adapter = TripAdapter { trip ->
            val bundle = Bundle().apply {
                putLong("tripId", trip.trip.id)
                selectedTotalSeats?.let { putInt("totalSeats", it) }
                copyRoundTripArgsFrom(arguments)
            }
            findNavController().navigate(
                R.id.action_tripListFragment_to_tripDetailsFragment, bundle
            )
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        origin      = arguments?.getString("origin") ?: ""
        destination = arguments?.getString("destination") ?: ""
        currentDate = arguments?.getLong("tripDate") ?: System.currentTimeMillis()
        selectedTotalSeats = arguments?.getInt("totalSeats", 0)?.takeIf { it == 24 || it == 34 }

        originText.text      = origin
        destinationText.text = destination
        tripDateText.text    = dateFmt.format(Date(currentDate))
        updateSeatTypeFilterText()

        backButton.setOnClickListener { findNavController().popBackStack() }
        filterButton.setOnClickListener { showSeatTypeFilter() }

        prevDayButton.setOnClickListener {
            currentDate -= 86400000L
            tripDateText.text = dateFmt.format(Date(currentDate))
            reloadTrips()
        }

        nextDayButton.setOnClickListener {
            currentDate += 86400000L
            tripDateText.text = dateFmt.format(Date(currentDate))
            reloadTrips()
        }

        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips)
            val isEmpty = trips.isEmpty()
            emptyText.visibility    = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        android.util.Log.d("TripList", "origin='$origin' destination='$destination' date=$currentDate")
        reloadTrips()
    }

    private fun showSeatTypeFilter() {
        val popup = PopupMenu(requireContext(), filterButton)
        popup.menu.add(0, FILTER_ALL, 0, "Tất cả")
        popup.menu.add(0, FILTER_24, 1, "Xe 24 ghế")
        popup.menu.add(0, FILTER_34, 2, "Xe 34 ghế")
        popup.menu.setGroupCheckable(0, true, true)
        popup.menu.findItem(
            when (selectedTotalSeats) {
                24 -> FILTER_24
                34 -> FILTER_34
                else -> FILTER_ALL
            }
        )?.isChecked = true
        popup.setOnMenuItemClickListener { item ->
            selectedTotalSeats = when (item.itemId) {
                FILTER_24 -> 24
                FILTER_34 -> 34
                else -> null
            }
            updateSeatTypeFilterText()
            reloadTrips()
            true
        }
        popup.show()
    }

    private fun reloadTrips() {
        viewModel.searchTrips(origin, destination, currentDate, selectedTotalSeats)
    }

    private fun updateSeatTypeFilterText() {
        seatTypeFilterText.text = when (selectedTotalSeats) {
            24 -> "Loại xe: 24 ghế"
            34 -> "Loại xe: 34 ghế"
            else -> "Loại xe: Tất cả"
        }
    }

    private companion object {
        const val FILTER_ALL = 0
        const val FILTER_24 = 24
        const val FILTER_34 = 34
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 2. TripDetailsFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class TripDetailsFragment : Fragment() {

    private val viewModel: TripDetailsViewModel by viewModels {
        ViewModelFactory {
            TripDetailsViewModel(
                ApiTripRepository()
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
        val dateFmt = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

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
                    statusText.text = "Trạng thái: ${StatusLabels.trip(tripStatus)}"
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
                    copyRoundTripArgsFrom(arguments)
                    arguments?.getInt("totalSeats", 0)?.takeIf { it == 24 || it == 34 }?.let { putInt("totalSeats", it) }
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

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 3. SeatSelectionFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class SeatSelectionFragment : Fragment() {

    // Ã¢â€â‚¬Ã¢â€â‚¬ ViewModel Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private val viewModel: SeatSelectionViewModel by viewModels {
        ViewModelFactory {
            SeatSelectionViewModel(
                seatRepository = ApiSeatRepository()
            )
        }
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Views Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private lateinit var seatsFloor1RecyclerView: RecyclerView
    private lateinit var seatsFloor2RecyclerView: RecyclerView
    private lateinit var emptySeatText: TextView
    private lateinit var selectedSeatText: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var confirmButton: MaterialButton
    private var confirmButtonDefaultText: CharSequence = ""

    // Ã¢â€â‚¬Ã¢â€â‚¬ Adapters Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private lateinit var adapterFloor1: SeatAdapter
    private lateinit var adapterFloor2: SeatAdapter

    // Ã¢â€â‚¬Ã¢â€â‚¬ Args Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private var tripId: Long      = -1L
    private var tripPrice: Double = 0.0
    private var origin: String = ""
    private var destination: String = ""
    private var tripDate: Long = 0L
    private var departureTime: Long = 0L
    private var isRoundTrip: Boolean = false
    private var isReturnLeg: Boolean = false
    private var returnDate: Long = 0L
    private var outboundTripId: Long = -1L
    private var outboundTripPrice: Double = 0.0
    private var outboundTripDate: Long = 0L
    private var outboundDepartureTime: Long = 0L
    private var outboundOrigin: String = ""
    private var outboundDestination: String = ""
    private var outboundSeatIds: List<Long> = emptyList()
    private var outboundSeatNumbers: List<String> = emptyList()

    // Ã¢â€â‚¬Ã¢â€â‚¬ Lifecycle Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

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
        isRoundTrip = arguments?.getBoolean("isRoundTrip", false) ?: false
        isReturnLeg = arguments?.getBoolean("isReturnLeg", false) ?: false
        returnDate = arguments?.getLong("returnDate") ?: 0L
        outboundTripId = arguments?.getLong("outboundTripId") ?: -1L
        outboundTripPrice = arguments?.getDouble("outboundTripPrice") ?: 0.0
        outboundTripDate = arguments?.getLong("outboundTripDate") ?: 0L
        outboundDepartureTime = arguments?.getLong("outboundDepartureTime") ?: 0L
        outboundOrigin = arguments?.getString("outboundOrigin").orEmpty()
        outboundDestination = arguments?.getString("outboundDestination").orEmpty()
        outboundSeatIds = arguments?.getLongArray("outboundSeatIds")?.toList().orEmpty()
        outboundSeatNumbers = arguments?.getStringArrayList("outboundSeatNumbers").orEmpty()

        if (tripId == -1L) {
            Toast.makeText(requireContext(), "Chuyến xe không hợp lệ", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        bindViews(view)
        confirmButtonDefaultText = confirmButton.text
        if (isRoundTrip && !isReturnLeg) {
            confirmButtonDefaultText = "CH\u1eccN CHUY\u1ebeN V\u1ec0"
            confirmButton.text = confirmButtonDefaultText
        } else if (isRoundTrip && isReturnLeg) {
            confirmButtonDefaultText = "X\u00c1C NH\u1eacN KH\u1ee8 H\u1ed2I"
            confirmButton.text = confirmButtonDefaultText
        }
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
            if (isRoundTrip && !isReturnLeg) {
                navigateToReturnTrips()
            } else if (isRoundTrip && isReturnLeg) {
                viewModel.bookRoundTripSeats(
                    outboundTripId = outboundTripId,
                    outboundSeatIds = outboundSeatIds,
                    outboundSeatNumbers = outboundSeatNumbers,
                    outboundPrice = outboundTripPrice,
                    returnTripId = tripId
                )
            } else {
                viewModel.bookSeats(tripId)
            }
        }
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Bind views Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private fun navigateToReturnTrips() {
        val selectedSeats = viewModel.selectedSeats.value.orEmpty()
        if (selectedSeats.isEmpty()) {
            Toast.makeText(requireContext(), "Vui l\u00f2ng ch\u1ecdn gh\u1ebf tr\u01b0\u1edbc khi ti\u1ebfp t\u1ee5c", Toast.LENGTH_SHORT).show()
            return
        }
        if (returnDate <= 0L) {
            Toast.makeText(requireContext(), "Thi\u1ebfu ng\u00e0y v\u1ec1 cho v\u00e9 kh\u1ee9 h\u1ed3i", Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putString("origin", destination)
            putString("destination", origin)
            putLong("tripDate", returnDate)
            putBoolean("isRoundTrip", true)
            putBoolean("isReturnLeg", true)
            putLong("returnDate", returnDate)
            putLong("outboundTripId", tripId)
            putDouble("outboundTripPrice", tripPrice)
            putLong("outboundTripDate", tripDate)
            putLong("outboundDepartureTime", departureTime)
            putString("outboundOrigin", origin)
            putString("outboundDestination", destination)
            putLongArray("outboundSeatIds", selectedSeats.map { it.id }.toLongArray())
            putStringArrayList("outboundSeatNumbers", ArrayList(selectedSeats.map { it.seatNumber }))
            arguments?.getInt("totalSeats", 0)?.takeIf { it == 24 || it == 34 }?.let { putInt("totalSeats", it) }
        }
        findNavController().navigate(R.id.tripListFragment, bundle)
    }

    private fun bindViews(view: View) {
        seatsFloor1RecyclerView = view.findViewById(R.id.seatsFloor1RecyclerView)
        seatsFloor2RecyclerView = view.findViewById(R.id.seatsFloor2RecyclerView)
        emptySeatText           = view.findViewById(R.id.emptySeatText)
        selectedSeatText        = view.findViewById(R.id.selectedSeatText)
        totalPriceText          = view.findViewById(R.id.totalPriceText)
        confirmButton           = view.findViewById(R.id.confirmButton)
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Setup RecyclerViews Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

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

    // Ã¢â€â‚¬Ã¢â€â‚¬ Observe ViewModel Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    private fun observeViewModel() {

        // Danh sÃ¡ch gháº¿ -> tÃ¡ch táº§ng -> submit
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

        // Gháº¿ Ä‘ang chá»n -> update icon + bottom bar
        viewModel.selectedSeats.observe(viewLifecycleOwner) { selected ->
            adapterFloor1.updateSelectedSeats(selected)
            adapterFloor2.updateSelectedSeats(selected)
            selectedSeatText.text = "x${selected.size}"
            val canConfirm = selected.isNotEmpty() && viewModel.isProcessing.value != true
            confirmButton.isEnabled = canConfirm
            confirmButton.alpha = if (canConfirm) 1f else 0.55f
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            val hasSelection = viewModel.selectedSeats.value.orEmpty().isNotEmpty()
            confirmButton.text = if (isProcessing) "ĐANG TẠO QR..." else confirmButtonDefaultText
            confirmButton.isEnabled = hasSelection && !isProcessing
            confirmButton.alpha = if (hasSelection && !isProcessing) 1f else 0.55f
        }

        // Tá»•ng tiá»n
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
                    putBoolean("isRoundTrip", checkout.isRoundTrip)
                    putStringArrayList("outboundSeatNumbers", ArrayList(checkout.outboundSeatNumbers))
                    putStringArrayList("returnSeatNumbers", ArrayList(checkout.returnSeatNumbers))
                    if (checkout.isRoundTrip) {
                        putString("origin", outboundOrigin.ifBlank { destination })
                        putString("destination", outboundDestination.ifBlank { origin })
                        putLong("tripDate", outboundTripDate)
                        putLong("departureTime", outboundDepartureTime)
                        putString("returnOrigin", origin)
                        putString("returnDestination", destination)
                        putLong("returnTripDate", tripDate)
                        putLong("returnDepartureTime", departureTime)
                    } else {
                        putString("origin", origin)
                        putString("destination", destination)
                        putLong("tripDate", tripDate)
                        putLong("departureTime", departureTime)
                    }
                }
                viewModel.clearCheckout()
                findNavController().navigate(
                    R.id.action_seatSelectionFragment_to_bookingConfirmationFragment,
                    bundle
                )
            }
        }

        // Lá»—i
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
}

