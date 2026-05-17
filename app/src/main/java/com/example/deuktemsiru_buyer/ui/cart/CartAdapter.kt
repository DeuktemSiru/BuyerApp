package com.example.deuktemsiru_buyer.ui.cart

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.data.CartItem
import com.example.deuktemsiru_buyer.databinding.ItemCartBinding
import com.example.deuktemsiru_buyer.util.formatPrice

class CartAdapter(
    private var items: List<CartItem>,
    val selectedIds: MutableSet<Long> = items.map { it.menuId }.toMutableSet(),
    private val onDelete: (CartItem) -> Unit,
    private val onIncrease: (CartItem) -> Unit,
    private val onDecrease: (CartItem) -> Unit,
    private val onSelectionChanged: () -> Unit,
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvEmoji.text = item.emoji
            binding.tvMenuName.text = item.menuName
            binding.tvDiscountedPrice.text = (item.discountedPrice * item.quantity).formatPrice()
            if (item.originalPrice > 0 && item.originalPrice != item.discountedPrice) {
                binding.tvOriginalPrice.visibility = View.VISIBLE
                binding.tvOriginalPrice.text = (item.originalPrice * item.quantity).formatPrice()
                binding.tvOriginalPrice.paintFlags =
                    binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvOriginalPrice.visibility = View.GONE
            }
            binding.tvQuantity.text = item.quantity.toString()

            binding.cbSelect.isChecked = item.menuId in selectedIds
            binding.cbSelect.setOnCheckedChangeListener { _, checked ->
                if (checked) selectedIds.add(item.menuId) else selectedIds.remove(item.menuId)
                onSelectionChanged()
            }

            binding.btnDelete.setOnClickListener { onDelete(item) }
            binding.btnPlus.setOnClickListener { onIncrease(item) }
            binding.btnMinus.setOnClickListener { onDecrease(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    fun update(newItems: List<CartItem>) {
        val newIds = newItems.map { it.menuId }.toSet()
        selectedIds.retainAll(newIds)
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(o: Int, n: Int) = items[o].menuId == newItems[n].menuId
            override fun areContentsTheSame(o: Int, n: Int) = items[o] == newItems[n]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    fun selectAll() {
        selectedIds.addAll(items.map { it.menuId })
        notifyItemRangeChanged(0, items.size)
    }

    fun deselectAll() {
        selectedIds.clear()
        notifyItemRangeChanged(0, items.size)
    }

    val allSelected: Boolean get() = items.isNotEmpty() && selectedIds.size == items.size

}
