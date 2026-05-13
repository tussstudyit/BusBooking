package com.example.busbooking.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.example.busbooking.R
import com.example.busbooking.data.model.Province
import com.example.busbooking.data.model.Stop
import com.example.busbooking.databinding.FragmentLocationPickerBottomSheetBinding
import com.example.busbooking.databinding.ItemProvinceBinding
import com.example.busbooking.databinding.ItemStopBinding
import com.example.busbooking.presentation.viewmodel.ProvinceViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LocationPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentLocationPickerBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProvinceViewModel by viewModels()

    private var onStopSelected: ((Stop) -> Unit)? = null

    // ─── Factory ─────────────────────────────────────────────────────────────

    companion object {
        const val TAG = "LocationPickerBottomSheet"
        private const val ARG_TITLE = "arg_title"

        fun newInstance(
            title: String,
            onStopSelected: (Stop) -> Unit
        ): LocationPickerBottomSheet {
            return LocationPickerBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_TITLE, title) }
                this.onStopSelected = onStopSelected
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bắt buộc để bg_bottom_sheet.xml (bo góc) được áp dụng
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationPickerBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        binding.bottomSheetTitle.text = arguments?.getString(ARG_TITLE) ?: "Chọn điểm"
        binding.closeButton.setOnClickListener { dismiss() }

        setupSearch()
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.clearSearchCache()
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener { text ->
            viewModel.searchProvinces(text.toString())
        }
    }

    // ─── Observe ──────────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.provinces.observe(viewLifecycleOwner) { provinces ->
            binding.progressBar.visibility = View.GONE
            renderProvinces(provinces)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        viewModel.selectedStop.observe(viewLifecycleOwner) { stop ->
            stop?.let {
                onStopSelected?.invoke(it)
                dismiss()
            }
        }
    }

    // ─── Render ───────────────────────────────────────────────────────────────

    private fun renderProvinces(provinces: List<Province>) {
        binding.provincesContainer.removeAllViews()

        provinces.forEach { province ->
            val provinceBinding = ItemProvinceBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.provincesContainer,
                false
            )

            provinceBinding.provinceName.text = province.name

            province.stops.forEach { stop ->
                val stopBinding = ItemStopBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    provinceBinding.stopsContainer,
                    false
                )
                stopBinding.stopName.text = stop.name
                stopBinding.stopAddress.text = stop.province
                stopBinding.root.setOnClickListener {
                    viewModel.selectStop(stop)
                }
                provinceBinding.stopsContainer.addView(stopBinding.root)
            }

            // Thêm mới: xử lý expand/collapse
            provinceBinding.provinceHeader.setOnClickListener {
                val isExpanded = provinceBinding.stopsContainer.visibility == View.VISIBLE
                provinceBinding.stopsContainer.visibility = if (isExpanded) View.GONE else View.VISIBLE
                provinceBinding.expandIcon.rotation = if (isExpanded) 0f else 180f
            }

            binding.provincesContainer.addView(provinceBinding.root)
        }
    }
}