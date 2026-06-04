package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R

data class RouteCatalogItem(
    val origin: String,
    val destination: String,
    val distanceText: String,
    val durationText: String,
    val priceText: String,
    @param:DrawableRes val imageResId: Int
)

class RouteCatalogAdapter(
    private val onItemClick: (RouteCatalogItem) -> Unit
) : RecyclerView.Adapter<RouteCatalogAdapter.ViewHolder>() {

    private val items = mutableListOf<RouteCatalogItem>()

    fun submitList(newItems: List<RouteCatalogItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_catalog, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.routeImage)
        private val titleText: TextView = itemView.findViewById(R.id.routeTitleText)
        private val metaText: TextView = itemView.findViewById(R.id.routeMetaText)
        private val priceText: TextView = itemView.findViewById(R.id.routePriceText)

        fun bind(item: RouteCatalogItem) {
            imageView.setImageResource(item.imageResId)
            titleText.text = "${item.origin} ⇄ ${item.destination}"
            metaText.text = "${item.distanceText} • ${item.durationText}"
            priceText.text = item.priceText
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
