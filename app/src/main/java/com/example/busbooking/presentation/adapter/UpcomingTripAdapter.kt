package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.relations.TicketDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpcomingTripAdapter(
    private val onItemClick: (ticketId: Long) -> Unit
) : ListAdapter<TicketDetails, UpcomingTripAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val routeText: TextView = view.findViewById(R.id.upcomingRouteText)
        val dateText: TextView = view.findViewById(R.id.upcomingDateText)
        val seatText: TextView = view.findViewById(R.id.upcomingSeatText)
        val statusText: TextView = view.findViewById(R.id.upcomingStatusText)
        val priceText: TextView = view.findViewById(R.id.upcomingPriceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_upcoming_trip, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val details = getItem(position)
        val route = details.tripWithRouteAndBus.route
        val trip = details.tripWithRouteAndBus.trip
        val seat = details.seat
        val ticket = details.ticket

        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        holder.routeText.text = "${route.origin} \u2192 ${route.destination}"
        holder.dateText.text = dateFmt.format(Date(trip.tripDate))
        holder.seatText.text = "Gh\u1ebf ${seat.seatNumber}"
        holder.statusText.text = ticket.status
        holder.priceText.text = String.format("%,.0f \u0111", trip.price)

        holder.itemView.setOnClickListener { onItemClick(ticket.id) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<TicketDetails>() {
            override fun areItemsTheSame(a: TicketDetails, b: TicketDetails): Boolean {
                return a.ticket.id == b.ticket.id
            }

            override fun areContentsTheSame(a: TicketDetails, b: TicketDetails): Boolean {
                return a == b
            }
        }
    }
}
