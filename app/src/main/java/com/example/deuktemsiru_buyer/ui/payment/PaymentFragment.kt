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
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.toDisplayHour
import kotlinx.coroutines.launch

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private var autoPayAfterLink = false

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
        val storeId = arguments?.getLong("storeId") ?: 0L
        if (storeId <= 0L) {
            Toast.makeText(requireContext(), "주문할 가게를 확인할 수 없어요.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        val menuId = arguments?.getLong("menuId") ?: 0L
        val totalPrice = arguments?.getInt("totalPrice") ?: 0
        val fromCart = arguments?.getBoolean("fromCart") ?: false
        autoPayAfterLink = arguments?.getBoolean("autoPayAfterLink") ?: false

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvSiruBalance.text = session.siruBalance.formatPrice()
        refreshSiruState(session)

        if (fromCart) {
            loadFromCart(session, storeId)
        } else {
            loadFromStore(session, storeId, menuId, totalPrice)
        }
    }

    private fun refreshSiruState(session: SessionManager) {
        viewLifecycleOwner.lifecycleScope.launch {
            syncSiruState(session)
        }
    }

    private suspend fun ensureSiruLinked(session: SessionManager): Boolean {
        syncSiruState(session)
        return session.isSiruLinked
    }

    private suspend fun syncSiruState(session: SessionManager) {
        runCatching { RetrofitClient.api.getMe().data }.getOrNull()?.let {
            session.isSiruLinked = it.isSiruLinked
            session.siruBalance = it.siruBalance
            if (_binding != null) binding.tvSiruBalance.text = it.siruBalance.formatPrice()
        }
    }

    private fun loadFromCart(session: SessionManager, storeId: Long) {
        val originalTotal = CartManager.items.sumOf { it.originalPrice * it.quantity }
        val discountedTotal = CartManager.totalPrice

        binding.tvStoreName.text = CartManager.storeName
        binding.tvMenuName.text = "장바구니 메뉴 ${CartManager.totalCount}개"
        binding.tvPickupTimeDisplay.text = formatPickupRange(
            CartManager.items.mapNotNull { it.pickupEnd.takeIf { end -> end.isNotBlank() } }.minOrNull()
        )
        setupPriceDisplay(originalTotal, discountedTotal, CartManager.totalCount)

        binding.btnPay.setOnClickListener {
            payFromCart(session, storeId, discountedTotal)
        }
        if (autoPayAfterLink) {
            autoPayAfterLink = false
            payFromCart(session, storeId, discountedTotal)
        }
    }

    private fun payFromCart(session: SessionManager, storeId: Long, discountedTotal: Int) {
        binding.btnPay.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            if (!ensureSiruLinked(session)) {
                binding.btnPay.isEnabled = true
                Toast.makeText(requireContext(), "시루 계정 연동 후 결제할 수 있어요.", Toast.LENGTH_SHORT).show()
                navigateToSiruLink(storeId, menuId = 0L, totalPrice = discountedTotal, fromCart = true)
                return@launch
            }
            val orderItems = CartManager.items.map {
                OrderItemRequest(productId = it.menuId, quantity = it.quantity)
            }

            submitOrder(session, storeId, orderItems, discountedTotal, clearCart = true)
        }
    }

    private fun payFromStore(
        session: SessionManager,
        storeId: Long,
        menuId: Long,
        discountedTotal: Int,
        selectedProductId: Long?,
    ) {
        binding.btnPay.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            if (!ensureSiruLinked(session)) {
                binding.btnPay.isEnabled = true
                Toast.makeText(requireContext(), "시루 계정 연동 후 결제할 수 있어요.", Toast.LENGTH_SHORT).show()
                navigateToSiruLink(storeId, menuId, discountedTotal, fromCart = false)
                return@launch
            }
            val orderItems = selectedProductId?.let {
                listOf(OrderItemRequest(productId = it, quantity = 1))
            }.orEmpty()

            if (orderItems.isEmpty()) {
                binding.btnPay.isEnabled = true
                Toast.makeText(requireContext(), "주문 가능한 메뉴가 없어요.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            submitOrder(session, storeId, orderItems, discountedTotal, clearCart = false)
        }
    }

    private fun loadFromStore(session: SessionManager, storeId: Long, menuId: Long, totalPrice: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val storeResponse = RetrofitClient.api.getStore(storeId).data ?: run {
                    findNavController().popBackStack()
                    return@launch
                }
                val store = storeResponse.toStore()
                val selectedMenu = storeResponse.products
                    .firstOrNull { it.productId == menuId && it.status != "SOLD_OUT" && it.quantityRemaining > 0 }
                    ?: storeResponse.products.firstOrNull { it.status != "SOLD_OUT" && it.quantityRemaining > 0 }

                binding.tvStoreName.text = store.name
                binding.tvMenuName.text = selectedMenu?.name ?: "주문 가능한 메뉴 없음"
                binding.tvPickupTimeDisplay.text = formatPickupRange(selectedMenu?.pickupEnd)

                val discountedTotal = selectedMenu?.discountPrice ?: totalPrice
                val originalTotal = selectedMenu?.originalPrice?.takeIf { it > 0 }
                    ?: selectedMenu?.discountPrice
                    ?: totalPrice
                setupPriceDisplay(originalTotal, discountedTotal, 1)

                binding.btnPay.setOnClickListener {
                    payFromStore(session, storeId, menuId, discountedTotal, selectedMenu?.productId)
                }
                if (autoPayAfterLink) {
                    autoPayAfterLink = false
                    payFromStore(session, storeId, menuId, discountedTotal, selectedMenu?.productId)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "가게 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun submitOrder(
        session: SessionManager,
        storeId: Long,
        orderItems: List<OrderItemRequest>,
        totalPrice: Int,
        clearCart: Boolean,
    ) {
        binding.btnPay.isEnabled = false
        binding.btnPay.text = getString(R.string.payment_processing_siru)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val order = RetrofitClient.api.createOrder(CreateOrderRequest(items = orderItems))
                    .data ?: throw IllegalStateException("Empty order response")
                session.lastOrderId = order.orderId
                if (clearCart) {
                    runCatching { RetrofitClient.api.clearCart() }
                    CartManager.clear()
                }
                runCatching { RetrofitClient.api.getMe().data }.getOrNull()?.let {
                    session.isSiruLinked = it.isSiruLinked
                    session.siruBalance = it.siruBalance
                }
                findNavController().navigate(
                    R.id.action_payment_to_pickup,
                    Bundle().apply { putLong("storeId", storeId) }
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "결제 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                binding.btnPay.isEnabled = true
                binding.btnPay.text = getString(R.string.btn_pay_siru, totalPrice.formatPrice())
            }
        }
    }

    private fun setupPriceDisplay(originalTotal: Int, discountedTotal: Int, itemCount: Int) {
        val discountAmount = (originalTotal - discountedTotal).coerceAtLeast(0)
        binding.tvItemPrice.text = discountedTotal.formatPrice()
        binding.tvOrderPrice.text = originalTotal.formatPrice()
        binding.tvDiscount.text = "-${discountAmount.formatPrice()}"
        binding.tvFinalPrice.text = discountedTotal.formatPrice()
        binding.tvSavingsMessage.text = "${discountAmount.formatPrice()}을 절약하고 음식 ${itemCount}개를 구해요"
        binding.btnPay.text = getString(R.string.btn_pay_siru, discountedTotal.formatPrice())
    }

    private fun navigateToSiruLink(storeId: Long, menuId: Long, totalPrice: Int, fromCart: Boolean) {
        findNavController().navigate(
            R.id.siruLinkFragment,
            Bundle().apply {
                putBoolean("returnToPayment", true)
                putLong("storeId", storeId)
                putLong("menuId", menuId)
                putInt("totalPrice", totalPrice)
                putBoolean("fromCart", fromCart)
            },
        )
    }

    private fun formatPickupRange(endTime: String?): String =
        endTime?.takeIf { it.isNotBlank() }?.let { "오늘 ${it.toDisplayHour()}까지 픽업" } ?: "픽업 시간 확인 중"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
