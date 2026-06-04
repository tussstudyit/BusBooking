package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.entity.Seat

/**
 * Adapter cho mÃ n hÃ¬nh chá»n gháº¿.
 * Dung model [Seat] tu API, khong dung SeatLayout.
 * Layout: item_seat.xml (ImageView icon + TextView sá»‘ gháº¿)
 */
class SeatSelectionAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<Seat, SeatSelectionAdapter.SeatViewHolder>(SeatDiff) {

    private var selectedSeatIds: Set<Long> = emptySet()
    private var bookedSeatIds: Set<Long>   = emptySet()

    fun setSelectedSeats(seats: List<Seat>) {
        selectedSeatIds = seats.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    fun setBookedSeats(seats: List<Seat>) {
        bookedSeatIds = seats.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val seatIcon: ImageView  = itemView.findViewById(R.id.seatIcon)
        private val seatText: TextView   = itemView.findViewById(R.id.seatNumberText)

        fun bind(seat: Seat) {
            seatText.text = seat.seatNumber

            val isBooked   = seat.id in bookedSeatIds
            val isSelected = seat.id in selectedSeatIds

            when {
                isBooked -> {
                    seatIcon.setImageResource(R.drawable.ic_seat_booked)
                    itemView.isEnabled = false
                    itemView.setOnClickListener(null)
                }
                isSelected -> {
                    seatIcon.setImageResource(R.drawable.ic_seat_selected)
                    itemView.isEnabled = true
                    itemView.setOnClickListener { onSeatClick(seat) }
                }
                else -> {
                    seatIcon.setImageResource(R.drawable.ic_seat_available)
                    itemView.isEnabled = true
                    itemView.setOnClickListener { onSeatClick(seat) }
                }
            }
        }
    }

    companion object {
        private val SeatDiff = object : DiffUtil.ItemCallback<Seat>() {
            override fun areItemsTheSame(a: Seat, b: Seat) = a.id == b.id
            override fun areContentsTheSame(a: Seat, b: Seat) = a == b
        }
    }
}
