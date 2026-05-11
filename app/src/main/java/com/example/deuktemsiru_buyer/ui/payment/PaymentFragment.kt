package com.example.deuktemsiru_buyer.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.SampleData
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

    private var selectedSlot = 1
    private val timeSlots = listOf("17:00", "17:30", "18:00", "18:30")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        setupTimeSlots()

        if (fromCart) {
            loadFromCart(session, storeId)
        } else {
            loadFromStore(session, storeId, menuId, totalPrice)
        }
    }

    private fun loadFromCart(session: SessionManager, storeId: Int) {
        val cartTotal = CartManager.totalPrice
        val extraDiscount = 1000
        val finalPrice = (cartTotal - extraDiscount).coerceAtLeast(100)

        binding.tvStoreName.text = CartManager.storeName
        binding.tvMenuName.text = "장바구니 메뉴 ${CartManager.totalCount}개"
        binding.tvOrderPrice.text = SampleData.formatPrice(cartTotal)
        binding.tvDiscount.text = "-0원"
        binding.tvExtraDiscount.text = "-${SampleData.formatPrice(extraDiscount)}"
        binding.tvFinalPrice.text = SampleData.formatPrice(finalPrice)
        binding.tvSavingsMessage.text = "${SampleData.formatPrice(extraDiscount)}을 추가 절약했어요"
        binding.btnPay.text = getString(R.string.btn_pay_siru, SampleData.formatPrice(finalPrice))

        binding.btnPay.setOnClickListener {
            val pickupTime = timeSlots.getOrElse(selectedSlot - 1) { "17:00" }
            val orderItems = CartManager.items.map {
                OrderItemRequest(menuItemId = it.menuId, quantity = it.quantity)
            }

            binding.btnPay.isEnabled = false
            binding.btnPay.text = getString(R.string.payment_processing_siru)

            lifecycleScope.launch {
                try {
                    val order = RetrofitClient.api.createOrder(
                        buyerId = session.userId,
                        req = CreateOrderRequest(
                            storeId = CartManager.storeId,
                            items = orderItems,
                            pickupTime = pickupTime,
                        )
                    )
                    session.lastOrderId = order.id
                    CartManager.clear()
                    findNavController().navigate(
                        R.id.action_payment_to_pickup,
                        Bundle().apply { putInt("storeId", storeId) }
                    )
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "결제 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                    binding.btnPay.isEnabled = true
                    binding.btnPay.text = getString(R.string.btn_pay_siru, SampleData.formatPrice(finalPrice))
                }
            }
        }
    }

    private fun loadFromStore(session: SessionManager, storeId: Int, menuId: Int, totalPrice: Int) {
        lifecycleScope.launch {
            try {
                val userId = if (session.isLoggedIn()) session.userId else null
                val storeResponse = RetrofitClient.api.getStore(storeId.toLong(), userId)
                val store = storeResponse.toStore()
                val selectedMenu = storeResponse.menus
                    .firstOrNull { it.id.toInt() == menuId && !it.isSoldOut }
                    ?: storeResponse.menus.firstOrNull { !it.isSoldOut }

                binding.tvStoreName.text = store.name
                binding.tvMenuName.text = selectedMenu?.name ?: "주문 가능한 메뉴 없음"

                val originalTotal = selectedMenu?.originalPrice ?: totalPrice
                val discountedTotal = selectedMenu?.discountedPrice ?: totalPrice
                val discountAmount = originalTotal - discountedTotal
                val extraDiscount = 1000
                val finalPrice = (discountedTotal - extraDiscount).coerceAtLeast(100)

                binding.tvOrderPrice.text = SampleData.formatPrice(originalTotal)
                binding.tvDiscount.text = "-${SampleData.formatPrice(discountAmount)}"
                binding.tvExtraDiscount.text = "-${SampleData.formatPrice(extraDiscount)}"
                binding.tvFinalPrice.text = SampleData.formatPrice(finalPrice)
                binding.tvSavingsMessage.text = "${SampleData.formatPrice(discountAmount + extraDiscount)}을 절약하고 음식 1개를 구해요"
                binding.btnPay.text = getString(R.string.btn_pay_siru, SampleData.formatPrice(finalPrice))

                binding.btnPay.setOnClickListener {
                    val pickupTime = timeSlots.getOrElse(selectedSlot - 1) { "17:00" }
                    val orderItems = selectedMenu?.let {
                        listOf(OrderItemRequest(menuItemId = it.id, quantity = 1))
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
                                buyerId = session.userId,
                                req = CreateOrderRequest(
                                    storeId = storeId.toLong(),
                                    items = orderItems,
                                    pickupTime = pickupTime,
                                )
                            )
                            session.lastOrderId = order.id
                            findNavController().navigate(
                                R.id.action_payment_to_pickup,
                                Bundle().apply { putInt("storeId", storeId) }
                            )
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "결제 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
                            binding.btnPay.isEnabled = true
                            binding.btnPay.text = getString(R.string.btn_pay_siru, SampleData.formatPrice(finalPrice))
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "가게 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun setupTimeSlots() {
        val slots = listOf(binding.slot1, binding.slot2, binding.slot3, binding.slot4)
        slots.forEachIndexed { index, slot ->
            slot.setOnClickListener {
                selectedSlot = index + 1
                updateSlotSelection(slots)
            }
        }
        updateSlotSelection(slots)
    }

    private fun updateSlotSelection(slots: List<Button>) {
        slots.forEachIndexed { index, slot ->
            slot.backgroundTintList = null
            if (index + 1 == selectedSlot) {
                slot.setBackgroundResource(R.drawable.bg_time_slot_selected)
                slot.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
            } else {
                slot.setBackgroundResource(R.drawable.bg_time_slot)
                slot.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
