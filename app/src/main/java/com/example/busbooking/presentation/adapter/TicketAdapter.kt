package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.relations.TicketDetails
import java.text.SimpleDateFormat
import java.util.*

class TicketAdapter(
    private val onItemClick: (TicketDetails) -> Unit
) : ListAdapter<TicketDetails, TicketAdapter.TicketViewHolder>(DiffCallback) {

    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val routeText  : TextView = itemView.findViewById(R.id.routeText)
        val dateText   : TextView = itemView.findViewById(R.id.dateText)
        val seatText   : TextView = itemView.findViewById(R.id.seatText)
        val priceText  : TextView = itemView.findViewById(R.id.priceText)
        val statusText : TextView = itemView.findViewById(R.id.statusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val details = getItem(position)
        val ticket  = details.ticket
        val route   = details.tripWithRouteAndBus.route
        val trip    = details.tripWithRouteAndBus.trip
        val seat    = details.seat

        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        holder.routeText.text  = "${route.origin} → ${route.destination}"
        holder.dateText.text   = "Ngày: ${dateFmt.format(Date(trip.tripDate))}"
        holder.seatText.text   = "Ghế: ${seat.seatNumber}"
        holder.priceText.text  = "${String.format("%,.0f", trip.price)} VNĐ"
        holder.statusText.text = ticket.status

        // Đổi màu theo trạng thái
        val bgColor = when (ticket.status) {
            "CONFIRMED" -> ContextCompat.getColor(holder.itemView.context, R.color.blue_primary)
            "PENDING"   -> ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_dark)
            "CANCELLED" -> ContextCompat.getColor(holder.itemView.context, R.color.error)
            "COMPLETED" -> ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
            else        -> ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
        }
        holder.statusText.setBackgroundColor(bgColor)

        holder.itemView.setOnClickListener { onItemClick(details) }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TicketDetails>() {
            override fun areItemsTheSame(old: TicketDetails, new: TicketDetails) =
                old.ticket.id == new.ticket.id
            override fun areContentsTheSame(old: TicketDetails, new: TicketDetails) =
                old == new
        }
    }
}