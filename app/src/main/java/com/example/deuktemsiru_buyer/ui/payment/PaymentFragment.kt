package com.example.deuktemsiru_buyer.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentPaymentBinding
import com.example.deuktemsiru_buyer.network.CreateOrderRequest
import com.example.deuktemsiru_buyer.network.OrderItemRequest
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlinx.coroutines.launch

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private val pickupTime = "18:00"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val storeId = arguments?.getInt("storeId") ?: 1
        val menuId = arguments?.getInt("menuId") ?: 0
        val totalPrice = arguments?.getInt("totalPrice") ?: 5900
        val fromCart = arguments?.getBoolean("fromCart") ?: false

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvSiruBalance.text = "%,d원".format(session.siruBalance)

        if (fromCart) {
            loadFromCart(session, storeId)
        } else {
            loadFromStore(session, storeId, menuId, totalPrice)
        }
    }

    private fun loadFromCart(session: SessionManager, storeId: Int) {
        val originalTotal = CartManager.items.sumOf { it.originalPrice * it.quantity }
        val discountedTotal = CartManager.totalPrice
        val discountAmount = (originalTotal - discountedTotal).coerceAtLeast(0)

        binding.tvStoreName.text = CartManager.storeName
        binding.tvMenuName.text = "장바구니 메뉴 ${CartManager.totalCount}개"
        binding.tvPickupTimeDisplay.text = formatPickupTime(pickupTime)
        binding.tvItemPrice.text = formatPrice(discountedTotal)
        binding.tvOrderPrice.text = formatPrice(originalTotal)
        binding.tvDiscount.text = "-${formatPrice(discountAmount)}"
        binding.tvFinalPrice.text = formatPrice(discountedTotal)
        binding.tvSavingsMessage.text = "${formatPrice(discountAmount)}을 절약하고 음식 ${CartManager.totalCount}개를 구해요"
        binding.btnPay.text = getString(R.string.btn_pay_siru, formatPrice(discountedTotal))

        binding.btnPay.setOnClickListener {
            if (!session.isSiruLinked) {
                Toast.makeText(requireContext(), "시루 계정 연동 후 결제할 수 있어요.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.siruLinkFragment)
                return@setOnClickListener
            }
            val orderItems = CartManager.items.map {
                OrderItemRequest(productId = it.menuId, quantity = it.quantity)
            }

            binding.btnPay.isEnabled = false
            binding.btnPay.text = getString(R.string.payment_processing_siru)

            lifecycleScope.launch {
                try {
                    val order = RetrofitClient.api.createOrder(
                        CreateOrderRequest(items = orderItems)
                    ).data ?: return@launch
                    session.lastOrderId = order.orderId
                    runCatching { RetrofitClient.api.clearCart() }
                    runCatching { RetrofitClient.api.getMe().data }.getOrNull()?.let {
                        session.isSiruLinked = it.isSiruLinked
                        session.siruBalance = it.siruBalance
                    }
                    CartManager.clear()
                    findNavController().navigate(
                        R.id.action_payment_to_pickup,
                        Bundle().apply { putInt("storeId", storeId) }
                    )
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "결제 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                    binding.btnPay.isEnabled = true
                    binding.btnPay.text = getString(R.string.btn_pay_siru, formatPrice(discountedTotal))
                }
            }
        }
    }

    private fun loadFromStore(session: SessionManager, storeId: Int, menuId: Int, totalPrice: Int) {
        lifecycleScope.launch {
            try {
                val storeResponse = RetrofitClient.api.getStore(storeId.toLong()).data ?: run {
                    findNavController().popBackStack()
                    return@launch
                }
                val store = storeResponse.toStore()
                val selectedMenu = storeResponse.products
                    .firstOrNull { it.productId.toInt() == menuId && it.status != "SOLD_OUT" && it.quantityRemaining > 0 }
                    ?: storeResponse.products.firstOrNull { it.status != "SOLD_OUT" && it.quantityRemaining > 0 }

                binding.tvStoreName.text = store.name
                binding.tvMenuName.text = selectedMenu?.name ?: "주문 가능한 메뉴 없음"
                binding.tvPickupTimeDisplay.text = formatPickupTime(pickupTime)

                val discountedTotal = selectedMenu?.discountPrice ?: totalPrice
                val originalTotal = selectedMenu?.let { if (it.originalPrice > 0) it.originalPrice else it.discountPrice } ?: totalPrice
                val discountAmount = (originalTotal - discountedTotal).coerceAtLeast(0)

                binding.tvItemPrice.text = "%,d원".format(discountedTotal)
                binding.tvOrderPrice.text = "%,d원".format(originalTotal)
                binding.tvDiscount.text = "-%,d원".format(discountAmount)
                binding.tvFinalPrice.text = "%,d원".format(discountedTotal)
                binding.tvSavingsMessage.text = "%,d원을 절약하고 음식 1개를 구해요".format(discountAmount)
                binding.btnPay.text = getString(R.string.btn_pay_siru, "%,d원".format(discountedTotal))

                binding.btnPay.setOnClickListener {
                    if (!session.isSiruLinked) {
                        Toast.makeText(requireContext(), "시루 계정 연동 후 결제할 수 있어요.", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.siruLinkFragment)
                        return@setOnClickListener
                    }
                    val orderItems = selectedMenu?.let {
                        listOf(OrderItemRequest(productId = it.productId, quantity = 1))
                    }.orEmpty()

                    if (orderItems.isEmpty()) {
                        Toast.makeText(requireContext(), "주문 가능한 메뉴가 없어요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    binding.btnPay.isEnabled = false
                    binding.btnPay.text = getString(R.string.payment_processing_siru)

                    lifecycleScope.launch {
                        try {
                            val order = RetrofitClient.api.createOrder(
                                CreateOrderRequest(items = orderItems)
                            ).data ?: return@launch

                            session.lastOrderId = order.orderId
                            runCatching { RetrofitClient.api.getMe().data }.getOrNull()?.let {
                                session.isSiruLinked = it.isSiruLinked
                                session.siruBalance = it.siruBalance
                            }
                            findNavController().navigate(
                                R.id.action_payment_to_pickup,
                                Bundle().apply { putInt("storeId", storeId) }
                            )
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "결제 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                            binding.btnPay.isEnabled = true
                            binding.btnPay.text = getString(R.string.btn_pay_siru, "%,d원".format(discountedTotal))
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "가게 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun formatPrice(price: Int): String = "%,d원".format(price)
    private fun formatPickupTime(time: String): String = "오늘 오후 ${time.toDisplayHour()} 픽업"

    private fun String.toDisplayHour(): String {
        val hour = substringBefore(":").toIntOrNull() ?: return this
        val minute = substringAfter(":", "00")
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:${minute.padStart(2, '0')}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
