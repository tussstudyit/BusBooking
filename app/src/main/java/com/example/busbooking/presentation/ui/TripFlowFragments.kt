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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.adapter.TripAdapter
import com.example.busbooking.presentation.viewmodel.TripDetailsViewModel
import com.example.busbooking.presentation.viewmodel.TripListViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.TripRepository
import com.example.busbooking.presentation.viewmodel.TripListViewModelFactory
import com.example.busbooking.presentation.viewmodel.ViewModelFactory

// ─────────────────────────────────────────────────────────────────────────────
// 1. TripListFragment
// ─────────────────────────────────────────────────────────────────────────────

class TripListFragment : Fragment() {

    private val viewModel: TripListViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        TripListViewModelFactory(
            TripRepository(
                tripDAO = db.tripDao(),
                routeDAO = db.routeDao()
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
            findNavController().navigate(R.id.action_tripListFragment_to_tripDetailsFragment, bundle)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Lấy arguments
        origin      = arguments?.getString("origin") ?: ""
        destination = arguments?.getString("destination") ?: ""
        currentDate = arguments?.getLong("tripDate") ?: System.currentTimeMillis()

        // Bind header
        originText.text      = origin
        destinationText.text = destination
        tripDateText.text    = dateFmt.format(Date(currentDate))

        // Nút back
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Nút prev/next ngày
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

        // Observe
        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips)
            val isEmpty = trips.isEmpty()
            emptyText.visibility    = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
        // Debug: kiểm tra arguments nhận được
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
                    tripDAO = db.tripDao(),
                    routeDAO = db.routeDao()
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
            statusText.text    = "Trạng thái: ${trip.trip.status}"
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
                Toast.makeText(requireContext(), "Không tải được dữ liệu chuyến", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadTrip(tripId)
    }
}