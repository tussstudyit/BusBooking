package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.db.BusBookingDatabase
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.UserRepository
import com.example.busbooking.presentation.ui.state.AdminState
import com.example.busbooking.presentation.viewmodel.AdminViewModel
import com.example.busbooking.presentation.viewmodel.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

// ─────────────────────────────────────────────────────────────────────────────
// 1. RouteListAdminFragment
// ─────────────────────────────────────────────────────────────────────────────

class RouteListAdminFragment : Fragment() {

    private val viewModel: AdminViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            AdminViewModel(
                UserRepository(db.userDao()),
                RouteRepository(db.routeDao())
            )
        }
    }
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var errorText: TextView
    private lateinit var addRouteFab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_route_list_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView  = view.findViewById(R.id.routeRecyclerView)
        progressBar   = view.findViewById(R.id.progressBar)
        emptyText     = view.findViewById(R.id.emptyText)
        errorText     = view.findViewById(R.id.errorText)
        addRouteFab   = view.findViewById(R.id.addRouteFab)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        addRouteFab.setOnClickListener {
            findNavController().navigate(R.id.action_routeListAdminFragment_to_routeFormAdminFragment)
        }

        viewModel.adminState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AdminState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    errorText.visibility = View.GONE
                }
                is AdminState.RoutesLoaded -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    if (state.routes.isEmpty()) {
                        emptyText.text = "Chưa có tuyến nào"
                        emptyText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        // TODO: Set adapter với state.routes
                    }
                }
                is AdminState.Error -> {
                    progressBar.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    errorText.text = state.message
                    errorText.visibility = View.VISIBLE
                }
                else -> {
                    progressBar.visibility = View.GONE
                }
            }
        }

        viewModel.loadAllRoutes()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. RouteFormAdminFragment — Tạo mới hoặc chỉnh sửa tuyến (routeId == -1L = tạo mới)
// ─────────────────────────────────────────────────────────────────────────────

class RouteFormAdminFragment : Fragment() {

    private val viewModel: AdminViewModel by viewModels {
        val db = BusBookingDatabase.getInstance(requireContext())
        ViewModelFactory {
            AdminViewModel(
                UserRepository(db.userDao()),
                RouteRepository(db.routeDao())
            )
        }
    }
    private lateinit var originInput: EditText
    private lateinit var destinationInput: EditText
    private lateinit var distanceInput: EditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    private var routeId: Long = -1L

    private val isEditMode get() = routeId != -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_route_form_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy routeId từ arguments
        routeId = arguments?.getLong("routeId") ?: -1L

        originInput      = view.findViewById(R.id.originInput)
        destinationInput = view.findViewById(R.id.destinationInput)
        distanceInput    = view.findViewById(R.id.distanceInput)
        saveButton       = view.findViewById(R.id.saveButton)
        progressBar      = view.findViewById(R.id.progressBar)
        errorText        = view.findViewById(R.id.errorText)

        saveButton.text = if (isEditMode) "Cập nhật" else "Tạo tuyến"

        saveButton.setOnClickListener {
            val origin      = originInput.text.toString().trim()
            val destination = destinationInput.text.toString().trim()
            val distance    = distanceInput.text.toString().trim().toIntOrNull() ?: 0

            if (origin.isBlank() || destination.isBlank() || distance <= 0) {
                showError("Vui lòng điền đầy đủ thông tin hợp lệ")
                return@setOnClickListener
            }

            viewModel.createRoute(origin, destination, distance)
        }

        viewModel.adminState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AdminState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    saveButton.isEnabled   = false
                    errorText.visibility   = View.GONE
                }
                is AdminState.ActionSuccess -> {
                    progressBar.visibility = View.GONE
                    findNavController().popBackStack()
                }
                is AdminState.Error -> {
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
    }

    private fun showError(message: String) {
        errorText.text       = message
        errorText.visibility = View.VISIBLE
    }
}