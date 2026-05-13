package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.databinding.ItemProvinceBinding
import com.example.busbooking.databinding.ItemStopBinding
import com.example.busbooking.data.model.Province
import com.example.busbooking.data.model.Stop

/**
 * Adapter tối ưu để hiển thị danh sách tỉnh/điểm dừng
 * - Reuse ViewHolder hiệu quả
 * - Expandable/collapsible provinces
 * - Smooth animations
 */
class ProvinceAdapter(
    private val onStopSelected: (Stop) -> Unit
) : RecyclerView.Adapter<ProvinceAdapter.ProvinceViewHolder>() {

    private var provinces: List<Province> = emptyList()
    private val expandedProvinces = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProvinceViewHolder {
        val binding = ItemProvinceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProvinceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProvinceViewHolder, position: Int) {
        holder.bind(provinces[position])
    }

    override fun getItemCount() = provinces.size

    /**
     * Update danh sách provinces (smooth update)
     */
    fun updateProvinces(newProvinces: List<Province>) {
        // Clear expanded states nếu provinces thay đổi
        if (newProvinces.size != provinces.size) {
            expandedProvinces.clear()
        }

        provinces = newProvinces
        notifyDataSetChanged()
    }

    /**
     * Toggle expand/collapse cho province
     */
    private fun toggleExpand(provinceId: String, position: Int) {
        if (expandedProvinces.contains(provinceId)) {
            expandedProvinces.remove(provinceId)
        } else {
            expandedProvinces.add(provinceId)
        }
        notifyItemChanged(position)
    }

    inner class ProvinceViewHolder(private val binding: ItemProvinceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(province: Province) {
            binding.apply {
                provinceName.text = province.name

                val isExpanded = expandedProvinces.contains(province.id)

                // Update UI state
                updateExpandState(isExpanded)

                // Render stops
                renderStops(province, isExpanded)

                // Handle header click
                provinceHeader.setOnClickListener {
                    toggleExpand(province.id, bindingAdapterPosition)
                }
            }
        }

        private fun updateExpandState(isExpanded: Boolean) {
            binding.apply {
                // Show/hide stops container
                stopsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

                // Rotate expand icon
                expandIcon.animate()
                    .rotation(if (isExpanded) 180f else 0f)
                    .setDuration(300)
                    .start()
            }
        }

        private fun renderStops(province: Province, isExpanded: Boolean) {
            binding.apply {
                // Xóa views cũ
                stopsContainer.removeAllViews()

                // Chỉ render stops nếu mở rộng
                if (isExpanded && province.stops.isNotEmpty()) {
                    province.stops.forEach { stop ->
                        val stopBinding = ItemStopBinding.inflate(
                            LayoutInflater.from(root.context),
                            stopsContainer,
                            false
                        )
                        stopBinding.apply {
                            stopName.text = stop.name
                            stopAddress.text = stop.province

                            // Click listener
                            root.setOnClickListener {
                                onStopSelected(stop)
                            }
                        }
                        stopsContainer.addView(stopBinding.root)
                    }
                }
            }
        }
    }
}

