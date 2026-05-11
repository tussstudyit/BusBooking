package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.ui.state.AdminState
import com.example.busbooking.presentation.ui.state.UserState
import com.example.busbooking.presentation.viewmodel.AdminViewModel
import com.example.busbooking.presentation.viewmodel.UserViewModel
import com.example.busbooking.utils.SessionManager

// ─────────────────────────────────────────────────────────────────────────────
// 1. AdminDashboardFragment — Điều hướng tới các màn quản lý
// ─────────────────────────────────────────────────────────────────────────────

class AdminDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nav = findNavController()
        view.findViewById<Button>(R.id.manageRoutesButton).setOnClickListener {
            nav.navigate(R.id.action_adminDashboardFragment_to_routeListAdminFragment)
        }
        view.findViewById<Button>(R.id.manageBusesButton).setOnClickListener {
            nav.navigate(R.id.action_adminDashboardFragment_to_busListAdminFragment)
        }
        view.findViewById<Button>(R.id.manageTripsButton).setOnClickListener {
            nav.navigate(R.id.action_adminDashboardFragment_to_tripListAdminFragment)
        }
        view.findViewById<Button>(R.id.manageTicketsButton).setOnClickListener {
            nav.navigate(R.id.action_adminDashboardFragment_to_ticketListAdminFragment)
        }
        view.findViewById<Button>(R.id.manageUsersButton).setOnClickListener {
            nav.navigate(R.id.action_adminDashboardFragment_to_userListAdminFragment)
        }
        view.findViewById<Button>(R.id.analyticsButton).setOnClickListener {
            nav.navigate(R.id.action_adminDashboardFragment_to_analyticsFragment)
        }
        view.findViewById<Button>(R.id.adminProfileButton).setOnClickListener {
            nav.navigate(R.id.action_adminDashboardFragment_to_adminProfileFragment)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. TicketListAdminFragment — Danh sách tất cả vé (chờ inject ViewModel)
// ─────────────────────────────────────────────────────────────────────────────

class TicketListAdminFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_ticket_list_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.ticketRecyclerView)
        progressBar  = view.findViewById(R.id.progressBar)
        emptyText    = view.findViewById(R.id.emptyText)
        errorText    = view.findViewById(R.id.errorText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // TODO: inject TicketViewModel và load tất cả vé
        emptyText.text       = "Không có vé nào"
        emptyText.visibility = View.VISIBLE
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. UserListAdminFragment — Danh sách user + tìm kiếm + block/unblock
// ─────────────────────────────────────────────────────────────────────────────

class UserListAdminFragment : Fragment() {

    private val viewModel: AdminViewModel by viewModels()

    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var errorText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_user_list_admin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchInput  = view.findViewById(R.id.searchInput)
        recyclerView = view.findViewById(R.id.userRecyclerView)
        progressBar  = view.findViewById(R.id.progressBar)
        emptyText    = view.findViewById(R.id.emptyText)
        errorText    = view.findViewById(R.id.errorText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchUsers(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.adminState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AdminState.Loading -> {
                    progressBar.visibility  = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    emptyText.visibility    = View.GONE
                    errorText.visibility    = View.GONE
                }
                is AdminState.UsersLoaded -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility   = View.GONE
                    if (state.users.isEmpty()) {
                        emptyText.text          = "Không tìm thấy người dùng"
                        emptyText.visibility    = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyText.visibility    = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        // TODO: set adapter với state.users
                        // Adapter cần nút block/unblock → viewModel.toggleBlockUser(userId, isBlocked)
                    }
                }
                is AdminState.ActionSuccess -> {
                    progressBar.visibility = View.GONE
                    viewModel.loadAllUsers() // reload sau block/unblock
                }
                is AdminState.Error -> {
                    progressBar.visibility = View.GONE
                    errorText.text         = state.message
                    errorText.visibility   = View.VISIBLE
                }
                else -> progressBar.visibility = View.GONE
            }
        }

        viewModel.loadAllUsers()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. AnalyticsFragment — Tổng quan số liệu (chờ inject ViewModel)
// ─────────────────────────────────────────────────────────────────────────────

class AnalyticsFragment : Fragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var totalTripsText: TextView
    private lateinit var totalTicketsText: TextView
    private lateinit var totalRevenueText: TextView
    private lateinit var totalUsersText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_analytics, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar      = view.findViewById(R.id.progressBar)
        totalTripsText   = view.findViewById(R.id.totalTripsText)
        totalTicketsText = view.findViewById(R.id.totalTicketsText)
        totalRevenueText = view.findViewById(R.id.totalRevenueText)
        totalUsersText   = view.findViewById(R.id.totalUsersText)

        // TODO: inject AdminViewModel / AnalyticsViewModel để tổng hợp số liệu
        totalTripsText.text   = "Tổng chuyến xe: --"
        totalTicketsText.text = "Tổng vé đã bán: --"
        totalRevenueText.text = "Doanh thu: --"
        totalUsersText.text   = "Tổng người dùng: --"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. AdminProfileFragment — Thông tin admin + đăng xuất
// ─────────────────────────────────────────────────────────────────────────────

class AdminProfileFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var phoneText: TextView
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_admin_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameText     = view.findViewById(R.id.nameText)
        emailText    = view.findViewById(R.id.emailText)
        phoneText    = view.findViewById(R.id.phoneText)
        logoutButton = view.findViewById(R.id.logoutButton)

        logoutButton.setOnClickListener { showLogoutDialog() }

        viewModel.userState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UserState.ProfileLoaded -> {
                    nameText.text  = state.user.name
                    emailText.text = state.user.email
                    phoneText.text = state.user.phone
                }
                is UserState.Error -> {
                    nameText.text = "Không thể tải thông tin"
                }
                else -> {}
            }
        }

        viewModel.loadProfile()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                SessionManager.clearSession()
                findNavController().navigate(R.id.action_adminProfileFragment_to_loginFragment)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}