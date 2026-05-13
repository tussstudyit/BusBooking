package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.databinding.ItemBannerBinding
import com.example.busbooking.presentation.viewmodel.Banner

class BannerAdapter : ListAdapter<Banner, BannerAdapter.BannerViewHolder>(DIFF) {

    inner class BannerViewHolder(
        private val binding: ItemBannerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Banner) {
            binding.bannerImage.setImageResource(item.imageRes)
            binding.bannerTitle.text = item.title
            binding.bannerSubtitle.text = item.subtitle
            binding.bannerActionLabel.text = item.actionLabel
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder =
        BannerViewHolder(
            ItemBannerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Banner>() {
            override fun areItemsTheSame(a: Banner, b: Banner) = a.title == b.title
            override fun areContentsTheSame(a: Banner, b: Banner) = a == b
        }
    }
}