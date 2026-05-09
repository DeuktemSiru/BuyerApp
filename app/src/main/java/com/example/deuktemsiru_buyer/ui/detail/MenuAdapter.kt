package com.example.deuktemsiru_buyer.ui.detail

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.data.MenuItem
import com.example.deuktemsiru_buyer.databinding.ItemMenuBinding

class MenuAdapter(
    private val menus: List<MenuItem>,
    private val onMenuClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    inner class MenuViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menu: MenuItem) {
            binding.tvEmoji.text = menu.emoji
            binding.tvMenuName.text = menu.name
            binding.tvOriginalPrice.text = "%,d원".format(menu.originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvDiscountRate.text = "${menu.discountRate}%"
            binding.tvDiscountPrice.text = "%,d원".format(menu.discountedPrice)
            binding.tvStock.text = "${menu.remainingItems}개 남음"

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
        holder.bind(menus[position])
    }

    override fun getItemCount() = menus.size
}
