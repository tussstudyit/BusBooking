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
import com.example.busbooking.data.relations.hasTripDeparted
import com.example.busbooking.data.relations.tripScheduleMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketAdapter(
    private val onItemClick: (TicketDetails) -> Unit
) : ListAdapter<TicketDetails, TicketAdapter.TicketViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val routeText: TextView = itemView.findViewById(R.id.routeText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val seatText: TextView = itemView.findViewById(R.id.seatText)
        private val priceText: TextView = itemView.findViewById(R.id.priceText)
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(details: TicketDetails) {
            val trip = details.tripWithRouteAndBus.trip
            val route = details.tripWithRouteAndBus.route
            val scheduleMillis = details.tripScheduleMillis()
            routeText.text = "${route.origin} \u2192 ${route.destination}"
            statusText.text = statusLabel(details.ticket.status, details.hasTripDeparted())
            dateText.text = if (scheduleMillis > 0L) dateFormat.format(Date(scheduleMillis)) else "--/--/---- --:--"
            seatText.text = "Gh\u1ebf ${details.seat.seatNumber}"
            priceText.text = "${String.format("%,.0f", trip.price)} VN\u0110"
            itemView.setOnClickListener { onItemClick(details) }
        }

        private fun statusLabel(status: String, hasTripDeparted: Boolean): String = when {
            hasTripDeparted && status == "CONFIRMED" -> "\u0110\u00e3 \u0111i"
            status == "CONFIRMED" -> "\u0110\u00e3 x\u00e1c nh\u1eadn"
            status == "PENDING" || status == "PENDING_PAYMENT" -> "Ch\u1edd thanh to\u00e1n"
            status == "PAYMENT_FAILED" -> "Thanh to\u00e1n th\u1ea5t b\u1ea1i"
            status == "USED" -> "\u0110\u00e3 \u0111i"
            status == "CANCELLED" -> "\u0110\u00e3 h\u1ee7y"
            else -> status
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TicketDetails>() {
            override fun areItemsTheSame(oldItem: TicketDetails, newItem: TicketDetails): Boolean {
                return oldItem.ticket.id == newItem.ticket.id
            }

            override fun areContentsTheSame(oldItem: TicketDetails, newItem: TicketDetails): Boolean {
                return oldItem == newItem
            }
        }
    }
}
