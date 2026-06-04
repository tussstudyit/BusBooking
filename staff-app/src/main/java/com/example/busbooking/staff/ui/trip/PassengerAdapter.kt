package com.example.busbooking.staff.ui.trip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.staff.R
import com.example.busbooking.staff.data.model.StaffPassenger
import com.example.busbooking.staff.utils.StaffFormatters

class PassengerAdapter : RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder>() {
    private val items = mutableListOf<StaffPassenger>()

    fun submitList(newItems: List<StaffPassenger>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PassengerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_passenger, parent, false)
        return PassengerViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PassengerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class PassengerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText = itemView.findViewById<TextView>(R.id.nameText)
        private val seatText = itemView.findViewById<TextView>(R.id.seatText)
        private val statusText = itemView.findViewById<TextView>(R.id.statusText)

        fun bind(item: StaffPassenger) {
            nameText.text = item.name
            seatText.text = "Ghế ${item.seatNumber} · ${item.phone}"
            statusText.text = "${StaffFormatters.paymentStatus(item.paymentStatus)} · ${StaffFormatters.checkInStatus(item.checkInStatus)}"
        }
    }
}
