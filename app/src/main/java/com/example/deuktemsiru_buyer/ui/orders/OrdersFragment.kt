package com.example.deuktemsiru_buyer.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentOrdersBinding
import com.example.deuktemsiru_buyer.databinding.ItemOrderHistoryBinding
import com.example.deuktemsiru_buyer.network.OrderListItemResponse
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.orderStatusLabel
import com.example.deuktemsiru_buyer.util.toast
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = SessionManager(requireContext())
        if (!session.isLoggedIn()) return

        loadOrders()
    }

    private fun loadOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val orders = RetrofitClient.api.getOrders().data.orEmpty()
                if (orders.isEmpty()) {
                    binding.rvOrders.visibility = View.GONE
                    binding.llEmpty.visibility = View.VISIBLE
                } else {
                    binding.llEmpty.visibility = View.GONE
                    binding.rvOrders.visibility = View.VISIBLE
                    binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvOrders.adapter = OrderHistoryAdapter(orders) { order ->
                        showOrderDetail(order)
                    }
                }
            } catch (e: Exception) {
                toast("주문 내역을 불러오지 못했어요.")
            }
        }
    }

    private fun showOrderDetail(order: OrderListItemResponse) {
        OrderDetailBottomSheet.newInstance(order.orderId) { loadOrders() }
            .show(childFragmentManager, "order_detail")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class OrderHistoryAdapter(
    private val orders: List<OrderListItemResponse>,
    private val onItemClick: (OrderListItemResponse) -> Unit,
) : RecyclerView.Adapter<OrderHistoryAdapter.VH>() {

    inner class VH(val binding: ItemOrderHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemOrderHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = orders[position]
        val b = holder.binding
        b.tvStoreName.text = order.storeName
        b.tvOrderNumber.text = "#${order.orderId}"
        b.tvStatus.text = orderStatusLabel(order.status)
        val (statusBackground, statusTextColor) = when (order.status) {
            "CONFIRMED", "PREPARING", "READY" -> R.drawable.bg_order_status_primary to R.color.primary
            "PICKED_UP", "COMPLETED" -> R.drawable.bg_order_status_success to R.color.success
            "CANCELLED" -> R.drawable.bg_order_status_muted to R.color.text_muted
            else -> R.drawable.bg_order_status_muted to R.color.text_sub
        }
        b.tvStatus.setBackgroundResource(statusBackground)
        b.tvStatus.setTextColor(ContextCompat.getColor(b.root.context, statusTextColor))
        b.tvMenuSummary.text = "주문 상품 ${order.itemCount}개"
        b.tvTotalAmount.text = order.totalPrice.formatPrice()
        b.tvPickupTime.text = "주문: ${order.createdAt.substringBefore('T')}"
        b.tvPickupCode.text = "코드: ${order.pickupCode ?: "----"}"
        b.root.setOnClickListener { onItemClick(order) }
    }
}
