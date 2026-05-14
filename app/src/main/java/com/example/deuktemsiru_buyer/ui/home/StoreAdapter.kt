package com.example.deuktemsiru_buyer.ui.home

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.databinding.ItemStoreCardBinding

class StoreAdapter(
    private val onStoreClick: (Store) -> Unit,
    private val onWishlistClick: (Store) -> Unit,
) : ListAdapter<Store, StoreAdapter.StoreViewHolder>(StoreDiffCallback) {

    inner class StoreViewHolder(private val binding: ItemStoreCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(store: Store) {
            val ctx = binding.root.context

            binding.tvThumbnailEmoji.text = store.emoji
            binding.tvStoreName.text = store.name
            binding.tvCategory.text = store.category
            binding.tvDistance.text = "도보 ${store.walkingMinutes}분"
            binding.tvDiscountBadge.text = "${store.discountRate}%"
            binding.tvOriginalPrice.text = "%,d원".format(store.originalPrice)
            binding.tvOriginalPrice.paintFlags =
                binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvDiscountPrice.text = "%,d원".format(store.discountedPrice)
            binding.tvStock.text = "${store.remainingItems}개 남음"

            val mins = store.minutesUntilClose
            binding.tvTimeLeft.text = "${mins}분 후 마감"
            val timeColor = if (mins <= 30) R.color.danger else R.color.warning
            binding.tvTimeLeft.setTextColor(ContextCompat.getColor(ctx, timeColor))
            val clockRes = if (mins <= 30) R.drawable.ic_clock else R.drawable.ic_clock_warning
            binding.ivClock.setImageResource(clockRes)

            binding.btnWishlist.setImageResource(
                if (store.isWishlisted) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )

            binding.cardRoot.setOnClickListener { onStoreClick(store) }
            binding.btnWishlist.setOnClickListener { onWishlistClick(store) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val binding = ItemStoreCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object StoreDiffCallback : DiffUtil.ItemCallback<Store>() {
        override fun areItemsTheSame(oldItem: Store, newItem: Store) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Store, newItem: Store) = oldItem == newItem
    }
}
