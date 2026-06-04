package com.example.busbooking.staff.ui.trip

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.model.StaffSeat
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.utils.StaffFormatters
import com.example.busbooking.staff.utils.StaffViewModelFactory
import kotlin.math.roundToInt

class TripDetailFragment : Fragment() {
    private val viewModel: TripDetailViewModel by viewModels {
        StaffViewModelFactory { TripDetailViewModel(StaffRepository()) }
    }
    private val tripId: Long by lazy { arguments?.getLong("tripId", 0L) ?: 0L }
    private val floor1SeatAdapter = StaffSeatAdapter { showSeatInfo(it) }
    private val floor2SeatAdapter = StaffSeatAdapter { showSeatInfo(it) }
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val liveRefresh = object : Runnable {
        override fun run() {
            viewModel.loadTrip(tripId)
            refreshHandler.postDelayed(this, LIVE_REFRESH_MS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val routeText = view.findViewById<TextView>(R.id.routeText)
        val timeText = view.findViewById<TextView>(R.id.timeText)
        val busText = view.findViewById<TextView>(R.id.busText)
        val staffText = view.findViewById<TextView>(R.id.staffText)
        val floor1SeatRecyclerView = view.findViewById<RecyclerView>(R.id.floor1SeatRecyclerView)
        val floor2SeatRecyclerView = view.findViewById<RecyclerView>(R.id.floor2SeatRecyclerView)

        floor1SeatRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        floor1SeatRecyclerView.adapter = floor1SeatAdapter
        floor1SeatRecyclerView.isNestedScrollingEnabled = false
        floor1SeatRecyclerView.setHasFixedSize(false)

        floor2SeatRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        floor2SeatRecyclerView.adapter = floor2SeatAdapter
        floor2SeatRecyclerView.isNestedScrollingEnabled = false
        floor2SeatRecyclerView.setHasFixedSize(false)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripDetailState.Success -> {
                    val trip = state.trip
                    val floor1Seats = trip.seats.filter { it.floor == 1 }
                    val floor2Seats = trip.seats.filter { it.floor == 2 }

                    routeText.text = "${trip.origin} → ${trip.destination}"
                    timeText.text = "Khởi hành: ${StaffFormatters.dateTime(trip.departureTime)}"
                    busText.text = "Xe: ${trip.licensePlate} · ${trip.bookedSeats} vé đã đặt · ${trip.totalSeats} ghế"
                    staffText.text = "Phụ trách: ${trip.staffName.ifBlank { trip.driverName.ifBlank { "Chưa cập nhật" } }}"

                    floor1SeatAdapter.submitList(floor1Seats)
                    floor2SeatAdapter.submitList(floor2Seats)
                    resizeSeatGrid(floor1SeatRecyclerView, floor1Seats)
                    resizeSeatGrid(floor2SeatRecyclerView, floor2Seats)
                }
                is TripDetailState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                TripDetailState.Loading -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTrip(tripId)
        refreshHandler.removeCallbacks(liveRefresh)
        refreshHandler.postDelayed(liveRefresh, LIVE_REFRESH_MS)
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(liveRefresh)
        super.onPause()
    }

    private fun showSeatInfo(seat: StaffSeat) {
        val passenger = seat.passenger
        if (passenger == null) {
            Toast.makeText(requireContext(), "Ghế ${seat.seatNumber} đang trống", Toast.LENGTH_SHORT).show()
            return
        }
        if (!seat.status.equals("CHECKED_IN", true) && !passenger.checkInStatus.equals("CHECKED_IN", true)) {
            Toast.makeText(requireContext(), "Ghế ${seat.seatNumber} đã đặt, khách chưa check-in", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Ghế ${seat.seatNumber}")
            .setMessage(
                "Vé: #${passenger.ticketId}\n" +
                    "Khách: ${passenger.name}\n" +
                    "SĐT: ${passenger.phone}\n" +
                    "Thanh toán: ${StaffFormatters.paymentStatus(passenger.paymentStatus)}\n" +
                    "Lên xe: ${StaffFormatters.checkInStatus(passenger.checkInStatus)}"
            )
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun resizeSeatGrid(recyclerView: RecyclerView, seats: List<StaffSeat>) {
        val rows = (seats.maxOfOrNull { it.rowIndex } ?: -1) + 1
        val params = recyclerView.layoutParams
        params.height = if (rows <= 0) ViewGroup.LayoutParams.WRAP_CONTENT else rows * dp(56)
        recyclerView.layoutParams = params
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private companion object {
        const val LIVE_REFRESH_MS = 15_000L
    }
}
