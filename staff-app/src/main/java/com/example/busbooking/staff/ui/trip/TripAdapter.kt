package com.example.busbooking.staff.ui.trip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.model.StaffTripSummary
import com.example.busbooking.staff.utils.StaffFormatters

class TripAdapter(
    private val onClick: (StaffTripSummary) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {
    private val items = mutableListOf<StaffTripSummary>()

    fun submitList(newItems: List<StaffTripSummary>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_staff_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripCodeText = itemView.findViewById<TextView>(R.id.tripCodeText)
        private val statusText = itemView.findViewById<TextView>(R.id.statusText)
        private val routeText = itemView.findViewById<TextView>(R.id.routeText)
        private val timeBusText = itemView.findViewById<TextView>(R.id.timeBusText)
        private val capacityText = itemView.findViewById<TextView>(R.id.capacityText)

        fun bind(item: StaffTripSummary) {
            tripCodeText.text = item.code
            statusText.text = StaffFormatters.tripStatus(item.status, item.departureTime)
            routeText.text = "${item.origin} → ${item.destination}"
            timeBusText.text = "${StaffFormatters.dateTime(item.departureTime)} · ${item.licensePlate}"
            capacityText.text = "${item.bookedSeats} vé đã đặt · ${item.totalSeats} ghế"
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
