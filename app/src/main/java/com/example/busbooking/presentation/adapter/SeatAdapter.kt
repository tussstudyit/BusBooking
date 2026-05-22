package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.domain.models.SeatDisplay
import com.example.busbooking.domain.models.SeatStatus

class SeatAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<SeatItem, RecyclerView.ViewHolder>(SeatItemDiff) {
    private var selectedSeatIds: Set<Long> = emptySet()

    fun updateSelectedSeats(seats: List<Seat>) {
        selectedSeatIds = seats.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    fun submitSeats(seats: List<SeatDisplay>) {
        submitList(buildItems(seats))
    }

    fun makeSpanSizeLookup(): GridLayoutManager.SpanSizeLookup =
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = 1
        }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is SeatItem.SeatCell) VIEW_SEAT else VIEW_EMPTY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_seat, parent, false)
        return if (viewType == VIEW_SEAT) {
            SeatViewHolder(view)
        } else {
            EmptyViewHolder(view.also { it.visibility = View.INVISIBLE })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is SeatViewHolder && item is SeatItem.SeatCell) {
            holder.bind(item.seatDisplay)
        }
    }

    private fun buildItems(seats: List<SeatDisplay>): List<SeatItem> {
        val result = mutableListOf<SeatItem>()
        val ordered = seats.sortedWith(
            compareBy<SeatDisplay> { it.seat.seatNumber.seatNumberIndex() ?: Int.MAX_VALUE }
                .thenBy { it.seat.rowIndex }
                .thenBy { it.seat.columnIndex }
                .thenBy { it.seat.id }
        )
        if (ordered.isEmpty()) {
            return result
        }

        result.add(SeatItem.SeatCell(ordered[0]))
        result.add(SeatItem.Empty)
        result.add(ordered.getOrNull(1)?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)

        var i = 2
        while (i < ordered.size) {
            result.add(ordered.getOrNull(i)?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
            result.add(ordered.getOrNull(i + 1)?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
            result.add(ordered.getOrNull(i + 2)?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
            i += 3
        }
        return result
    }

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val seatIcon: ImageView = itemView.findViewById(R.id.seatIcon)
        private val seatNumber: TextView = itemView.findViewById(R.id.seatNumberText)

        fun bind(seatDisplay: SeatDisplay) {
            val seat = seatDisplay.seat
            val isSelected = seat.id in selectedSeatIds

            seatNumber.text = seat.seatNumber.padSeatNumber()

            when {
                isSelected -> {
                    seatIcon.setImageResource(R.drawable.ic_seat_selected)
                    itemView.isEnabled = true
                    itemView.alpha = 1f
                    itemView.setOnClickListener { onSeatClick(seat) }
                }
                seatDisplay.status == SeatStatus.BOOKED -> {
                    seatIcon.setImageResource(R.drawable.ic_seat_booked)
                    itemView.isEnabled = false
                    itemView.alpha = 0.85f
                    itemView.setOnClickListener(null)
                }
                else -> {
                    seatIcon.setImageResource(R.drawable.ic_seat_available)
                    itemView.isEnabled = true
                    itemView.alpha = 1f
                    itemView.setOnClickListener { onSeatClick(seat) }
                }
            }
        }
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val VIEW_SEAT = 0
        private const val VIEW_EMPTY = 1

        private val SeatItemDiff = object : DiffUtil.ItemCallback<SeatItem>() {
            override fun areItemsTheSame(old: SeatItem, new: SeatItem): Boolean {
                if (old is SeatItem.SeatCell && new is SeatItem.SeatCell) {
                    return old.seatDisplay.seat.id == new.seatDisplay.seat.id
                }
                return old === new
            }

            override fun areContentsTheSame(old: SeatItem, new: SeatItem): Boolean = old == new
        }
    }
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

private fun String.seatNumberIndex(): Int? {
    return dropWhile { it.isLetter() }.toIntOrNull()
}

sealed class SeatItem {
    data class SeatCell(val seatDisplay: SeatDisplay) : SeatItem()
    object Empty : SeatItem()
}
