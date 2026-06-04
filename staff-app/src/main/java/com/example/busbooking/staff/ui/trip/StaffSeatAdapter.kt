package com.example.busbooking.staff.ui.trip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.model.StaffSeat
import java.util.Locale

class StaffSeatAdapter(
    private val onSeatClick: (StaffSeat) -> Unit
) : RecyclerView.Adapter<StaffSeatAdapter.SeatViewHolder>() {
    private val items = mutableListOf<StaffSeatItem>()

    fun submitList(newItems: List<StaffSeat>) {
        items.clear()
        items.addAll(
            newItems
                .groupBy { it.floor }
                .toSortedMap()
                .values
                .flatMap { buildFloorItems(it) }
        )
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is StaffSeatItem.SeatCell) VIEW_SEAT else VIEW_EMPTY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_staff_seat, parent, false)
        return if (viewType == VIEW_SEAT) {
            SeatViewHolder(view, onSeatClick)
        } else {
            SeatViewHolder(view.also { it.visibility = View.INVISIBLE }, onSeatClick)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val item = items[position]
        if (item is StaffSeatItem.SeatCell) {
            holder.bind(item.seat)
        }
    }

    private fun buildFloorItems(seats: List<StaffSeat>): List<StaffSeatItem> {
        val ordered = seats.sortedWith(
            compareBy<StaffSeat> { it.rowIndex }
                .thenBy { it.columnIndex }
                .thenBy { it.seatNumber.seatNumberIndex() ?: Int.MAX_VALUE }
                .thenBy { it.seatId }
        )
        if (ordered.isEmpty()) return emptyList()

        val seatsByPosition = ordered.associateBy { it.rowIndex to it.columnIndex }
        val maxRow = ordered.maxOf { it.rowIndex }
        val result = mutableListOf<StaffSeatItem>()
        for (row in 0..maxRow) {
            for (column in 0..2) {
                result.add(seatsByPosition[row to column]?.let { StaffSeatItem.SeatCell(it) } ?: StaffSeatItem.Empty)
            }
        }
        return result
    }

    class SeatViewHolder(
        itemView: View,
        private val onSeatClick: (StaffSeat) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val seatIcon = itemView.findViewById<ImageView>(R.id.seatIcon)
        private val seatNumberText = itemView.findViewById<TextView>(R.id.seatNumberText)

        fun bind(item: StaffSeat) {
            seatNumberText.text = item.seatNumber.padSeatNumber()
            when (item.status.uppercase(Locale.ROOT)) {
                "CHECKED_IN" -> {
                    seatIcon.setImageResource(R.drawable.ic_staff_seat_checked_in)
                    seatNumberText.setTextColor(itemView.context.getColor(R.color.staff_success))
                }
                "BOOKED" -> {
                    seatIcon.setImageResource(R.drawable.ic_staff_seat_booked)
                    seatNumberText.setTextColor(itemView.context.getColor(R.color.staff_text_primary))
                }
                else -> {
                    seatIcon.setImageResource(R.drawable.ic_staff_seat_available)
                    seatNumberText.setTextColor(itemView.context.getColor(R.color.staff_text_secondary))
                }
            }
            itemView.setOnClickListener { onSeatClick(item) }
        }
    }

    companion object {
        private const val VIEW_SEAT = 0
        private const val VIEW_EMPTY = 1
    }
}

private sealed class StaffSeatItem {
    data class SeatCell(val seat: StaffSeat) : StaffSeatItem()
    object Empty : StaffSeatItem()
}

private fun String.seatNumberIndex(): Int? {
    return dropWhile { it.isLetter() }.toIntOrNull()
}

private fun String.padSeatNumber(): String {
    val prefix = takeWhile { it.isLetter() }
    val number = dropWhile { it.isLetter() }.toIntOrNull()
    return if (prefix.isNotBlank() && number != null) {
        "$prefix${number.toString().padStart(2, '0')}"
    } else {
        this
    }
}
