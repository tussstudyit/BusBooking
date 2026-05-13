package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.viewmodel.PopularRoute

class PopularRouteAdapter(
    private val onItemClick: (route: PopularRoute) -> Unit
) : ListAdapter<PopularRoute, PopularRouteAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val originText: TextView      = view.findViewById(R.id.routeOriginText)
        val destinationText: TextView = view.findViewById(R.id.routeDestinationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_popular_route, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.originText.text      = item.origin
        holder.destinationText.text = item.destination
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PopularRoute>() {
            override fun areItemsTheSame(a: PopularRoute, b: PopularRoute) =
                a.origin == b.origin && a.destination == b.destination
            override fun areContentsTheSame(a: PopularRoute, b: PopularRoute) = a == b
        }
    }
}