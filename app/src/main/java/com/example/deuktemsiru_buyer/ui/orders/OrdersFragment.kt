package com.example.deuktemsiru_buyer.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.deuktemsiru_buyer.data.SampleData
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentOrdersBinding
import com.example.deuktemsiru_buyer.databinding.ItemOrderHistoryBinding
import com.example.deuktemsiru_buyer.network.OrderApiResponse
import com.example.deuktemsiru_buyer.network.RetrofitClient
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

        lifecycleScope.launch {
            try {
                val orders = RetrofitClient.api.getOrders(session.userId)
                if (orders.isEmpty()) {
                    binding.rvOrders.visibility = View.GONE
                    binding.llEmpty.visibility = View.VISIBLE
                } else {
                    binding.llEmpty.visibility = View.GONE
                    binding.rvOrders.visibility = View.VISIBLE
                    binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvOrders.adapter = OrderHistoryAdapter(orders)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "주문 내역을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class OrderHistoryAdapter(
    private val orders: List<OrderApiResponse>
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
        b.tvOrderNumber.text = order.orderNumber
        b.tvStatus.text = statusLabel(order.status)
        b.tvMenuSummary.text = order.items.joinToString(", ") { "${it.emoji} ${it.name}" }
        b.tvTotalAmount.text = SampleData.formatPrice(order.totalAmount)
        b.tvPickupTime.text = "픽업: ${order.pickupTime}"
        b.tvPickupCode.text = "코드: ${order.pickupCode}"
    }

    private fun statusLabel(status: String) = when (status) {
        "NEW" -> "접수 대기"
        "PREPARING" -> "준비중"
        "READY" -> "픽업 대기"
        "COMPLETED" -> "완료"
        "REJECTED" -> "거절됨"
        else -> status
    }
}
