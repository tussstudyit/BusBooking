package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

// ─────────────────────────────────────────────────────────────────────────────
// 1. TripListFragment — Danh sách chuyến xe theo kết quả tìm kiếm
// ─────────────────────────────────────────────────────────────────────────────

class TripListFragment : Fragment() {

    private val viewModel: TripListViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: TripAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_trip_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.tripsRecyclerView)
        emptyText    = view.findViewById(R.id.emptyText)

        adapter = TripAdapter { trip ->
            val bundle = Bundle().apply { putLong("tripId", trip.trip.id) }
            findNavController().navigate(R.id.action_tripListFragment_to_tripDetailsFragment, bundle)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val origin      = arguments?.getString("origin") ?: ""
        val destination = arguments?.getString("destination") ?: ""
        val tripDate    = arguments?.getLong("tripDate") ?: System.currentTimeMillis()

        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips)
            val isEmpty = trips.isEmpty()
            emptyText.visibility    = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.searchTrips(origin, destination, tripDate)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. TripDetailsFragment — Chi tiết chuyến xe + nút chọn ghế
// ─────────────────────────────────────────────────────────────────────────────

class TripDetailsFragment : Fragment() {

    private val viewModel: TripDetailsViewModel by viewModels()

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

        val routeText       = view.findViewById<TextView>(R.id.routeText)
        val dateText        = view.findViewById<TextView>(R.id.dateText)
        val departureText   = view.findViewById<TextView>(R.id.departureText)
        val arrivalText     = view.findViewById<TextView>(R.id.arrivalText)
        val priceText       = view.findViewById<TextView>(R.id.priceText)
        val busText         = view.findViewById<TextView>(R.id.busText)
        val statusText      = view.findViewById<TextView>(R.id.statusText)
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
            val bundle = Bundle().apply { putLong("tripId", tripId) }
            findNavController().navigate(R.id.action_tripDetailsFragment_to_seatSelectionFragment, bundle)
        }

        viewModel.loadTrip(tripId)
    }
}
