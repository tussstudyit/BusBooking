package com.example.busbooking.presentation.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.busbooking.R
import com.example.busbooking.domain.repository.AuthRepository
import com.example.busbooking.domain.repository.RouteRepository
import com.example.busbooking.domain.repository.TicketRepository
import com.example.busbooking.presentation.adapter.BannerAdapter
import com.example.busbooking.presentation.adapter.PopularRouteAdapter
import com.example.busbooking.presentation.adapter.PromotionAdapter
import com.example.busbooking.presentation.adapter.UpcomingTripAdapter
import com.example.busbooking.presentation.viewmodel.HomeState
import com.example.busbooking.presentation.viewmodel.HomeViewModel
import com.example.busbooking.presentation.viewmodel.ViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory {
            HomeViewModel(
                authRepository = AuthRepository(),
                ticketRepository = TicketRepository(),
                routeRepository = RouteRepository()
            )
        }
    }

    // ─── Views ────────────────────────────────────────────────────────────────
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var userNameText: TextView
    private lateinit var greetingText: TextView
    private lateinit var bannerViewPager: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var originInput: MaterialAutoCompleteTextView
    private lateinit var destinationInput: MaterialAutoCompleteTextView
    private lateinit var datePickerButton: MaterialButton
    private lateinit var returnDatePickerButton: MaterialButton
    private lateinit var returnDateContainer: View
    private lateinit var returnDateDivider: View
    private lateinit var oneWayButton: MaterialButton
    private lateinit var roundTripButton: MaterialButton
    private lateinit var swapButton: ImageButton
    private lateinit var searchButton: MaterialButton
    private lateinit var seeAllTicketsText: TextView
    private lateinit var seeAllRoutesText: TextView
    private lateinit var upcomingRecyclerView: RecyclerView
    private lateinit var upcomingEmptyText: TextView
    private lateinit var popularRoutesRecyclerView: RecyclerView
    private lateinit var promotionsRecyclerView: RecyclerView

    // ─── Adapters ─────────────────────────────────────────────────────────────
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var upcomingAdapter: UpcomingTripAdapter
    private lateinit var popularRouteAdapter: PopularRouteAdapter
    private lateinit var promotionAdapter: PromotionAdapter

    // ─── State ────────────────────────────────────────────────────────────────
    private var selectedDate: Long = System.currentTimeMillis()
    private var selectedReturnDate: Long = 0L
    private var isRoundTrip: Boolean = false
    private var originTag: String = ""
    private var destinationTag: String = ""
    private var refreshHomeOnResume: Boolean = false

    // ─── Real-time date updater ───────────────────────────────────────────────
    private val dateHandler = Handler(Looper.getMainLooper())
    private val dateUpdater = object : Runnable {
        override fun run() {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (selectedDate < todayStart) {
                selectedDate = todayStart
                datePickerButton.text = formatDate(selectedDate)
                if (selectedReturnDate in 1..<selectedDate) {
                    selectedReturnDate = 0L
                    returnDatePickerButton.text = "Chọn ngày"
                }
            }

            val now = System.currentTimeMillis()
            val nextMidnight = todayStart + 24 * 60 * 60 * 1000L
            dateHandler.postDelayed(this, nextMidnight - now)
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        datePickerButton.text = formatDate(selectedDate)
        dateHandler.post(dateUpdater)

        // Khôi phục lại text nếu đã chọn trước đó
        if (originTag.isNotEmpty()) originInput.setText(originTag)
        if (destinationTag.isNotEmpty()) destinationInput.setText(destinationTag)

        setupAdapters()
        setupListeners()
        observeViewModel()
        viewModel.loadHome()
    }

    override fun onResume() {
        super.onResume()
        if (refreshHomeOnResume) {
            viewModel.refresh()
        } else {
            refreshHomeOnResume = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dateHandler.removeCallbacks(dateUpdater)
    }

    // ─── Bind views ───────────────────────────────────────────────────────────
    private fun bindViews(view: View) {
        swipeRefresh              = view.findViewById(R.id.swipeRefresh)
        progressBar               = view.findViewById(R.id.progressBar)
        userNameText              = view.findViewById(R.id.userNameText)
        greetingText              = view.findViewById(R.id.greetingText)
        bannerViewPager           = view.findViewById(R.id.bannerViewPager)
        dotsContainer             = view.findViewById(R.id.dotsContainer)
        originInput               = view.findViewById(R.id.originInput)
        destinationInput          = view.findViewById(R.id.destinationInput)
        datePickerButton          = view.findViewById(R.id.datePickerButton)
        returnDatePickerButton    = view.findViewById(R.id.returnDatePickerButton)
        returnDateContainer       = view.findViewById(R.id.returnDateContainer)
        returnDateDivider         = view.findViewById(R.id.returnDateDivider)
        oneWayButton              = view.findViewById(R.id.oneWayButton)
        roundTripButton           = view.findViewById(R.id.roundTripButton)
        swapButton                = view.findViewById(R.id.swapButton)
        searchButton              = view.findViewById(R.id.searchButton)
        seeAllTicketsText         = view.findViewById(R.id.seeAllTicketsText)
        seeAllRoutesText          = view.findViewById(R.id.seeAllRoutesText)
        upcomingRecyclerView      = view.findViewById(R.id.upcomingRecyclerView)
        upcomingEmptyText         = view.findViewById(R.id.upcomingEmptyText)
        popularRoutesRecyclerView = view.findViewById(R.id.popularRoutesRecyclerView)
        promotionsRecyclerView    = view.findViewById(R.id.promotionsRecyclerView)

        originInput.isFocusable            = false
        originInput.isFocusableInTouchMode = false
        destinationInput.isFocusable            = false
        destinationInput.isFocusableInTouchMode = false
    }

    // ─── Adapters ─────────────────────────────────────────────────────────────
    private fun setupAdapters() {
        bannerAdapter = BannerAdapter()
        bannerViewPager.adapter = bannerAdapter
        bannerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })

        upcomingAdapter = UpcomingTripAdapter { ticketId ->
            val bundle = Bundle().apply { putLong("ticketId", ticketId) }
            findNavController().navigate(R.id.action_homeFragment_to_ticketDetailsFragment, bundle)
        }
        upcomingRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        upcomingRecyclerView.adapter = upcomingAdapter

        popularRouteAdapter = PopularRouteAdapter { route ->
            originInput.setText(route.origin)
            originTag = route.origin
            destinationInput.setText(route.destination)
            destinationTag = route.destination
        }
        popularRoutesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        popularRoutesRecyclerView.adapter = popularRouteAdapter

        promotionAdapter = PromotionAdapter()
        promotionsRecyclerView.adapter = promotionAdapter
    }

    // ─── Listeners ────────────────────────────────────────────────────────────
    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        oneWayButton.setOnClickListener {
            isRoundTrip = false
            returnDateContainer.visibility = View.GONE
            returnDateDivider.visibility   = View.GONE
            oneWayButton.setTextColor(resources.getColor(R.color.blue_primary, null))
            roundTripButton.setTextColor(resources.getColor(R.color.text_primary, null))
        }

        roundTripButton.setOnClickListener {
            isRoundTrip = true
            returnDateContainer.visibility = View.VISIBLE
            returnDateDivider.visibility   = View.VISIBLE
            roundTripButton.setTextColor(resources.getColor(R.color.blue_primary, null))
            oneWayButton.setTextColor(resources.getColor(R.color.text_primary, null))
        }

        datePickerButton.setOnClickListener { showDatePicker() }
        returnDatePickerButton.setOnClickListener { showReturnDatePicker() }

        originInput.setOnClickListener {
            LocationPickerBottomSheet
                .newInstance(title = "Chọn điểm đi") { stop ->
                    originInput.setText(stop.name)
                    originInput.tag = stop.province
                    originTag = stop.province
                }
                .show(childFragmentManager, LocationPickerBottomSheet.TAG)
        }

        destinationInput.setOnClickListener {
            LocationPickerBottomSheet
                .newInstance(title = "Chọn điểm đến") { stop ->
                    destinationInput.setText(stop.name)
                    destinationInput.tag = stop.province
                    destinationTag = stop.province
                }
                .show(childFragmentManager, LocationPickerBottomSheet.TAG)
        }

        swapButton.setOnClickListener {
            val tmpText = originInput.text.toString()
            val tmpTag  = originTag

            originInput.setText(destinationInput.text.toString())
            originInput.tag = destinationTag
            originTag = destinationTag

            destinationInput.setText(tmpText)
            destinationInput.tag = tmpTag
            destinationTag = tmpTag
        }

        searchButton.setOnClickListener {
            val origin      = originTag.trim()
            val destination = destinationTag.trim()

            if (origin.isEmpty() || destination.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập điểm đi và điểm đến", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (origin == destination) {
                Toast.makeText(requireContext(), "Điểm đi và điểm đến không được trùng nhau", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isRoundTrip && selectedReturnDate == 0L) {
                Toast.makeText(requireContext(), "Vui lòng chọn ngày về", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isRoundTrip && selectedReturnDate < selectedDate) {
                Toast.makeText(requireContext(), "Ngày về phải sau ngày đi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putString("origin", origin)
                putString("destination", destination)
                putLong("tripDate", selectedDate)
                putBoolean("isRoundTrip", isRoundTrip)
                if (isRoundTrip) putLong("returnDate", selectedReturnDate)
            }
            findNavController().navigate(R.id.action_homeFragment_to_tripListFragment, bundle)
        }

        seeAllTicketsText.setOnClickListener {
            findNavController().navigate(R.id.myTicketsFragment)
        }

        seeAllRoutesText.setOnClickListener {
            findNavController().navigate(R.id.routeSearchFragment)
        }
    }

    // ─── Observe ──────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.homeState.observe(viewLifecycleOwner) { state ->
            swipeRefresh.isRefreshing = false

            when (state) {
                is HomeState.Loading -> progressBar.visibility = View.VISIBLE

                is HomeState.Success -> {
                    progressBar.visibility = View.GONE

                    val name = state.user?.name?.takeIf { it.isNotBlank() } ?: "Khách"
                    userNameText.text = name
                    greetingText.text = buildGreeting()

                    bannerAdapter.submitList(state.banners)
                    setupDots(state.banners.size)

                    if (state.upcomingTickets.isEmpty()) {
                        upcomingEmptyText.visibility    = View.VISIBLE
                        upcomingRecyclerView.visibility = View.GONE
                    } else {
                        upcomingEmptyText.visibility    = View.GONE
                        upcomingRecyclerView.visibility = View.VISIBLE
                        upcomingAdapter.submitList(state.upcomingTickets)
                    }

                    popularRouteAdapter.submitList(state.popularRoutes)
                    promotionAdapter.submitList(state.promotions)
                }

                is HomeState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private fun formatDate(millis: Long): String =
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(millis))

    private fun buildGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Chào buổi sáng ☀️"
            hour < 18 -> "Chào buổi chiều 🌤"
            else      -> "Chào buổi tối 🌙"
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            cal.set(year, month, day, 0, 0, 0)
            selectedDate = cal.timeInMillis
            datePickerButton.text = formatDate(selectedDate)
            if (selectedReturnDate in 1..<selectedDate) {
                selectedReturnDate = 0L
                returnDatePickerButton.text = "Chọn ngày"
            }
        },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun showReturnDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            cal.set(year, month, day, 0, 0, 0)
            selectedReturnDate = cal.timeInMillis
            returnDatePickerButton.text = formatDate(selectedReturnDate)
        },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = selectedDate
            show()
        }
    }

    // ─── Dots ─────────────────────────────────────────────────────────────────
    private fun setupDots(count: Int) {
        dotsContainer.removeAllViews()
        repeat(count) { i ->
            val dot = View(requireContext()).apply {
                val size = if (i == 0) 10 else 8
                layoutParams = LinearLayout.LayoutParams(dpToPx(size), dpToPx(size))
                    .apply { setMargins(dpToPx(4), 0, dpToPx(4), 0) }
                background = requireContext().getDrawable(
                    if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(selectedIndex: Int) {
        for (i in 0 until dotsContainer.childCount) {
            val dot  = dotsContainer.getChildAt(i)
            val size = if (i == selectedIndex) 10 else 8
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply {
                width  = dpToPx(size)
                height = dpToPx(size)
            }
            dot.background = requireContext().getDrawable(
                if (i == selectedIndex) R.drawable.dot_active else R.drawable.dot_inactive
            )
            dot.requestLayout()
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
