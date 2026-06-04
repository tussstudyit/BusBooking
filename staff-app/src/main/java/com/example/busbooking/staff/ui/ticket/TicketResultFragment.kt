package com.example.busbooking.staff.ui.ticket

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
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.api.StaffApiClient
import com.example.busbooking.staff.data.model.TicketVerificationResult
import com.example.busbooking.staff.data.repository.StaffRepository
import com.example.busbooking.staff.utils.StaffFormatters
import com.example.busbooking.staff.utils.StaffViewModelFactory

class TicketResultFragment : Fragment() {
    private val viewModel: TicketResultViewModel by viewModels {
        StaffViewModelFactory { TicketResultViewModel(StaffRepository()) }
    }
    private lateinit var currentResult: TicketVerificationResult

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ticket_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentResult = arguments?.getString("ticketResult")
            ?.let { StaffApiClient.json.decodeFromString<TicketVerificationResult>(it) }
            ?: TicketVerificationResult(valid = false, message = "Không có dữ liệu vé")

        val passengerText = view.findViewById<TextView>(R.id.passengerText)
        val phoneText = view.findViewById<TextView>(R.id.phoneText)
        val routeText = view.findViewById<TextView>(R.id.routeText)
        val timeText = view.findViewById<TextView>(R.id.timeText)
        val seatText = view.findViewById<TextView>(R.id.seatText)
        val statusText = view.findViewById<TextView>(R.id.statusText)
        val errorText = view.findViewById<TextView>(R.id.errorText)
        val checkInButton = view.findViewById<Button>(R.id.checkInButton)
        val openTripButton = view.findViewById<Button>(R.id.openTripButton)

        fun bind(result: TicketVerificationResult) {
            passengerText.text = result.passengerName
            phoneText.text = "SĐT: ${result.phone}"
            routeText.text = "${result.origin} → ${result.destination}"
            timeText.text = "Khởi hành: ${StaffFormatters.dateTime(result.departureTime)}"
            seatText.text = "Ghế: ${result.seatNumber}"
            statusText.text = "${StaffFormatters.paymentStatus(result.paymentStatus)} · ${StaffFormatters.checkInStatus(result.checkInStatus)}"
            checkInButton.isEnabled = result.valid && result.checkInStatus.uppercase() != "CHECKED_IN"
            openTripButton.visibility = if (result.tripId > 0L) View.VISIBLE else View.GONE
        }

        bind(currentResult)
        checkInButton.setOnClickListener { viewModel.checkIn(currentResult.ticketId) }
        openTripButton.setOnClickListener {
            if (currentResult.tripId > 0L) {
                findNavController().navigate(R.id.tripDetailFragment, Bundle().apply { putLong("tripId", currentResult.tripId) })
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            checkInButton.isEnabled = state !is TicketCheckInState.Loading
            when (state) {
                is TicketCheckInState.Success -> {
                    errorText.visibility = View.GONE
                    currentResult = state.result
                    bind(state.result)
                    Toast.makeText(requireContext(), "Đã xác nhận hành khách lên xe", Toast.LENGTH_SHORT).show()
                }
                is TicketCheckInState.Error -> {
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                }
                else -> Unit
            }
        }
    }
}
