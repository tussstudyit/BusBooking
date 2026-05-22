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
import com.example.busbooking.presentation.viewmodel.TripListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val onItemClick: (TripWithRouteAndBus) -> Unit
) : ListAdapter<TripListItem, TripAdapter.TripViewHolder>(DiffCallback) {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val busText: TextView = itemView.findViewById(R.id.itemBusText)
        val departureText: TextView = itemView.findViewById(R.id.itemDepartureText)
        val dateText: TextView = itemView.findViewById(R.id.itemDateText)
        val busTypeText: TextView = itemView.findViewById(R.id.itemBusTypeText)
        val seatsText: TextView = itemView.findViewById(R.id.itemSeatsText)
        val seatsProgress: ProgressBar = itemView.findViewById(R.id.itemSeatsProgress)
        val priceText: TextView = itemView.findViewById(R.id.itemPriceText)
        val statusText: TextView = itemView.findViewById(R.id.itemStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val item = getItem(position)
        val details = item.tripDetails
        val totalSeats = item.seatAvailability.totalSeats
        val availableSeats = item.seatAvailability.availableSeats

        with(holder) {
            busText.text = details.bus.busName
            departureText.text = timeFmt.format(Date(details.trip.departureTime))
            dateText.text = dateFmt.format(Date(details.trip.tripDate))
            busTypeText.text = "XE ${totalSeats} GIƯỜNG"
            seatsText.text = seatStatusText(details.trip.status, availableSeats, totalSeats)
            seatsProgress.progress = item.seatAvailability.occupiedPercent

            val priceInThousands = (details.trip.price / 1000).toInt()
            priceText.text = "$priceInThousands"
            statusText.text = ".000 VNĐ"

            itemView.alpha = if (item.isBookingOpen) 1f else 0.62f
            itemView.setOnClickListener { onItemClick(details) }
        }
    }

    private fun seatStatusText(tripStatus: String, availableSeats: Int, totalSeats: Int): String {
        return when {
            tripStatus != "SCHEDULED" -> "Trạng thái: $tripStatus"
            totalSeats <= 0 -> "Chưa có sơ đồ ghế"
            availableSeats <= 0 -> "Hết chỗ 0/$totalSeats giường"
            else -> "Còn $availableSeats/$totalSeats giường"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TripListItem>() {
            override fun areItemsTheSame(old: TripListItem, new: TripListItem): Boolean =
                old.tripDetails.trip.id == new.tripDetails.trip.id

            override fun areContentsTheSame(old: TripListItem, new: TripListItem): Boolean =
                old == new
        }
    }
}
