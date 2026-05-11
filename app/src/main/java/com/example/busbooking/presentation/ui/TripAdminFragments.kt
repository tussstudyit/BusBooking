package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.ui.state.TripState
import com.example.busbooking.presentation.viewmodel.TripViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

// ─────────────────────────────────────────────────────────────────────────────
// 1. TripListAdminFragment
// ─────────────────────────────────────────────────────────────────────────────

class TripListAdminFragment : Fragment() {

    private val viewModel: TripViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var errorText: TextView
    private lateinit var addTripFab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_trip_list_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.tripRecyclerView)
        progressBar  = view.findViewById(R.id.progressBar)
        emptyText    = view.findViewById(R.id.emptyText)
        errorText    = view.findViewById(R.id.errorText)
        addTripFab   = view.findViewById(R.id.addTripFab)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        addTripFab.setOnClickListener {
            findNavController().navigate(R.id.action_tripListAdminFragment_to_tripFormAdminFragment)
        }

        viewModel.tripState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripState.Loading -> {
                    progressBar.visibility  = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility    = View.GONE
                    errorText.visibility    = View.GONE
                }
                is TripState.SearchSuccess -> {
                    progressBar.visibility  = View.GONE
                    errorText.visibility    = View.GONE
                    emptyText.visibility    = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    // TODO: set adapter với state.trips
                }
                is TripState.Empty -> {
                    progressBar.visibility  = View.GONE
                    recyclerView.visibility = View.GONE
                    errorText.visibility    = View.GONE
                    emptyText.text          = state.message
                    emptyText.visibility    = View.VISIBLE
                }
                is TripState.Error -> {
                    progressBar.visibility  = View.GONE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility    = View.GONE
                    errorText.text          = state.message
                    errorText.visibility    = View.VISIBLE
                }
                else -> progressBar.visibility = View.GONE
            }
        }

        viewModel.loadAllTrips()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. TripFormAdminFragment — Tạo mới hoặc chỉnh sửa chuyến (tripId == -1L = tạo mới)
// ─────────────────────────────────────────────────────────────────────────────

class TripFormAdminFragment : Fragment() {

    private val viewModel: TripViewModel by viewModels()

    private lateinit var routeSpinner: Spinner
    private lateinit var busSpinner: Spinner
    private lateinit var departureDateInput: EditText
    private lateinit var departureTimeInput: EditText
    private lateinit var arrivalTimeInput: EditText
    private lateinit var priceInput: EditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    private var tripId: Long = -1L

    private val isEditMode get() = tripId != -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_trip_form_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy tripId từ arguments
        tripId = arguments?.getLong("tripId") ?: -1L

        routeSpinner       = view.findViewById(R.id.routeSpinner)
        busSpinner         = view.findViewById(R.id.busSpinner)
        departureDateInput = view.findViewById(R.id.departureDateInput)
        departureTimeInput = view.findViewById(R.id.departureTimeInput)
        arrivalTimeInput   = view.findViewById(R.id.arrivalTimeInput)
        priceInput         = view.findViewById(R.id.priceInput)
        saveButton         = view.findViewById(R.id.saveButton)
        progressBar        = view.findViewById(R.id.progressBar)
        errorText          = view.findViewById(R.id.errorText)

        saveButton.text = if (isEditMode) "Cập nhật" else "Tạo chuyến"

        saveButton.setOnClickListener {
            val price = priceInput.text.toString().trim().toDoubleOrNull()
            if (price == null || price <= 0) {
                showError("Giá vé không hợp lệ")
                return@setOnClickListener
            }

            // TODO: Validate thêm các fields khác: route, bus, departure, arrival
            // Lấy giá trị từ spinners và inputs, sau đó gọi viewModel.createTrip()
            // Mặc thời để tạm: findNavController().popBackStack()
            findNavController().popBackStack()
        }

        viewModel.tripState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    saveButton.isEnabled   = false
                    errorText.visibility   = View.GONE
                }
                is TripState.Empty,
                is TripState.DetailSuccess -> {
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled   = true
                    if (isEditMode && state is TripState.DetailSuccess) {
                        // TODO: Populate fields từ state.trip
                        // routeSpinner.setSelection(...)
                        // busSpinner.setSelection(...)
                        // departureDateInput.setText(...)
                    }
                }
                is TripState.Error -> {
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled   = true
                    showError(state.message)
                }
                else -> {
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled   = true
                }
            }
        }

        // Load trip data nếu chế độ edit
        if (isEditMode) {
            // TODO: viewModel.loadTripById(tripId)
        }
    }

    private fun showError(message: String) {
        errorText.text       = message
        errorText.visibility = View.VISIBLE
    }
}