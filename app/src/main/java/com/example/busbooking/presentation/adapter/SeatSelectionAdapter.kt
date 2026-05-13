package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.model.SeatLayout
import com.example.busbooking.databinding.ItemSeatBinding

/**
 * Production-ready Seat Selection Adapter
 *
 * Features:
 * - Smooth animations with DiffUtil
 * - Click listeners for seat selection
 * - Visual distinction: available, selected, booked, disabled
 * - Window seat highlighting
 * - Aisle space handling
 *
 * Usage:
 * ```kotlin
 * val adapter = SeatSelectionAdapter { seat ->
 *     // User clicked seat
 *     onSeatClicked(seat)
 * }
 * recyclerView.adapter = adapter
 * adapter.submitList(seats)
 * ```
 */
class SeatSelectionAdapter(
    private val onSeatSelected: (SeatLayout, isSelected: Boolean) -> Unit
) : ListAdapter<SeatLayout, SeatSelectionAdapter.SeatViewHolder>(SeatDiffCallback()) {

    private val selectedSeats = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemSeatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = getItem(position)
        holder.bind(seat, seat.seatNumber in selectedSeats)
    }

    fun getSelectedSeats(): List<SeatLayout> {
        return currentList.filter { it.seatNumber in selectedSeats }
    }

    fun clearSelection() {
        selectedSeats.clear()
        notifyDataSetChanged()
    }

    inner class SeatViewHolder(private val binding: ItemSeatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(seat: SeatLayout, isSelected: Boolean) {
            binding.apply {
                val context = root.context
                val isSelectable = !seat.isBooked && !seat.isAisle && !seat.isLocked

                // Display
                seatLabel.text = seat.seatNumber
                seatLabel.visibility = if (seat.isAisle) android.view.View.INVISIBLE else android
                    .view.View.VISIBLE

                // Color based on state
                val bgColor = when {
                    seat.isAisle -> android.R.color.transparent // Transparent for aisle
                    isSelected -> R.color.green_primary        // User selected
                    seat.isBooked -> R.color.red_500           // Already booked
                    seat.isLocked -> R.color.orange_500        // Temporary lock
                    seat.isWindow -> R.color.blue_primary      // Window seat (premium)
                    else -> R.color.light_gray                 // Available
                }

                root.setBackgroundColor(ContextCompat.getColor(context, bgColor))

                // Clickable only if available
                root.isEnabled = isSelectable
                root.alpha = if (isSelectable) 1.0f else 0.5f

                // Click handler
                if (isSelectable) {
                    root.setOnClickListener {
                        val newState = seat.seatNumber !in selectedSeats
                        if (newState) {
                            selectedSeats.add(seat.seatNumber)
                        } else {
                            selectedSeats.remove(seat.seatNumber)
                        }
                        onSeatSelected(seat, newState)
                        notifyItemChanged(bindingAdapterPosition)
                    }
                } else {
                    root.setOnClickListener(null)
                }

                // Accessibility
                root.contentDescription = buildString {
                    append("Seat ${seat.seatNumber}")
                    append(" - Floor ${seat.floor}")
                    append(if (seat.isWindow) ", Window seat" else "")
                    append(if (seat.isBooked) ", Already booked" else "")
                    if (isSelected) append(", Selected")
                }
            }
        }
    }

    private class SeatDiffCallback : DiffUtil.ItemCallback<SeatLayout>() {
        override fun areItemsTheSame(oldItem: SeatLayout, newItem: SeatLayout): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SeatLayout, newItem: SeatLayout): Boolean {
            return oldItem.isBooked == newItem.isBooked &&
                    oldItem.isLocked == newItem.isLocked
        }
    }
}

