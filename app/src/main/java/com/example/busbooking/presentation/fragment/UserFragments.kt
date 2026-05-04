package com.example.busbooking.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.adapter.TripSearchAdapter
import com.example.busbooking.presentation.viewmodel.TripSearchViewModel
import com.example.busbooking.presentation.ui.state.TripSearchUIState

// ════════════════════════════════════════════════════════════════════════════════
// FRAGMENT 1: TripSearchFragment
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Fragment for searching bus trips.
 *
 * Responsibilities:
 *   • Load origins/destinations on screen load
 *   • Display spinners for origin/destination selection
 *   • Show date picker
 *   • Search trips on button click
 *   • Display results in RecyclerView
 *
 * Lifecycle:
 *   - onCreate(): No DB access
 *   - onCreateView(): Inflate fragment_trip_search.xml
 *   - onViewCreated(): Set up UI, observe LiveData
 *   - onDestroyView(): Cleanup (auto by lifecycle)
 *
 * LiveData Observation:
 *   - uiState: Shows loading/results/error
 *   - navigationEvents: For navigation to seat selection
 */
class TripSearchFragment : Fragment() {

    private val viewModel: TripSearchViewModel by viewModels()
    private lateinit var tripAdapter: TripSearchAdapter

    // UI Views
    private lateinit var originSpinner: android.widget.Spinner
    private lateinit var destinationSpinner: android.widget.Spinner
    private lateinit var dateField: android.widget.EditText
    private lateinit var searchButton: android.widget.Button
    private lateinit var resultsRecycler: RecyclerView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var errorText: android.widget.TextView
    private lateinit var noResultsText: android.widget.TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Step 1: Find all views by ID
        originSpinner = view.findViewById(R.id.trip_search_origin_spinner)
        destinationSpinner = view.findViewById(R.id.trip_search_destination_spinner)
        dateField = view.findViewById(R.id.trip_search_date_field)
        searchButton = view.findViewById(R.id.trip_search_submit_button)
        resultsRecycler = view.findViewById(R.id.trip_search_results_recycler)
        progressBar = view.findViewById(R.id.trip_search_progress_bar)
        errorText = view.findViewById(R.id.trip_search_error_text)
        noResultsText = view.findViewById(R.id.trip_search_no_results_text)

