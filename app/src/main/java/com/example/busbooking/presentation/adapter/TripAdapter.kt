package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.relations.TripWithRouteAndBus
import java.text.SimpleDateFormat
import java.util.*

class TripAdapter(
    private val onItemClick: (TripWithRouteAndBus) -> Unit
) : ListAdapter<TripWithRouteAndBus, TripAdapter.TripViewHolder>(DiffCallback) {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val routeText: TextView      = itemView.findViewById(R.id.itemRouteText)
        val dateText: TextView       = itemView.findViewById(R.id.itemDateText)
        val departureText: TextView  = itemView.findViewById(R.id.itemDepartureText)
        val arrivalText: TextView    = itemView.findViewById(R.id.itemArrivalText)
        val priceText: TextView      = itemView.findViewById(R.id.itemPriceText)
        val busText: TextView        = itemView.findViewById(R.id.itemBusText)
        val statusText: TextView     = itemView.findViewById(R.id.itemStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val item = getItem(position)
        with(holder) {
            routeText.text     = "${item.route.origin} → ${item.route.destination}"
            dateText.text      = dateFmt.format(Date(item.trip.tripDate))
            departureText.text = "Khởi hành: ${timeFmt.format(Date(item.trip.departureTime))}"
            arrivalText.text   = "Đến nơi: ${timeFmt.format(Date(item.trip.arrivalTime))}"
            priceText.text     = "Giá: ${String.format("%,.0f", item.trip.price)} VNĐ"
            busText.text       = "${item.bus.busName} (${item.bus.licensePlate})"
            statusText.text    = item.trip.status

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TripWithRouteAndBus>() {
            override fun areItemsTheSame(old: TripWithRouteAndBus, new: TripWithRouteAndBus) =
                old.trip.id == new.trip.id

            override fun areContentsTheSame(old: TripWithRouteAndBus, new: TripWithRouteAndBus) =
                old == new
        }
    }
}