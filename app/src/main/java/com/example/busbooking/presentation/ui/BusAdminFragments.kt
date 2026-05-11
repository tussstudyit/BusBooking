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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.viewmodel.BusViewModel
import com.example.busbooking.presentation.viewmodel.BusState
import com.google.android.material.floatingactionbutton.FloatingActionButton

// ─────────────────────────────────────────────────────────────────────────────
// 1. BusListAdminFragment
// ─────────────────────────────────────────────────────────────────────────────

class BusListAdminFragment : Fragment() {

    private val viewModel: BusViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var errorText: TextView
    private lateinit var addBusFab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_bus_list_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.busRecyclerView)
        progressBar  = view.findViewById(R.id.progressBar)
        emptyText    = view.findViewById(R.id.emptyText)
        errorText    = view.findViewById(R.id.errorText)
        addBusFab    = view.findViewById(R.id.addBusFab)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        addBusFab.setOnClickListener {
            findNavController().navigate(R.id.action_busListAdminFragment_to_busFormAdminFragment)
        }

        viewModel.busState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BusState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    errorText.visibility = View.GONE
                }
                is BusState.BusesLoaded -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    if (state.buses.isEmpty()) {
                        emptyText.text = "Chưa có xe nào"
                        emptyText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        // TODO: Set adapter với state.buses
                    }
                }
                is BusState.Error -> {
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

        viewModel.loadAllBuses()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. BusFormAdminFragment — Tạo mới hoặc chỉnh sửa xe (busId == -1L = tạo mới)
// ─────────────────────────────────────────────────────────────────────────────

class BusFormAdminFragment : Fragment() {

    private val viewModel: BusViewModel by viewModels()

    private lateinit var busNameInput: EditText
    private lateinit var licensePlateInput: EditText
    private lateinit var totalSeatsInput: EditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    private var busId: Long = -1L

    private val isEditMode get() = busId != -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_bus_form_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy busId từ arguments
        busId = arguments?.getLong("busId") ?: -1L

        busNameInput      = view.findViewById(R.id.busNameInput)
        licensePlateInput = view.findViewById(R.id.licensePlateInput)
        totalSeatsInput   = view.findViewById(R.id.totalSeatsInput)
        saveButton        = view.findViewById(R.id.saveButton)
        progressBar       = view.findViewById(R.id.progressBar)
        errorText         = view.findViewById(R.id.errorText)

        saveButton.text = if (isEditMode) "Cập nhật" else "Tạo xe"

        saveButton.setOnClickListener {
            val busName      = busNameInput.text.toString().trim()
            val licensePlate = licensePlateInput.text.toString().trim()
            val totalSeats   = totalSeatsInput.text.toString().trim().toIntOrNull() ?: 0

            if (busName.isBlank() || licensePlate.isBlank() || totalSeats <= 0) {
                showError("Vui lòng điền đầy đủ thông tin hợp lệ")
                return@setOnClickListener
            }

            if (isEditMode) {
                viewModel.updateBus(busId, busName, totalSeats, licensePlate)
            } else {
                viewModel.createBus(busName, totalSeats, licensePlate)
            }
        }

        viewModel.busState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BusState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    saveButton.isEnabled = false
                    errorText.visibility = View.GONE
                }
                is BusState.ActionSuccess -> {
                    progressBar.visibility = View.GONE
                    findNavController().popBackStack()
                }
                is BusState.Error -> {
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled = true
                    showError(state.message)
                }
                else -> {
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled = true
                }
            }
        }

        // Load dữ liệu nếu chế độ edit
        if (isEditMode) {
            viewModel.loadBusById(busId)
        }
    }

    private fun showError(message: String) {
        errorText.text       = message
        errorText.visibility = View.VISIBLE
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. SeatManagementFragment — Quản lý ghế của một xe (theo args.busId)
// ─────────────────────────────────────────────────────────────────────────────

class SeatManagementFragment : Fragment() {

    private val viewModel: BusViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var errorText: TextView
    private lateinit var generateSeatsButton: Button

    private var busId: Long = -1L
    private var totalSeatsForBus: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_seat_management, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy busId từ arguments
        busId = arguments?.getLong("busId") ?: -1L
        if (busId == -1L) {
            // Nếu không có busId, pop back stack
            findNavController().popBackStack()
            return
        }

        recyclerView        = view.findViewById(R.id.seatsRecyclerView)
        progressBar         = view.findViewById(R.id.progressBar)
        emptyText           = view.findViewById(R.id.emptyText)
        errorText           = view.findViewById(R.id.errorText)
        generateSeatsButton = view.findViewById(R.id.generateSeatsButton)

        // Hiển thị ghế dạng lưới 4 cột (bố cục xe khách thông thường)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)

        generateSeatsButton.setOnClickListener {
            // Lấy tổng số ghế từ bus info, mặc định 40
            totalSeatsForBus = 40 // TODO: Update từ bus data
            viewModel.generateSeats(busId, totalSeatsForBus)
        }

        viewModel.busState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BusState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    errorText.visibility = View.GONE
                }
                is BusState.SeatsLoaded -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    if (state.seats.isEmpty()) {
                        emptyText.text = "Chưa có ghế nào. Nhấn 'Tạo ghế' để tạo tự động."
                        emptyText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        // TODO: Set adapter với state.seats
                    }
                }
                is BusState.ActionSuccess -> {
                    progressBar.visibility = View.GONE
                    viewModel.loadSeatsByBus(busId)
                }
                is BusState.Error -> {
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

        viewModel.loadSeatsByBus(busId)
    }
}