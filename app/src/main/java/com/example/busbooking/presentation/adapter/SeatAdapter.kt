package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.entity.Seat

/**
 * SeatAdapter — hiển thị danh sách ghế, hỗ trợ trạng thái selected/available/booked.
 * bookedSeatIds: tập hợp id ghế đã được đặt (không thể chọn).
 */
class SeatAdapter(
    private val bookedSeatIds: Set<Long> = emptySet(),
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<Seat, SeatAdapter.SeatViewHolder>(DiffCallback) {

    private var selectedSeatId: Long? = null

    fun setSelectedSeat(seat: Seat?) {
        val old = selectedSeatId
        selectedSeatId = seat?.id
        // refresh chỉ 2 item thay đổi
        currentList.forEachIndexed { index, s ->
            if (s.id == old || s.id == selectedSeatId) notifyItemChanged(index)
        }
    }

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val seatLabel: TextView = itemView.findViewById(R.id.seatLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = getItem(position)
        val isBooked   = seat.id in bookedSeatIds
        val isSelected = seat.id == selectedSeatId

        holder.seatLabel.text = seat.seatNumber

        val bgColor = when {
            isBooked   -> ContextCompat.getColor(holder.itemView.context, R.color.seat_booked)
            isSelected -> ContextCompat.getColor(holder.itemView.context, R.color.seat_selected)
            else       -> ContextCompat.getColor(holder.itemView.context, R.color.seat_available)
        }
        holder.itemView.setBackgroundColor(bgColor)
        holder.itemView.isEnabled = !isBooked

        holder.itemView.setOnClickListener {
            if (!isBooked) onSeatClick(seat)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Seat>() {
            override fun areItemsTheSame(old: Seat, new: Seat) = old.id == new.id
            override fun areContentsTheSame(old: Seat, new: Seat) = old == new
        }
    }
}