package com.example.busbooking.staff.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.ui.trip.TripAdapter
import com.example.busbooking.staff.utils.StaffViewModelFactory

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels {
        StaffViewModelFactory { HomeViewModel(StaffRepository()) }
    }
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val liveRefresh = object : Runnable {
        override fun run() {
            viewModel.loadHome()
            refreshHandler.postDelayed(this, LIVE_REFRESH_MS)
        }
    }
    private val todayTripAdapter = TripAdapter { trip ->
        findNavController().navigate(
            R.id.tripDetailFragment,
            Bundle().apply { putLong("tripId", trip.id) }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val staffNameText = view.findViewById<TextView>(R.id.staffNameText)
        val companyText = view.findViewById<TextView>(R.id.companyText)
        val todayTripsText = view.findViewById<TextView>(R.id.todayTripsText)
        val checkInText = view.findViewById<TextView>(R.id.checkInText)
        val homeTripRecyclerView = view.findViewById<RecyclerView>(R.id.homeTripRecyclerView)
        val homeTripEmptyText = view.findViewById<TextView>(R.id.homeTripEmptyText)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        homeTripRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeTripRecyclerView.adapter = todayTripAdapter

        view.findViewById<Button>(R.id.tripsButton).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_tripListFragment)
        }
        view.findViewById<Button>(R.id.scanButton).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_ticketScannerFragment)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeState.Success -> {
                    errorText.visibility = View.GONE
                    staffNameText.text = state.summary.staffName
                    companyText.text = state.summary.companyName
                    todayTripsText.text = state.summary.assignedTripsToday.toString()
                    checkInText.text = "${state.summary.checkedInPassengers}/${state.summary.bookedPassengersToday}"
                    todayTripAdapter.submitList(state.summary.todayTrips)
                    homeTripEmptyText.visibility = if (state.summary.todayTrips.isEmpty()) View.VISIBLE else View.GONE
                    homeTripRecyclerView.visibility = if (state.summary.todayTrips.isEmpty()) View.GONE else View.VISIBLE
                }
                is HomeState.Error -> {
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                }
                HomeState.Loading -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHome()
        refreshHandler.removeCallbacks(liveRefresh)
        refreshHandler.postDelayed(liveRefresh, LIVE_REFRESH_MS)
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(liveRefresh)
        super.onPause()
    }

    private companion object {
        const val LIVE_REFRESH_MS = 15_000L
    }
}
