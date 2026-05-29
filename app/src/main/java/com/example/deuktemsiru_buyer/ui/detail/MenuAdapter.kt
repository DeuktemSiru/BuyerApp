package com.example.deuktemsiru_buyer.ui.detail

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.MenuItem
import com.example.deuktemsiru_buyer.databinding.ItemMenuBinding
import com.example.deuktemsiru_buyer.util.formatPrice

class MenuAdapter(
    menus: List<MenuItem>,
    selectedMenuId: Long,
    private val onMenuClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, MenuAdapter.MenuViewHolder>(MenuDiffCallback) {

    private var selectedMenuId: Long = selectedMenuId

    init {
        submitList(menus)
    }

    fun selectMenu(menuId: Long) {
        if (selectedMenuId == menuId) return
        val oldPosition = currentList.indexOfFirst { it.id == selectedMenuId }
        val newPosition = currentList.indexOfFirst { it.id == menuId }
        selectedMenuId = menuId
        if (oldPosition != -1) notifyItemChanged(oldPosition)
        if (newPosition != -1) notifyItemChanged(newPosition)
    }

    inner class MenuViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menu: MenuItem) {
            val isSelected = menu.id == selectedMenuId && !menu.isSoldOut
            binding.tvEmoji.text = menu.emoji
            binding.tvMenuName.text = menu.name
            binding.tvOriginalPrice.text = menu.originalPrice.formatPrice()
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvDiscountRate.text = "${menu.discountRate}%"
            binding.tvDiscountPrice.text = menu.discountedPrice.formatPrice()
            binding.tvStock.text = "${menu.remainingItems}개 남음"
            binding.tvSelectedBadge.isVisible = isSelected
            binding.itemRoot.setBackgroundResource(
                if (isSelected) R.drawable.bg_menu_item_selected else R.drawable.bg_menu_item_normal
            )
            binding.itemRoot.contentDescription = if (isSelected) {
                "선택된 메뉴, ${menu.name}, ${menu.discountedPrice.formatPrice()}"
            } else {
                "${menu.name}, ${menu.discountedPrice.formatPrice()}"
            }

            if (menu.isSoldOut) {
                binding.flSoldOut.visibility = View.VISIBLE
                binding.itemRoot.alpha = 0.5f
            } else {
                binding.flSoldOut.visibility = View.GONE
                binding.itemRoot.alpha = 1.0f
            }

            binding.itemRoot.setOnClickListener {
                if (!menu.isSoldOut) onMenuClick(menu)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object MenuDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem) = oldItem == newItem
    }
}