        // Step 2: Set up RecyclerView adapter
        tripAdapter = TripSearchAdapter()
        resultsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tripAdapter
        }

        // Step 3: Set up adapter click listener
        tripAdapter.setOnTripClickListener { tripId ->
            // Navigate to seat selection with trip ID
            val action = TripSearchFragmentDirections.actionTripSearchToSeatSelection(
                tripId = tripId
            )
            findNavController().navigate(action)
        }

        // Step 4: Set up button clicks
        searchButton.setOnClickListener {
            val origin = originSpinner.selectedItem?.toString() ?: ""
            val destination = destinationSpinner.selectedItem?.toString() ?: ""
            val date = dateField.text.toString()

            if (origin.isNotEmpty() && destination.isNotEmpty() && date.isNotEmpty()) {
                viewModel.searchTrips(origin, destination, date)
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dateField.setOnClickListener {
            showDatePicker()
        }

        // Step 5: Load origins/destinations on first load
        viewModel.loadOrigins()
        viewModel.loadDestinations()

        // Step 6: Observe ViewModel UIState ⭐ CRITICAL
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripSearchUIState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    resultsRecycler.visibility = View.GONE
                    errorText.visibility = View.GONE
                    noResultsText.visibility = View.GONE
                    searchButton.isEnabled = false
                }

                is TripSearchUIState.TripsLoaded -> {
                    progressBar.visibility = View.GONE
                    searchButton.isEnabled = true

                    if (state.trips.isEmpty()) {
                        noResultsText.visibility = View.VISIBLE
                        resultsRecycler.visibility = View.GONE
                        errorText.visibility = View.GONE
                    } else {
                        resultsRecycler.visibility = View.VISIBLE
                        noResultsText.visibility = View.GONE
                        errorText.visibility = View.GONE
                        tripAdapter.setTrips(state.trips)
                    }
                }

                is TripSearchUIState.OriginsLoaded -> {
                    // TODO: Populate origin spinner with state.origins
                    // originAdapter.setData(state.origins)
                }

                is TripSearchUIState.DestinationsLoaded -> {
                    // TODO: Populate destination spinner with state.destinations
                    // destinationAdapter.setData(state.destinations)
                }

                is TripSearchUIState.Error -> {
                    progressBar.visibility = View.GONE
                    resultsRecycler.visibility = View.GONE
                    noResultsText.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                    searchButton.isEnabled = true
                }
            }
        }

        // Step 7: Observe one-time navigation events
        viewModel.navigationEvents.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigationEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                // Other events as needed
            }
        }
    }

    private fun showDatePicker() {
        // TODO: Implement date picker dialog
        // Set result to dateField
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// FRAGMENT 2: SeatSelectionFragment ⭐ CRITICAL
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Fragment for selecting a seat visually.
 *
 * Responsibilities:
 *   • Load bus and available seats
 *   • Display seats in visual grid (6 columns)
 *   • Allow single seat selection
 *   • Show seat legend
 *   • Enable "Proceed" only when seat selected
 *
 * Features:
 *   - 4 seat states: Available, Selected, Booked, Blocked
 *   - Single selection (deselect previous)
 *   - Click rules (only AVAILABLE clickable)
 *   - Visual feedback (colors, icons, legends)
 *
 * Key Challenge:
 *   Coordinating seat grid state with ViewModel state
 *   → Adapter holds selectedSeatId
 *   → Fragment passes selected seat to ViewModel on proceed
 */
class SeatSelectionFragment : Fragment() {

    private val viewModel: SeatSelectionViewModel by viewModels()
    private lateinit var seatAdapter: SeatGridAdapter

    // UI Views
    private lateinit var tripInfoText: android.widget.TextView
    private lateinit var seatGrid: RecyclerView
    private lateinit var proceedButton: android.widget.Button
    private lateinit var cancelButton: android.widget.Button
    private lateinit var progressBar: android.widget.ProgressBar

    // Data
    private var tripId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seat_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Extract trip ID from arguments
        tripId = arguments?.getLong("tripId") ?: 0
        if (tripId == 0L) {
            Toast.makeText(context, "Invalid trip ID", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // Step 1: Find views
        tripInfoText = view.findViewById(R.id.seat_selection_trip_info)
        seatGrid = view.findViewById(R.id.seat_selection_recycler_grid)
        proceedButton = view.findViewById(R.id.seat_selection_proceed_button)
        cancelButton = view.findViewById(R.id.seat_selection_cancel_button)
        progressBar = view.findViewById(R.id.seat_selection_progress_bar)

        // Step 2: Set up seat grid with GridLayoutManager
        seatAdapter = SeatGridAdapter()
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 6)
        seatGrid.apply {
            layoutManager = gridLayoutManager
            adapter = seatAdapter
        }

        // Step 3: Set up seat click listener
        seatAdapter.setOnSeatClickListener { seat ->
            // Adapter auto-selects seat
            // Just enable proceed button
            proceedButton.isEnabled = true
        }

        // Step 4: Set up buttons
        proceedButton.isEnabled = false  // Disabled until seat selected
        proceedButton.setOnClickListener {
            val selectedSeatId = seatAdapter.getSelectedSeatId()
            if (selectedSeatId != null) {
                // Navigate to booking confirmation
                val action = SeatSelectionFragmentDirections
                    .actionSeatSelectionToBookingConfirmation(
                        tripId = tripId,
                        seatId = selectedSeatId
                    )
                findNavController().navigate(action)
            }
        }

        cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Step 5: Load seats for trip
        viewModel.loadSeatsForTrip(tripId)

        // Step 6: Observe UIState ⭐
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SeatSelectionUIState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    seatGrid.visibility = View.GONE
                }

                is SeatSelectionUIState.Loaded -> {
                    progressBar.visibility = View.GONE
                    seatGrid.visibility = View.VISIBLE

                    // Display trip info
                    tripInfoText.text = "${state.trip.route.origin} → ${state.trip.route.destination}\n" +
                            "Date: ${state.trip.trip.tripDate} | Departure: ${state.trip.trip.departureTime}"

                    // Display seats
                    seatAdapter.setSeats(state.seats)
                }

                is SeatSelectionUIState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// FRAGMENT 3: BookingConfirmationFragment ⭐ ATOMIC BOOKING
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Fragment for confirming booking (CRITICAL PHASE).
 *
 * Responsibilities:
 *   • Load booking details (trip + seat + price)
 *   • Display confirmation summary
 *   • Handle CONFIRM → Atomic Room transaction
 *   • Handle AlreadyBooked race condition
 *
 * ATOMIC BOOKING SEQUENCE:
 *   1. User clicks "CONFIRM BOOKING"
 *   2. ViewModel calls TicketRepository.bookTicket()
 *   3. Repository executes Room @Transaction:
 *      ├─ Check if seat booked
 *      ├─ If free: Insert ticket atomically
 *      └─ Only 1 concurrent insert succeeds
 *   4. BookingResult returned:
 *      ├─ Success → Navigate to ticket detail
 *      └─ AlreadyBooked → Show error dialog
 */
class BookingConfirmationFragment : Fragment() {

    private val viewModel: BookingConfirmationViewModel by viewModels()

    // UI Views
    private lateinit var originDestText: android.widget.TextView
    private lateinit var dateText: android.widget.TextView
    private lateinit var timeText: android.widget.TextView
    private lateinit var durationText: android.widget.TextView
    private lateinit var seatText: android.widget.TextView
    private lateinit var priceText: android.widget.TextView
    private lateinit var totalText: android.widget.TextView
    private lateinit var confirmButton: android.widget.Button
    private lateinit var cancelButton: android.widget.Button
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var errorText: android.widget.TextView

    // Data
    private var tripId: Long = 0
    private var seatId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Extract data from arguments
        tripId = arguments?.getLong("tripId") ?: 0
        seatId = arguments?.getLong("seatId") ?: 0

        if (tripId == 0L || seatId == 0L) {
            Toast.makeText(context, "Invalid trip or seat", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // Find views
        originDestText = view.findViewById(R.id.booking_confirmation_origin_dest)
        dateText = view.findViewById(R.id.booking_confirmation_date)
        timeText = view.findViewById(R.id.booking_confirmation_time)
        durationText = view.findViewById(R.id.booking_confirmation_duration)
        seatText = view.findViewById(R.id.booking_confirmation_seat)
        priceText = view.findViewById(R.id.booking_confirmation_price)
        totalText = view.findViewById(R.id.booking_confirmation_total)
        confirmButton = view.findViewById(R.id.booking_confirmation_confirm_button)
        cancelButton = view.findViewById(R.id.booking_confirmation_cancel_button)
        progressBar = view.findViewById(R.id.booking_confirmation_progress_bar)
        errorText = view.findViewById(R.id.booking_confirmation_error_text)

        // Set up buttons
        confirmButton.setOnClickListener {
            // ⭐ ATOMIC BOOKING TRANSACTION
            viewModel.confirmBooking(tripId, seatId)
        }

        cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Load booking details
        viewModel.loadBookingDetails(tripId, seatId)

        // Observe UIState ⭐
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BookingConfirmationUIState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    confirmButton.isEnabled = false
                }

                is BookingConfirmationUIState.Loaded -> {
                    progressBar.visibility = View.GONE
                    confirmButton.isEnabled = true

                    // Display confirmation details
                    val trip = state.bookingDetails
                    originDestText.text = "${trip.originDest}"
                    dateText.text = trip.date
                    timeText.text = trip.time
                    durationText.text = trip.duration
                    seatText.text = trip.seat
                    priceText.text = "₹${trip.price}"
                    totalText.text = "₹${trip.total}"
                }

                is BookingConfirmationUIState.BookingSuccess -> {
                    // ✅ BOOKING SUCCESSFUL
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Booking confirmed!", Toast.LENGTH_SHORT).show()

                    // Navigate to ticket detail
                    val action = BookingConfirmationFragmentDirections
                        .actionBookingConfirmationToTicketDetail(
                            ticketId = state.ticketId
                        )
                    findNavController().navigate(action)
                }

                is BookingConfirmationUIState.SeatAlreadyBooked -> {
                    // ⚠️ RACE CONDITION HANDLED
                    progressBar.visibility = View.GONE
                    showAlreadyBookedDialog()
                }

                is BookingConfirmationUIState.Error -> {
                    progressBar.visibility = View.GONE
                    confirmButton.isEnabled = true
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.message
                }
            }
        }
    }

    private fun showAlreadyBookedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Seat Already Booked")
            .setMessage("This seat was just booked by another user.\nPlease select another seat or search again.")
            .setPositiveButton("Select Again") { _, _ ->
                // Go back to seat selection
                findNavController().popBackStack()
            }
            .setNegativeButton("New Search") { _, _ ->
                // Go back to trip search
                findNavController().popBackStack(R.id.tripSearchFragment, false)
            }
            .show()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// IMPORTS & TODOS
// ════════════════════════════════════════════════════════════════════════════════

/**
 * TODO: Import statements needed
 *
 * import androidx.recyclerview.widget.GridLayoutManager
 * import androidx.recyclerview.widget.LinearLayoutManager
 * import com.example.busbooking.presentation.adapter.SeatGridAdapter
 * import com.example.busbooking.presentation.viewmodel.SeatSelectionViewModel
 * import com.example.busbooking.presentation.viewmodel.BookingConfirmationViewModel
 * import com.example.busbooking.presentation.ui.state.*
 * import com.example.busbooking.presentation.ui.event.NavigationEvent
 */

