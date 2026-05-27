package com.example.busbooking.presentation.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.FirebaseAuthRepository
import com.example.busbooking.domain.repository.FirebaseRouteRepository
import com.example.busbooking.domain.repository.FirebaseSeatRepository
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.SeatRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.data.relations.isTicketHistory
import com.example.busbooking.data.relations.isUpcomingTicket
import com.example.busbooking.presentation.adapter.SeatAdapter
import com.example.busbooking.presentation.adapter.TicketAdapter
import com.example.busbooking.presentation.ui.state.BookingState
import com.example.busbooking.presentation.ui.state.UserState
import com.example.busbooking.presentation.viewmodel.BookingConfirmationViewModel
import com.example.busbooking.presentation.viewmodel.BookingViewModel
import com.example.busbooking.presentation.viewmodel.RouteSearchViewModel
import com.example.busbooking.presentation.viewmodel.SeatSelectionViewModel
import com.example.busbooking.presentation.viewmodel.UserProfileViewModel
import com.example.busbooking.presentation.viewmodel.UserViewModel
import com.example.busbooking.presentation.viewmodel.ViewModelFactory
import com.example.busbooking.utils.SessionManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// BASE: TÃƒÂ¡i sÃ¡Â»Â­ dÃ¡Â»Â¥ng UI loading/empty/error + RecyclerView chung cho ticket list
// internal thay vÃƒÂ¬ private Ã„â€˜Ã¡Â»Æ’ subclass public cÃƒÂ³ thÃ¡Â»Æ’ kÃ¡ÂºÂ¿ thÃ¡Â»Â«a
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

internal abstract class BaseTicketListFragment : Fragment() {

    protected val viewModel: UserViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            UserViewModel(
                AuthRepository(db.userDao()),
                TicketRepository(db.ticketDao(), db.seatDao())
            )
        }
    }
    protected lateinit var recyclerView: RecyclerView
    protected lateinit var progressBar: ProgressBar
    protected lateinit var emptyText: TextView
    protected lateinit var errorText: TextView

    abstract val layoutResId: Int
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
                is UserState.Loading       -> showLoading()
                is UserState.TicketsLoaded -> onTicketsLoaded(state)
                is UserState.Error         -> showError(state.message)
                else                       -> progressBar.visibility = View.GONE
            }
        }

        loadTickets()
    }

    abstract fun onTicketsLoaded(state: UserState.TicketsLoaded)

    protected open fun loadTickets() {
        viewModel.loadMyTickets()
    }

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

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 1. MyTicketsFragment Ã¢â‚¬â€ chÃ¡Â»â€° hiÃ¡Â»Æ’n thÃ¡Â»â€¹ vÃƒÂ© CONFIRMED hoÃ¡ÂºÂ·c PENDING
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
internal class MyTicketsFragment : BaseTicketListFragment() {

