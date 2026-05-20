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

/**
 * Layout xe giường nằm — GridLayoutManager spanCount = 3
 *
 * Hàng lẻ  : [ghế] [EMPTY] [ghế]   ← A01, (trống), A02
 * Hàng chẵn: [ghế] [ghế]   [ghế]   ← A03, A04,     A05
 * Hàng lẻ  : [ghế] [EMPTY] [ghế]   ← A06, (trống), A07
 * Hàng chẵn: [ghế] [ghế]   [ghế]   ← A08, A09,     A10
 * ...
 *
 * Mỗi chu kỳ 5 ghế → 2 hàng × 3 ô = 6 ô (trong đó 1 ô là lối đi)
 */
class SeatAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<SeatItem, RecyclerView.ViewHolder>(SeatItemDiff) {

    private var selectedSeatIds: Set<Long> = emptySet()

    // ── Public API ────────────────────────────────────────────────────────────

    fun updateSelectedSeats(seats: List<Seat>) {
        selectedSeatIds = seats.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    fun submitSeats(seats: List<Seat>) {
        submitList(buildItems(seats))
    }

    // ── Build items theo pattern xe giường ───────────────────────────────────

    private fun buildItems(seats: List<Seat>): List<SeatItem> {
        val result = mutableListOf<SeatItem>()
        var i = 0
        while (i < seats.size) {
            // ── Hàng lẻ: [ghế] [EMPTY] [ghế] ──
            result.add(seats.getOrNull(i)?.let     { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
            result.add(SeatItem.Empty)                                          // lối đi giữa
            result.add(seats.getOrNull(i + 1)?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
            i += 2

            // ── Hàng chẵn: [ghế] [ghế] [ghế] ──
            if (i < seats.size) {
                result.add(seats.getOrNull(i)?.let     { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
                result.add(seats.getOrNull(i + 1)?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
                result.add(seats.getOrNull(i + 2)?.let { SeatItem.SeatCell(it) } ?: SeatItem.Empty)
                i += 3
            }
        }
        return result
    }

    // ── SpanSizeLookup: tất cả ô đều chiếm 1 span ────────────────────────────

    fun makeSpanSizeLookup(): GridLayoutManager.SpanSizeLookup =
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = 1
        }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int) =
        if (getItem(position) is SeatItem.SeatCell) VIEW_SEAT else VIEW_EMPTY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SEAT) {
            SeatViewHolder(inflater.inflate(R.layout.item_seat, parent, false))
        } else {
            // Dùng cùng layout nhưng INVISIBLE để giữ đúng kích thước ô
            EmptyViewHolder(inflater.inflate(R.layout.item_seat, parent, false).also {
                it.visibility = View.INVISIBLE
            })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is SeatViewHolder && item is SeatItem.SeatCell) {
            holder.bind(item.seat)
        }
    }

    inner class SeatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val seatIcon: ImageView  = itemView.findViewById(R.id.seatIcon)
        private val seatNumber: TextView = itemView.findViewById(R.id.seatNumberText)

        fun bind(seat: Seat) {
            seatNumber.text = seat.seatNumber
            val isBooked   = seat.isBooked
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

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private const val VIEW_SEAT  = 0
        private const val VIEW_EMPTY = 1

        private val SeatItemDiff = object : DiffUtil.ItemCallback<SeatItem>() {
            override fun areItemsTheSame(old: SeatItem, new: SeatItem): Boolean {
                if (old is SeatItem.SeatCell && new is SeatItem.SeatCell)
                    return old.seat.id == new.seat.id
                return old === new
            }
            override fun areContentsTheSame(old: SeatItem, new: SeatItem) = old == new
        }
    }
}

sealed class SeatItem {
    data class SeatCell(val seat: Seat) : SeatItem()
    object Empty : SeatItem()
}