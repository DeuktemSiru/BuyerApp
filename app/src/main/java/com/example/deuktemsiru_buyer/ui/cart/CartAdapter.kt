package com.example.deuktemsiru_buyer.ui.cart

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.data.CartItem
import com.example.deuktemsiru_buyer.databinding.ItemCartBinding

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
            binding.tvDiscountedPrice.text = formatPrice(item.discountedPrice * item.quantity)
            binding.tvOriginalPrice.text = formatPrice(item.originalPrice * item.quantity)
            binding.tvOriginalPrice.paintFlags =
                binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
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
        // keep selection in sync — remove ids that no longer exist
        val newIds = newItems.map { it.menuId }.toSet()
        selectedIds.retainAll(newIds)
        items = newItems
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedIds.addAll(items.map { it.menuId })
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedIds.clear()
        notifyDataSetChanged()
    }

    val allSelected: Boolean get() = items.isNotEmpty() && selectedIds.size == items.size

    private fun formatPrice(price: Int): String = "%,d원".format(price)
}