    override val layoutResId    = R.layout.fragment_my_tickets
    override val recyclerViewId = R.id.ticketsRecyclerView
    private var showingHistory = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TabLayout>(R.id.tabLayout).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showingHistory = tab.position == 1
                loadTickets()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = loadTickets()
        })
    }

    override fun loadTickets() {
        if (showingHistory) {
            viewModel.loadTicketHistory()
        } else {
            viewModel.loadMyTickets()
        }
    }

    override fun onTicketsLoaded(state: UserState.TicketsLoaded) {
        val tickets = if (showingHistory) {
            state.tickets.filter { it.isTicketHistory() }
        } else {
            state.tickets.filter { it.isUpcomingTicket() }
        }
        if (tickets.isEmpty()) {
            showEmpty()
            emptyText.text = if (showingHistory) {
                "B\u1ea1n ch\u01b0a c\u00f3 l\u1ecbch s\u1eed \u0111\u1eb7t v\u00e9 n\u00e0o"
            } else {
                "B\u1ea1n ch\u01b0a c\u00f3 v\u00e9 n\u00e0o s\u1eafp t\u1edbi"
            }
        } else {
            showList()
            val adapter = TicketAdapter { ticket ->
                val bundle = Bundle().apply {
                    putLong("ticketId", ticket.ticket.id)
                    putBoolean(
                        "resumePayment",
                        !showingHistory && ticket.ticket.status in setOf("PENDING", "PENDING_PAYMENT")
                    )
                }
                val actionId = if (!showingHistory && ticket.ticket.status in setOf("PENDING", "PENDING_PAYMENT")) {
                    R.id.action_myTicketsFragment_to_bookingConfirmationFragment
                } else {
                    R.id.action_myTicketsFragment_to_ticketDetailsFragment
                }
                findNavController().navigate(actionId, bundle)
            }
            recyclerView.adapter = adapter
            adapter.submitList(tickets)
        }
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 2. BookingHistoryFragment Ã¢â‚¬â€ hiÃ¡Â»Æ’n thÃ¡Â»â€¹ TÃ¡ÂºÂ¤T CÃ¡ÂºÂ¢ vÃƒÂ© kÃ¡Â»Æ’ cÃ¡ÂºÂ£ Ã„â€˜ÃƒÂ£ hÃ¡Â»Â§y/Ã„â€˜ÃƒÂ£ Ã„â€˜i
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
internal class BookingHistoryFragment : BaseTicketListFragment() {

    override val layoutResId    = R.layout.fragment_booking_history
    override val recyclerViewId = R.id.historyRecyclerView

    override fun loadTickets() {
        viewModel.loadTicketHistory()
    }

    override fun onTicketsLoaded(state: UserState.TicketsLoaded) {
        val allTickets = state.tickets
            .filter { it.isTicketHistory() }
            .sortedByDescending { it.ticket.bookingTime }
        if (allTickets.isEmpty()) {
            showEmpty()
            emptyText.text = "B\u1ea1n ch\u01b0a c\u00f3 l\u1ecbch s\u1eed \u0111\u1eb7t v\u00e9 n\u00e0o"
        } else {
            showList()
            val adapter = TicketAdapter { ticket ->
                val bundle = Bundle().apply { putLong("ticketId", ticket.ticket.id) }
                findNavController().navigate(R.id.ticketDetailsFragment, bundle)
            }
            recyclerView.adapter = adapter
            adapter.submitList(allTickets)
        }
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 3. TicketDetailsFragment Ã¢â‚¬â€ nhÃ¡ÂºÂ­n ticketId qua arguments (khÃƒÂ´ng dÃƒÂ¹ng Safe Args)
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class TicketDetailsFragment : Fragment() {

    private val viewModel: BookingViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            BookingViewModel(
                SeatRepository(db.seatDao()),
                TicketRepository(db.ticketDao(), db.seatDao())
            )
        }
    }
    // Ã„ÂÃ¡Â»Âc ticketId tÃ¡Â»Â« Bundle thay vÃƒÂ¬ navArgs() Ã„â€˜Ã¡Â»Æ’ trÃƒÂ¡nh lÃ¡Â»â€”i Safe Args
    private val ticketId: Long by lazy {
        arguments?.getLong("ticketId", -1L) ?: -1L
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var originCityText: TextView
    private lateinit var originStationText: TextView
    private lateinit var destinationCityText: TextView
    private lateinit var destinationStationText: TextView
    private lateinit var ticketIdText: TextView
    private lateinit var pickupTimeText: TextView
    private lateinit var pickupDateText: TextView
    private lateinit var quantityText: TextView
    private lateinit var seatText: TextView
    private lateinit var statusText: TextView
    private lateinit var priceText: TextView
    private lateinit var pickupPointText: TextView
    private lateinit var dropoffPointText: TextView
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_ticket_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ticketId == -1L) {
            Toast.makeText(requireContext(), "V\u00e9 kh\u00f4ng h\u1ee3p l\u1ec7", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        progressBar  = view.findViewById(R.id.progressBar)
        errorText    = view.findViewById(R.id.errorText)
        originCityText = view.findViewById(R.id.originCityText)
        originStationText = view.findViewById(R.id.originStationText)
        destinationCityText = view.findViewById(R.id.destinationCityText)
        destinationStationText = view.findViewById(R.id.destinationStationText)
        ticketIdText = view.findViewById(R.id.ticketIdText)
        pickupTimeText = view.findViewById(R.id.pickupTimeText)
        pickupDateText = view.findViewById(R.id.pickupDateText)
        quantityText = view.findViewById(R.id.quantityText)
        seatText     = view.findViewById(R.id.seatText)
        statusText   = view.findViewById(R.id.statusText)
        priceText    = view.findViewById(R.id.priceText)
        pickupPointText = view.findViewById(R.id.pickupPointText)
        dropoffPointText = view.findViewById(R.id.dropoffPointText)
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

        viewModel.loadTicket(ticketId)
    }

    private fun bindTicket(state: BookingState.TicketLoaded) {
        progressBar.visibility = View.GONE
        errorText.visibility   = View.GONE

        // TicketLoaded chÃ¡Â»Â©a TicketDetails Ã¢â‚¬â€ truy cÃ¡ÂºÂ­p qua cÃƒÂ¡c relation
        val details = state.ticket                          // TicketDetails
        val ticket  = details.ticket                        // Ticket entity
        val trip    = details.tripWithRouteAndBus.trip      // Trip
        val route   = details.tripWithRouteAndBus.route     // Route
        val seat    = details.seat                          // Seat

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        originCityText.text = route.origin
        originStationText.text = ""
        destinationCityText.text = route.destination
        destinationStationText.text = ""
        ticketIdText.text = "#${ticket.id}"
        statusText.text = displayTicketStatus(ticket.status)
        priceText.text = "${String.format("%,.0f", trip.price)} VN\u0110"
        pickupTimeText.text = timeFmt.format(Date(trip.departureTime))
        pickupDateText.text = dateFmt.format(Date(trip.tripDate))
        quantityText.text = "1 v\u00e9"
        seatText.text = seat.seatNumber
        pickupPointText.text = route.origin
        dropoffPointText.text = route.destination

        val cancellable = ticket.status == "CONFIRMED" || ticket.status == "PENDING"
        cancelButton.isEnabled  = cancellable
        cancelButton.visibility = if (cancellable) View.VISIBLE else View.GONE
    }

    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("H\u1ee7y v\u00e9")
            .setMessage("B\u1ea1n c\u00f3 ch\u1eafc mu\u1ed1n h\u1ee7y v\u00e9 n\u00e0y kh\u00f4ng?")
            .setPositiveButton("H\u1ee7y v\u00e9") { _, _ -> viewModel.cancelTicket(ticketId) }
            .setNegativeButton("Kh\u00f4ng", null)
            .show()
    }

    private fun displayTicketStatus(status: String): String = when (status) {
        "CONFIRMED" -> "\u0110\u00e3 x\u00e1c nh\u1eadn"
        "PENDING", "PENDING_PAYMENT" -> "Ch\u1edd thanh to\u00e1n"
        "CANCELLED" -> "\u0110\u00e3 h\u1ee7y"
        "COMPLETED" -> "Ho\u00e0n t\u1ea5t"
        else -> status
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 4. UserDashboardFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class UserDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 5. UserProfileFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class UserProfileFragment : Fragment() {

    private val viewModel: UserProfileViewModel by viewModels {
        ViewModelFactory {
            UserProfileViewModel(FirebaseAuthRepository())
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // XÃƒÂ³a dÃƒÂ²ng: sessionManager = SessionManager(requireContext())

        val nameInput = view.findViewById<EditText>(R.id.nameInput)
        val emailInput = view.findViewById<EditText>(R.id.emailInput)
        val phoneInput = view.findViewById<EditText>(R.id.phoneInput)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)

        viewModel.loadProfile(SessionManager.getCurrentUserId())

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            nameInput.setText(user.name)
            emailInput.setText(user.email)
            phoneInput.setText(user.phone)
            emailInput.isEnabled = false
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
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            viewModel.updateProfile(name, phone)
        }

        logoutButton.setOnClickListener {
            SessionManager.clearSession()
            findNavController().navigate(
                R.id.action_userProfileFragment_to_loginFragment
            )
        }
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 6. RouteSearchFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class RouteSearchFragment : Fragment() {

    private val viewModel: RouteSearchViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            RouteSearchViewModel(FirebaseRouteRepository())
        }
    }
    private lateinit var originInput: AutoCompleteTextView
    private lateinit var destinationInput: AutoCompleteTextView
    private lateinit var dateButton: Button
    private lateinit var searchButton: Button

    private var selectedDate: Long = System.currentTimeMillis() + 86_400_000L

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

        originInput.setText(DEFAULT_ORIGIN, false)
        destinationInput.setText(DEFAULT_DESTINATION, false)
        updateDateLabel()
        dateButton.setOnClickListener { showDatePicker() }

        searchButton.setOnClickListener {
            val origin      = originInput.text.toString().trim()
            val destination = destinationInput.text.toString().trim()
            if (origin.isEmpty() || destination.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập điểm đi và điểm đến", Toast.LENGTH_SHORT).show()
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
            originInput.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, origins)
            )
        }

        viewModel.destinations.observe(viewLifecycleOwner) { destinations ->
            destinationInput.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, destinations)
            )
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
        dateButton.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    private companion object {
        private const val DEFAULT_ORIGIN = "H\u00e0 N\u1ed9i"
        private const val DEFAULT_DESTINATION = "\u0110\u00e0 N\u1eb5ng"
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 7. SeatSelectionFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class LegacySeatSelectionFragment : Fragment() {

    private val viewModel: SeatSelectionViewModel by viewModels {
        ViewModelFactory {
            SeatSelectionViewModel(
                FirebaseSeatRepository()
            )
        }
    }

    private lateinit var adapterFloor1: SeatAdapter
    private lateinit var adapterFloor2: SeatAdapter
    private var tripId: Long = -1L
    private var tripPrice: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_seat_selection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripId    = arguments?.getLong("tripId", -1L) ?: -1L
        tripPrice = arguments?.getFloat("tripPrice", 0.0f)?.toDouble() ?: 0.0

        if (tripId == -1L) {
            Toast.makeText(requireContext(), "Chuyến xe không hợp lệ", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        viewModel.setTripPrice(tripPrice)

        val rv1              = view.findViewById<RecyclerView>(R.id.seatsFloor1RecyclerView)
        val rv2              = view.findViewById<RecyclerView>(R.id.seatsFloor2RecyclerView)
        val selectedSeatText = view.findViewById<TextView>(R.id.selectedSeatText)
        val totalPriceText   = view.findViewById<TextView>(R.id.totalPriceText)
        val confirmButton    = view.findViewById<Button>(R.id.confirmButton)

        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            findNavController().popBackStack()
        }
        view.findViewById<ImageButton>(R.id.homeButton).setOnClickListener {
            findNavController().popBackStack(R.id.nav_home, false)
        }

        adapterFloor1 = SeatAdapter { seat -> viewModel.toggleSeat(seat) }
        adapterFloor2 = SeatAdapter { seat -> viewModel.toggleSeat(seat) }

        rv1.layoutManager = GridLayoutManager(requireContext(), 3)
        rv2.layoutManager = GridLayoutManager(requireContext(), 3)
        rv1.adapter = adapterFloor1
        rv2.adapter = adapterFloor2

        // Ã¢â€â‚¬Ã¢â€â‚¬ Observe ghÃ¡ÂºÂ¿ Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        viewModel.seats.observe(viewLifecycleOwner) { seats ->
            val floor1 = seats.filter { it.seat.floor == 1 }
            val floor2 = seats.filter { it.seat.floor == 2 }
            adapterFloor1.submitSeats(floor1)
            adapterFloor2.submitSeats(floor2)
        }

        // Ã¢â€â‚¬Ã¢â€â‚¬ Observe ghÃ¡ÂºÂ¿ Ã„â€˜ÃƒÂ£ chÃ¡Â»Ân Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        viewModel.selectedSeats.observe(viewLifecycleOwner) { selected ->
            adapterFloor1.updateSelectedSeats(selected)
            adapterFloor2.updateSelectedSeats(selected)
            selectedSeatText.text = "x${selected.size}"
        }

        // Ã¢â€â‚¬Ã¢â€â‚¬ Observe tÃ¡Â»â€¢ng tiÃ¡Â»Ân Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        viewModel.totalPrice.observe(viewLifecycleOwner) { price ->
            totalPriceText.text = if (price == 0.0) "  0 vnđ"
            else "  ${String.format("%,.0f", price)} vnđ"
        }

        // Ã¢â€â‚¬Ã¢â€â‚¬ Observe kÃ¡ÂºÂ¿t quÃ¡ÂºÂ£ Ã„â€˜Ã¡ÂºÂ·t vÃƒÂ© Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        viewModel.bookingResult.observe(viewLifecycleOwner) { ticketIds ->
            ticketIds ?: return@observe
            Toast.makeText(requireContext(), "Đã tạo yêu cầu thanh toán. Đang mở cổng thanh toán...", Toast.LENGTH_LONG).show()
            // Payment URL observer opens VNPAY when the backend returns a signed URL.


        }

        // Ã¢â€â‚¬Ã¢â€â‚¬ Observe lÃ¡Â»â€”i Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        viewModel.paymentUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                viewModel.clearPaymentUrl()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }

        // Ã¢â€â‚¬Ã¢â€â‚¬ Confirm button Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
        confirmButton.setOnClickListener {
            if (viewModel.selectedSeats.value.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn ghế", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.bookSeats(tripId)
        }

        viewModel.loadSeats(tripId)
    }
}

// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// 8. BookingConfirmationFragment
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

class BookingConfirmationFragment : Fragment() {

    private val viewModel: BookingConfirmationViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            BookingConfirmationViewModel(
                TicketRepository(db.ticketDao(), db.seatDao())
            )
        }
    }
    private var confirmationTicketId: Long = -1L
    private var reloadPaymentStatusOnResume = false
    private var paymentStatusPollJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_booking_confirmation, container, false)

    override fun onResume() {
        super.onResume()
        if (reloadPaymentStatusOnResume && confirmationTicketId != -1L) {
            viewModel.loadTicket(confirmationTicketId)
        }
    }

    override fun onDestroyView() {
        stopPaymentStatusPolling()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ticketId = arguments?.getLong("ticketId", -1L) ?: -1L
        if (ticketId == -1L) {
            Toast.makeText(requireContext(), "Vé không hợp lệ", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        confirmationTicketId = ticketId

        val originCityText      = view.findViewById<TextView>(R.id.originCityText)
        val originStationText   = view.findViewById<TextView>(R.id.originStationText)
        val destinationCityText = view.findViewById<TextView>(R.id.destinationCityText)
        val destinationStation  = view.findViewById<TextView>(R.id.destinationStationText)
        val routeDirectionIcon  = view.findViewById<ImageView>(R.id.routeDirectionIcon)
        val ticketIdText        = view.findViewById<TextView>(R.id.ticketIdText)
        val statusText          = view.findViewById<TextView>(R.id.statusText)
        val priceText           = view.findViewById<TextView>(R.id.priceText)
        val pickupTimeText      = view.findViewById<TextView>(R.id.pickupTimeText)
        val pickupDateText      = view.findViewById<TextView>(R.id.pickupDateText)
        val quantityText        = view.findViewById<TextView>(R.id.quantityText)
        val seatText            = view.findViewById<TextView>(R.id.seatText)
        val returnSeatRow       = view.findViewById<View>(R.id.returnSeatRow)
        val returnSeatText      = view.findViewById<TextView>(R.id.returnSeatText)
        val pickupPointText     = view.findViewById<TextView>(R.id.pickupPointText)
        val dropoffPointText    = view.findViewById<TextView>(R.id.dropoffPointText)
        val totalPriceText      = view.findViewById<TextView>(R.id.totalPriceText)
        val paymentMethodText   = view.findViewById<TextView>(R.id.paymentMethodText)
        val qrCard              = view.findViewById<View>(R.id.qrCard)
        val qrCodeImage         = view.findViewById<ImageView>(R.id.qrCodeImage)
        val qrStatusText        = view.findViewById<TextView>(R.id.qrStatusText)
        val payButton           = view.findViewById<Button>(R.id.payButton)
        val cancelPaymentButton = view.findViewById<Button>(R.id.cancelPaymentButton)
        val viewTicketsButton   = view.findViewById<Button>(R.id.viewTicketsButton)

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val paymentId = arguments?.getString("paymentId").orEmpty()
        var activePaymentUrl = arguments?.getString("paymentUrl").orEmpty()
        val qrImageBase64 = arguments?.getString("qrImageBase64").orEmpty()
        val paymentError = arguments?.getString("paymentError").orEmpty()
        val seatNumbers = arguments?.getStringArrayList("seatNumbers").orEmpty()
        val totalPrice = arguments?.getDouble("totalPrice", 0.0) ?: 0.0
        val origin = arguments?.getString("origin").orEmpty()
        val destination = arguments?.getString("destination").orEmpty()
        val tripDate = arguments?.getLong("tripDate", 0L) ?: 0L
        val departureTime = arguments?.getLong("departureTime", 0L) ?: 0L
        val isRoundTrip = arguments?.getBoolean("isRoundTrip", false) ?: false
        val outboundSeatNumbers = arguments?.getStringArrayList("outboundSeatNumbers").orEmpty()
        val returnSeatNumbers = arguments?.getStringArrayList("returnSeatNumbers").orEmpty()
        val resumePayment = arguments?.getBoolean("resumePayment", false) ?: false
        val hasCheckout = seatNumbers.isNotEmpty() || activePaymentUrl.isNotBlank() || paymentId.isNotBlank()
        reloadPaymentStatusOnResume = hasCheckout
        var requestedResumePayment = false

        fun requestPendingPaymentPayload(statusMessage: String = "Đang tạo lại mã QR VNPAY...") {
            if (requestedResumePayment) return
            requestedResumePayment = true
            qrCard.visibility = View.VISIBLE
            qrCodeImage.visibility = View.GONE
            qrStatusText.text = statusMessage
            payButton.visibility = View.VISIBLE
            payButton.isEnabled = false
            payButton.alpha = 0.55f
            payButton.text = "Đang tạo QR..."
            cancelPaymentButton.visibility = View.VISIBLE
            viewModel.loadPendingPayment(ticketId)
        }

        if (hasCheckout) {
            originCityText.text = origin.ifBlank { "\u0110i\u1ec3m \u0111i" }
            originStationText.text = ""
            destinationCityText.text = destination.ifBlank { "\u0110i\u1ec3m \u0111\u1ebfn" }
            destinationStation.text = ""
            routeDirectionIcon.setImageResource(if (isRoundTrip) R.drawable.ic_swap else R.drawable.ic_bus)
            ticketIdText.text = "#$ticketId"
            statusText.text = "Ch\u1edd x\u00e1c nh\u1eadn"
            priceText.text = "${String.format("%,.0f", totalPrice)} VN\u0110"
            pickupTimeText.text = if (departureTime > 0L) timeFmt.format(Date(departureTime)) else "--:--"
            pickupDateText.text = if (tripDate > 0L) dateFmt.format(Date(tripDate)) else "--/--/----"
            quantityText.text = "${seatNumbers.size.coerceAtLeast(1)} v\u00e9"
            seatText.text = if (isRoundTrip && outboundSeatNumbers.isNotEmpty()) {
                outboundSeatNumbers.joinToString(", ")
            } else {
                seatNumbers.joinToString(", ")
            }
            returnSeatRow.visibility = if (isRoundTrip) View.VISIBLE else View.GONE
            returnSeatText.text = returnSeatNumbers.joinToString(", ")
            pickupPointText.text = origin.ifBlank { "Theo th\u00f4ng tin chuy\u1ebfn xe" }
            dropoffPointText.text = destination.ifBlank { "Theo th\u00f4ng tin chuy\u1ebfn xe" }
            totalPriceText.text = "${String.format("%,.0f", totalPrice)} VN\u0110"
            paymentMethodText.text = "VNPAY"
            bindVnpayQr(activePaymentUrl, qrImageBase64, paymentError, qrCard, qrCodeImage, qrStatusText, payButton)
            payButton.visibility = View.VISIBLE
            cancelPaymentButton.visibility = View.VISIBLE
            if (activePaymentUrl.isBlank() && paymentId.isNotBlank()) {
                requestPendingPaymentPayload()
            }
            startPaymentStatusPolling(ticketId)
        } else {
            qrCard.visibility = View.GONE
            payButton.visibility = View.GONE
            cancelPaymentButton.visibility = View.GONE
        }

        viewModel.ticket.observe(viewLifecycleOwner) { details ->
            details ?: return@observe

            val ticket = details.ticket
            if (hasCheckout) {
                statusText.text = displayConfirmationStatus(ticket.status)
                updateCheckoutPaymentUi(ticket.status, qrStatusText, payButton, cancelPaymentButton)
                if (isPendingPaymentStatus(ticket.status)) {
                    startPaymentStatusPolling(ticketId)
                } else {
                    stopPaymentStatusPolling()
                }
                return@observe
            }

            val route  = details.tripWithRouteAndBus.route
            val trip   = details.tripWithRouteAndBus.trip
            val seat   = details.seat

            originCityText.text      = route.origin
            originStationText.text   = ""
            destinationCityText.text = route.destination
            destinationStation.text  = ""
            routeDirectionIcon.setImageResource(R.drawable.ic_bus)
            returnSeatRow.visibility = View.GONE

            ticketIdText.text     = "#${ticket.id}"
            statusText.text       = displayConfirmationStatus(ticket.status)
            priceText.text        = "${String.format("%,.0f", trip.price)} VN\u0110"

            pickupTimeText.text   = timeFmt.format(Date(trip.departureTime))
            pickupDateText.text   = dateFmt.format(Date(trip.tripDate))
            quantityText.text     = "1 v\u00e9"
            seatText.text         = seat.seatNumber
            pickupPointText.text  = route.origin
            dropoffPointText.text = route.destination
            totalPriceText.text   = "${String.format("%,.0f", trip.price)} VN\u0110"
            paymentMethodText.text = "VNPAY"

            if (resumePayment && isPendingPaymentStatus(ticket.status)) {
                reloadPaymentStatusOnResume = true
                qrCard.visibility = View.VISIBLE
                qrCodeImage.visibility = View.GONE
                qrStatusText.text = "\u0110ang t\u1ea1o l\u1ea1i m\u00e3 QR VNPAY..."
                payButton.visibility = View.VISIBLE
                payButton.isEnabled = false
                payButton.alpha = 0.55f
                payButton.text = "\u0110ang t\u1ea1o QR..."
                cancelPaymentButton.visibility = View.VISIBLE
                if (!requestedResumePayment) {
                    requestPendingPaymentPayload()
                }
            } else {
                qrCard.visibility = View.GONE
                payButton.visibility = View.GONE
                cancelPaymentButton.visibility = View.GONE
            }
        }

        payButton.setOnClickListener {
            if (activePaymentUrl.isBlank()) {
                if (paymentId.isNotBlank() || resumePayment) {
                    requestPendingPaymentPayload("Đang tạo lại link thanh toán VNPAY...")
                } else {
                    Toast.makeText(requireContext(), "Chưa có link thanh toán VNPAY", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activePaymentUrl)))
        }

        cancelPaymentButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("H\u1ee7y thanh to\u00e1n")
                .setMessage("B\u1ea1n c\u00f3 ch\u1eafc mu\u1ed1n h\u1ee7y v\u00e9 \u0111ang ch\u1edd thanh to\u00e1n n\u00e0y kh\u00f4ng?")
                .setPositiveButton("H\u1ee7y") { _, _ -> viewModel.cancelPendingPayment(ticketId) }
                .setNegativeButton("Kh\u00f4ng", null)
                .show()
        }

        viewTicketsButton.setOnClickListener {
            findNavController().navigate(R.id.action_bookingConfirmationFragment_to_myTicketsFragment)
        }

        viewModel.paymentPayload.observe(viewLifecycleOwner) { payload ->
            payload ?: return@observe
            requestedResumePayment = false
            activePaymentUrl = payload.paymentUrl
            bindVnpayQr(
                payload.paymentUrl,
                payload.qrImageBase64,
                "",
                qrCard,
                qrCodeImage,
                qrStatusText,
                payButton
            )
            payButton.visibility = View.VISIBLE
            cancelPaymentButton.visibility = View.VISIBLE
        }

        viewModel.cancelSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(requireContext(), "\u0110\u00e3 h\u1ee7y thanh to\u00e1n", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_bookingConfirmationFragment_to_myTicketsFragment)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                requestedResumePayment = false
                qrStatusText.text = message
                val canRetryPayment = paymentId.isNotBlank() || resumePayment
                payButton.isEnabled = canRetryPayment
                payButton.alpha = if (canRetryPayment) 1f else 0.55f
                payButton.text = if (canRetryPayment) "Thử tạo lại thanh toán" else "Chưa có link thanh toán"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.loadTicket(ticketId)
    }

    private fun bindVnpayQr(
        paymentUrl: String,
        qrImageBase64: String,
        paymentError: String,
        qrCard: View,
        qrCodeImage: ImageView,
        qrStatusText: TextView,
        payButton: Button
    ) {
        qrCard.visibility = View.VISIBLE
        val qrBitmap = decodeQrBitmap(qrImageBase64)
        if (qrBitmap != null) {
            qrCodeImage.visibility = View.VISIBLE
            qrCodeImage.setImageBitmap(qrBitmap)
        } else {
            qrCodeImage.visibility = View.GONE
        }

        qrStatusText.text = if (paymentUrl.isNotBlank() && qrBitmap != null) {
            "Dùng app ngân hàng hoặc VNPAY để quét mã"
        } else if (paymentUrl.isNotBlank()) {
            "Không đọc được ảnh QR. Bấm mở trang thanh toán để tiếp tục."
        } else {
            "Chưa tạo được mã QR VNPAY: ${paymentError.ifBlank { "kiểm tra cấu hình VNPAY và admin-web" }}"
        }
        payButton.text = if (paymentUrl.isBlank()) "Chưa có link thanh toán" else "Mở trang thanh toán"
        payButton.isEnabled = paymentUrl.isNotBlank()
        payButton.alpha = if (paymentUrl.isNotBlank()) 1f else 0.55f
    }

    private fun updateCheckoutPaymentUi(
        status: String,
        qrStatusText: TextView,
        payButton: Button,
        cancelPaymentButton: Button
    ) {
        when (status) {
            "CONFIRMED" -> {
                qrStatusText.text = "Thanh toán thành công. Vé đã được xác nhận."
                payButton.visibility = View.GONE
                cancelPaymentButton.visibility = View.GONE
            }
            "PAYMENT_FAILED", "CANCELLED" -> {
                qrStatusText.text = "Thanh toán không thành công. Vui lòng đặt lại vé nếu cần."
                payButton.isEnabled = false
                payButton.alpha = 0.55f
                cancelPaymentButton.visibility = View.GONE
            }
            else -> {
                qrStatusText.text = "Đang chờ kết quả thanh toán VNPAY. Vé sẽ tự động cập nhật sau khi thanh toán."
                cancelPaymentButton.visibility = View.VISIBLE
            }
        }
    }

    private fun startPaymentStatusPolling(ticketId: Long) {
        if (paymentStatusPollJob?.isActive == true) return
        paymentStatusPollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(PAYMENT_STATUS_POLL_INTERVAL_MS)
                viewModel.loadTicket(ticketId)
            }
        }
    }

    private fun stopPaymentStatusPolling() {
        paymentStatusPollJob?.cancel()
        paymentStatusPollJob = null
    }

    private fun displayConfirmationStatus(status: String): String = when (status) {
        "CONFIRMED" -> "\u0110\u00e3 x\u00e1c nh\u1eadn"
        "PENDING", "PENDING_PAYMENT" -> "Ch\u1edd thanh to\u00e1n"
        "PAYMENT_FAILED" -> "Thanh to\u00e1n th\u1ea5t b\u1ea1i"
        "CANCELLED" -> "\u0110\u00e3 h\u1ee7y"
        else -> status
    }

    private fun isPendingPaymentStatus(status: String): Boolean {
        return status == "PENDING" || status == "PENDING_PAYMENT"
    }

    private fun decodeQrBitmap(qrImageBase64: String) = runCatching {
        if (qrImageBase64.isBlank()) return@runCatching null
        val bytes = Base64.decode(qrImageBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    private companion object {
        const val PAYMENT_STATUS_POLL_INTERVAL_MS = 2_000L
    }
}

