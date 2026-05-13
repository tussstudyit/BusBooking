package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
        val busText: TextView         = itemView.findViewById(R.id.itemBusText)
        val departureText: TextView   = itemView.findViewById(R.id.itemDepartureText)
        val dateText: TextView        = itemView.findViewById(R.id.itemDateText)
        val busTypeText: TextView     = itemView.findViewById(R.id.itemBusTypeText)
        val seatsText: TextView       = itemView.findViewById(R.id.itemSeatsText)
        val seatsProgress: ProgressBar = itemView.findViewById(R.id.itemSeatsProgress)
        val priceText: TextView       = itemView.findViewById(R.id.itemPriceText)
        val statusText: TextView      = itemView.findViewById(R.id.itemStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val item = getItem(position)
        val totalSeats = item.bus.totalSeats

        // Giả lập số ghế còn (thay bằng query thực nếu có)
        val availableSeats = (totalSeats * 0.8).toInt()
        val occupiedPercent = ((totalSeats - availableSeats) * 100 / totalSeats)

        with(holder) {
            busText.text       = item.bus.busName
            departureText.text = timeFmt.format(Date(item.trip.departureTime))
            dateText.text      = dateFmt.format(Date(item.trip.tripDate))
            busTypeText.text   = "XE ${totalSeats} GIƯỜNG"
            seatsText.text     = "Còn $availableSeats/$totalSeats giường"
            seatsProgress.progress = occupiedPercent

            // Hiển thị giá dạng "190" với ".000 VNĐ" bên dưới
            val priceInThousands = (item.trip.price / 1000).toInt()
            priceText.text  = "$priceInThousands"
            statusText.text = ".000 VNĐ"

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