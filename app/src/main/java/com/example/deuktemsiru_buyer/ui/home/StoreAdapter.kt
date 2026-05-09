package com.example.deuktemsiru_buyer.ui.home

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.databinding.ItemStoreCardBinding

class StoreAdapter(
    private var stores: List<Store>,
    private val onStoreClick: (Store) -> Unit,
    private val onWishlistClick: (Store) -> Unit
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    inner class StoreViewHolder(private val binding: ItemStoreCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(store: Store) {
            binding.tvThumbnailEmoji.text = store.emoji
            binding.tvStoreName.text = store.name
            binding.tvCategory.text = store.category
            binding.tvDistance.text = "도보 ${store.walkingMinutes}분"
            binding.tvDiscountBadge.text = "${store.discountRate}%"
            binding.tvOriginalPrice.text = "%,d원".format(store.originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvDiscountPrice.text = "%,d원".format(store.discountedPrice)
            binding.tvStock.text = "${store.remainingItems}개 남음"

            val timeText = "${store.minutesUntilClose}분 후 마감"
            binding.tvTimeLeft.text = timeText

            if (store.minutesUntilClose <= 30) {
                binding.tvTimeLeft.setTextColor(Color.parseColor("#FF3B30"))
                binding.ivClock.setImageResource(R.drawable.ic_clock)
            } else {
                binding.tvTimeLeft.setTextColor(Color.parseColor("#FF8800"))
                binding.ivClock.setImageResource(R.drawable.ic_clock_warning)
            }

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
        holder.bind(stores[position])
    }

    override fun getItemCount() = stores.size

    fun updateStores(newStores: List<Store>) {
        stores = newStores
        notifyDataSetChanged()
    }
}
