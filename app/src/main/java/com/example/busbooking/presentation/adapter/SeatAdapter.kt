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

class SeatAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<SeatItem, RecyclerView.ViewHolder>(SeatItemDiff) {

    private var selectedSeats: Set<Long> = emptySet()
    private var bookedSeatIds: Set<Long> = emptySet()

    fun setSelectedSeats(seats: List<Seat>) {
        selectedSeats = seats.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    fun setBookedSeats(seats: List<Seat>) {
        bookedSeatIds = seats.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    /**
     * Sắp xếp ghế theo layout xe giường nằm:
     * Cột 0 (trái): ghế lẻ  → 1,3,6,9,12,15...
     * Cột 1 (giữa): lối đi  → placeholder
     * Cột 2 (phải): ghế chẵn → 2,5,8,11,14,17...
     * Cột 1 hàng 1: ghế 4,7,10,13,16
     */
    fun submitSeats(seats: List<Seat>) {
        val floor1 = seats.filter { it.floor == 1 }.sortedBy { it.seatNumber }
        val floor2 = seats.filter { it.floor == 2 }.sortedBy { it.seatNumber }
        submitList(buildSeatItems(floor1) + buildSeatItems(floor2))
    }

    private fun buildSeatItems(seats: List<Seat>): List<SeatItem> {
        // Map seatNumber → Seat
        val seatMap = seats.associateBy { it.seatNumber }

        // Lấy số hàng tối đa
        val maxRow = seats.maxOfOrNull {
            it.seatNumber.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
        } ?: 0

        val items = mutableListOf<SeatItem>()

        // Layout 3 cột: [trái] [giữa] [phải]
        // Hàng 1: A01(trái), EMPTY(giữa), A02(phải)
        // Hàng 2: A03(trái), A04(giữa),  A05(phải)
        // Hàng 3: A06(trái), EMPTY(giữa), A07(phải) ...

        var row = 1
        var seatIdx = 1

        while (seatIdx <= maxRow) {
            val prefix = if (seats.firstOrNull()?.seatNumber?.startsWith("A") == true) "A" else "B"

            // Hàng lẻ: trái + EMPTY + phải
            val leftNum  = String.format("%02d", seatIdx)
            val rightNum = String.format("%02d", seatIdx + 1)

            items.add(seatMap["$prefix$leftNum"]?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
            items.add(SeatItem.Empty) // lối đi
            items.add(seatMap["$prefix$rightNum"]?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)

            seatIdx += 2

            // Hàng giữa (cột giữa có ghế)
            if (seatIdx <= maxRow) {
                val midNum = String.format("%02d", seatIdx)
                items.add(SeatItem.Empty)
                items.add(seatMap["$prefix$midNum"]?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
                items.add(SeatItem.Empty)
                seatIdx += 1
            }

            row++
        }

        return items
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position) is SeatItem.SeatCell) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_seat, parent, false)
            SeatViewHolder(view)
        } else {
            val view = View(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            EmptyViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SeatViewHolder) {
            holder.bind((getItem(position) as SeatItem.SeatCell).seat)
        }
    }

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val seatIcon: ImageView  = itemView.findViewById(R.id.seatIcon)
        private val seatNumber: TextView = itemView.findViewById(R.id.seatNumberText)

        fun bind(seat: Seat) {
            seatNumber.text = seat.seatNumber
            val isBooked   = seat.id in bookedSeatIds
            val isSelected = seat.id in selectedSeats

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

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    object SeatItemDiff : DiffUtil.ItemCallback<SeatItem>() {
        override fun areItemsTheSame(old: SeatItem, new: SeatItem): Boolean {
            if (old is SeatItem.SeatCell && new is SeatItem.SeatCell)
                return old.seat.id == new.seat.id
            return old == new
        }
        override fun areContentsTheSame(old: SeatItem, new: SeatItem) = old == new
    }
}

sealed class SeatItem {
    data class SeatCell(val seat: Seat) : SeatItem()
    object Empty : SeatItem()
}