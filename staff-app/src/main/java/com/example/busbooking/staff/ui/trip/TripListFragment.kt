package com.example.busbooking.staff.ui.trip

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.utils.StaffTripGroups
import com.example.busbooking.staff.utils.StaffViewModelFactory

class TripListFragment : Fragment() {
    private val viewModel: TripListViewModel by viewModels {
        StaffViewModelFactory { TripListViewModel(StaffRepository()) }
    }
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val liveRefresh = object : Runnable {
        override fun run() {
            viewModel.loadTrips()
            refreshHandler.postDelayed(this, LIVE_REFRESH_MS)
        }
    }

    private lateinit var adapter: TripAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var sectionTitleText: TextView
    private lateinit var upcomingTripsButton: Button
    private lateinit var historyTripsButton: Button

    private var currentGroups = StaffTripGroups(emptyList(), emptyList())
    private var currentSection = TripSection.UPCOMING

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_trip_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.tripRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        sectionTitleText = view.findViewById(R.id.sectionTitleText)
        upcomingTripsButton = view.findViewById(R.id.upcomingTripsButton)
        historyTripsButton = view.findViewById(R.id.historyTripsButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        adapter = TripAdapter { trip ->
            findNavController().navigate(
                R.id.action_tripListFragment_to_tripDetailFragment,
                Bundle().apply { putLong("tripId", trip.id) }
            )
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        upcomingTripsButton.setOnClickListener { selectSection(TripSection.UPCOMING) }
        historyTripsButton.setOnClickListener { selectSection(TripSection.HISTORY) }
        renderTrips()

        viewModel.state.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state is TripListState.Loading) View.VISIBLE else View.GONE
            when (state) {
                is TripListState.Success -> {
                    currentGroups = state.groups
                    renderTrips()
                }
                is TripListState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                TripListState.Loading -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTrips()
        refreshHandler.removeCallbacks(liveRefresh)
        refreshHandler.postDelayed(liveRefresh, LIVE_REFRESH_MS)
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(liveRefresh)
        super.onPause()
    }

    private fun selectSection(section: TripSection) {
        if (currentSection == section) return
        currentSection = section
        renderTrips()
    }

    private fun renderTrips() {
        val selectedTrips = when (currentSection) {
            TripSection.UPCOMING -> currentGroups.upcomingTrips
            TripSection.HISTORY -> currentGroups.historyTrips
        }
        sectionTitleText.text = when (currentSection) {
            TripSection.UPCOMING -> "Chuyến xe sắp tới"
            TripSection.HISTORY -> "Lịch sử chuyến đã qua"
        }
        emptyText.text = when (currentSection) {
            TripSection.UPCOMING -> "Chưa có chuyến sắp tới"
            TripSection.HISTORY -> "Chưa có chuyến đã qua"
        }

        upcomingTripsButton.text = "Sắp tới (${currentGroups.upcomingTrips.size})"
        historyTripsButton.text = "Lịch sử (${currentGroups.historyTrips.size})"
        updateSectionButton(upcomingTripsButton, currentSection == TripSection.UPCOMING)
        updateSectionButton(historyTripsButton, currentSection == TripSection.HISTORY)

        adapter.submitList(selectedTrips)
        emptyText.visibility = if (selectedTrips.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (selectedTrips.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateSectionButton(button: Button, selected: Boolean) {
        button.setBackgroundResource(if (selected) R.drawable.bg_staff_primary_button else R.drawable.bg_staff_card)
        button.setTextColor(
            requireContext().getColor(
                if (selected) android.R.color.white else R.color.staff_text_primary
            )
        )
    }

    private enum class TripSection {
        UPCOMING,
        HISTORY
    }

    private companion object {
        const val LIVE_REFRESH_MS = 15_000L
    }
}
