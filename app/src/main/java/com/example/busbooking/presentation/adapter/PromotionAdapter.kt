package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.presentation.viewmodel.Promotion

class PromotionAdapter : ListAdapter<Promotion, PromotionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val badgeText:   TextView = view.findViewById(R.id.promoBadgeText)
        val titleText:   TextView = view.findViewById(R.id.promoTitleText)
        val descText:    TextView = view.findViewById(R.id.promoDescText)
        val expiryText:  TextView = view.findViewById(R.id.promoExpiryText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_promotion, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.badgeText.text  = item.badgeLabel
        holder.titleText.text  = item.title
        holder.descText.text   = item.description
        holder.expiryText.text = item.expiresLabel
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Promotion>() {
            override fun areItemsTheSame(a: Promotion, b: Promotion) = a.id == b.id
            override fun areContentsTheSame(a: Promotion, b: Promotion) = a == b
        }
    }
}