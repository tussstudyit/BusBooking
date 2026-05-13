package com.example.busbooking.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.entity.Seat
import com.google.android.material.card.MaterialCardView

class SeatAdapter(
    private val bookedSeatIds: Set<Long> = emptySet(),
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<Seat, SeatAdapter.SeatViewHolder>(DiffCallback) {

    private val selectedSeatIds = mutableSetOf<Long>()

    fun setSelectedSeats(seats: List<Seat>) {
        selectedSeatIds.clear()
        selectedSeatIds.addAll(seats.map { it.id })
        notifyDataSetChanged()
    }

    inner class SeatViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val seatLabel: TextView =
            itemView.findViewById(R.id.seatLabel)

        val seatCard: MaterialCardView =
            itemView.findViewById(R.id.seatCard)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SeatViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seat, parent, false)

        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {

        val seat = getItem(position)

        val isBooked   = seat.id in bookedSeatIds
        val isSelected = seat.id in selectedSeatIds

        holder.seatLabel.text = seat.seatNumber

        when {

            // =====================
            // ĐÃ BÁN
            // =====================

            isBooked -> {

                holder.seatCard.setCardBackgroundColor(
                    Color.parseColor("#E0E0E0")
                )

                holder.seatCard.strokeColor =
                    Color.parseColor("#E0E0E0")

                holder.seatLabel.setTextColor(Color.GRAY)

                holder.itemView.isEnabled = false
            }

            // =====================
            // ĐANG CHỌN
            // =====================

            isSelected -> {

                holder.seatCard.setCardBackgroundColor(
                    Color.parseColor("#FF4DA6")
                )

                holder.seatCard.strokeColor =
                    Color.parseColor("#FF4DA6")

                holder.seatLabel.setTextColor(Color.WHITE)

                holder.itemView.isEnabled = true
            }

            // =====================
            // CÒN TRỐNG
            // =====================

            else -> {

                holder.seatCard.setCardBackgroundColor(Color.WHITE)

                holder.seatCard.strokeColor =
                    Color.parseColor("#4CAF50")

                holder.seatLabel.setTextColor(Color.BLACK)

                holder.itemView.isEnabled = true
            }
        }

        holder.itemView.setOnClickListener {

            if (isBooked) return@setOnClickListener

            if (isSelected) {
                selectedSeatIds.remove(seat.id)
            } else {
                selectedSeatIds.add(seat.id)
            }

            notifyItemChanged(position)

            onSeatClick(seat)

            // animation click
            holder.itemView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(80)
                .withEndAction {
                    holder.itemView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .duration = 80
                }
        }
    }

    companion object {

        private val DiffCallback =
            object : DiffUtil.ItemCallback<Seat>() {

                override fun areItemsTheSame(
                    old: Seat,
                    new: Seat
                ) = old.id == new.id

                override fun areContentsTheSame(
                    old: Seat,
                    new: Seat
                ) = old == new
            }
    }
}