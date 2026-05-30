package com.example.deuktemsiru_buyer.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.CartRepository
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.StoreRepository
import com.example.deuktemsiru_buyer.databinding.FragmentPaymentBinding
import com.example.deuktemsiru_buyer.network.CreateOrderRequest
import com.example.deuktemsiru_buyer.network.OrderItemRequest
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.Result
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.toDisplayHour
import com.example.deuktemsiru_buyer.util.toast
import kotlinx.coroutines.launch

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private lateinit var cartRepository: CartRepository
    private val storeRepository by lazy { StoreRepository(RetrofitClient.api) }
    private var autoPayAfterLink = false
    private var selectedPaymentTotal = 0
    private var selectedPickupTime: String? = null

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

        session = SessionManager(requireContext())
        cartRepository = CartRepository(RetrofitClient.api, session)
        val storeId = arguments?.getLong("storeId") ?: 0L
        if (storeId <= 0L) {
            toast("주문할 가게를 확인할 수 없어요.")
            findNavController().popBackStack()
            return
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        updateSiruWalletState()
        autoPayAfterLink = arguments?.getBoolean("autoPayAfterLink") ?: false
        selectedPickupTime = arguments?.getString("pickupTime")

        refreshSiruState()
        loadDraft(storeId)
    }

    private fun refreshSiruState() {
        viewLifecycleOwner.lifecycleScope.launch { syncSiruState() }
    }

    private suspend fun syncSiruState(): Boolean {
        val synced = session.syncMe(RetrofitClient.api)
        if (synced && _binding != null) updateSiruWalletState()
        return synced
    }

    private fun loadDraft(storeId: Long) {
        if (arguments?.getBoolean("fromCart") == true) {
            bindPayment(cartDraft(storeId))
        } else {
            loadStoreDraft(storeId)
        }
    }

    private fun cartDraft(storeId: Long): PaymentDraft {
        val items = CartManager.items
        return PaymentDraft(
            storeId = storeId,
            menuId = 0L,
            storeName = CartManager.storeName,
            menuSummary = "장바구니 메뉴 ${CartManager.totalCount}개",
            pickupStart = items.mapNotNull { it.pickupStart.takeIf(String::isNotBlank) }.maxOrNull(),
            pickupEnd = items.mapNotNull { it.pickupEnd.takeIf(String::isNotBlank) }.minOrNull(),
            originalTotal = items.sumOf { it.originalPrice * it.quantity },
            discountedTotal = CartManager.totalPrice,
            itemCount = CartManager.totalCount,
            orderItems = items.map { OrderItemRequest(productId = it.menuId, quantity = it.quantity) },
            clearCart = true,
            fromCart = true,
        )
    }

    private fun loadStoreDraft(storeId: Long) {
        val requestedMenuId = arguments?.getLong("menuId") ?: 0L
        val fallbackTotal = arguments?.getInt("totalPrice") ?: 0
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = storeRepository.getStore(storeId)) {
                is Result.Success -> {
                    val store = result.data
                    val menu = store.menus.firstOrNull { it.id == requestedMenuId && !it.isSoldOut }
                        ?: store.menus.firstOrNull { !it.isSoldOut }
                    bindPayment(
                        PaymentDraft(
                            storeId = store.id,
                            menuId = requestedMenuId,
                            storeName = store.name,
                            menuSummary = menu?.name ?: "주문 가능한 메뉴 없음",
                            pickupStart = menu?.pickupStart,
                            pickupEnd = menu?.pickupEnd,
                            originalTotal = menu?.originalPrice?.takeIf { it > 0 } ?: menu?.discountedPrice ?: fallbackTotal,
                            discountedTotal = menu?.discountedPrice ?: fallbackTotal,
                            itemCount = 1,
                            orderItems = menu?.let { listOf(OrderItemRequest(productId = it.id, quantity = 1)) }.orEmpty(),
                            clearCart = false,
                            fromCart = false,
                        )
                    )
                }
                is Result.Error -> {
                    toast("가게 정보를 불러오지 못했어요.")
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun bindPayment(draft: PaymentDraft) {
        binding.tvStoreName.text = draft.storeName
        binding.tvMenuName.text = draft.menuSummary
        binding.tvPickupTimeDisplay.text = formatPickupRange(draft.pickupStart, draft.pickupEnd)
        setupPickupSlots(draft)
        setupPriceDisplay(draft.originalTotal, draft.discountedTotal, draft.itemCount)
        binding.btnPay.setOnClickListener { pay(draft) }

        if (autoPayAfterLink) {
            autoPayAfterLink = false
            pay(draft)
        }
    }

    private fun pay(draft: PaymentDraft) {
        if (selectedPickupTime == null) {
            toast("선택 가능한 픽업 시간이 없어요.")
            return
        }
        binding.btnPay.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            syncSiruState()
            if (!session.isSiruLinked) {
                binding.btnPay.isEnabled = true
                toast("시루 계정 연동 후 결제할 수 있어요.")
                navigateToSiruLink(draft)
                return@launch
            }
            if (draft.orderItems.isEmpty()) {
                binding.btnPay.isEnabled = true
                toast("주문 가능한 메뉴가 없어요.")
                return@launch
            }
            submitOrder(draft)
        }
    }

    private suspend fun submitOrder(draft: PaymentDraft) {
        binding.btnPay.text = getString(R.string.payment_processing_siru)
        runCatching {
            val order = RetrofitClient.api.createOrder(
                CreateOrderRequest(items = draft.orderItems, pickupTime = selectedPickupTime)
            ).data
                ?: throw IllegalStateException("Empty order response")
            session.lastOrderId = order.orderId
            if (draft.clearCart) {
                cartRepository.clearServerCart()
                CartManager.clear()
            }
            session.syncMe(RetrofitClient.api)
            findNavController().navigate(
                R.id.action_payment_to_pickup,
                Bundle().apply { putLong("storeId", draft.storeId) },
            )
        }.onFailure {
            toast("결제 중 오류가 발생했어요.")
            binding.btnPay.isEnabled = true
            binding.btnPay.text = getString(R.string.btn_pay_siru, draft.discountedTotal.formatPrice())
        }
    }

    private fun setupPriceDisplay(originalTotal: Int, discountedTotal: Int, itemCount: Int) {
        selectedPaymentTotal = discountedTotal
        val discountAmount = (originalTotal - discountedTotal).coerceAtLeast(0)
        binding.tvItemPrice.text = discountedTotal.formatPrice()
        binding.tvOrderPrice.text = originalTotal.formatPrice()
        binding.tvDiscount.text = "-${discountAmount.formatPrice()}"
        binding.tvFinalPrice.text = discountedTotal.formatPrice()
        binding.tvSavingsMessage.text = "${discountAmount.formatPrice()}을 절약하고 음식 ${itemCount}개를 구해요"
        binding.btnPay.text = getString(R.string.btn_pay_siru, discountedTotal.formatPrice())
        updateSiruWalletState(discountedTotal)
        binding.tvSiruCashback.text = (discountedTotal * 5 / 100).formatPrice()
    }

    private fun updateSiruWalletState(paymentTotal: Int = selectedPaymentTotal) {
        binding.tvSiruBalance.text = session.siruBalance.formatPrice()
        binding.tvSiruBalanceAfter.text = (session.siruBalance - paymentTotal).coerceAtLeast(0).formatPrice()
        binding.tvSiruAccountStatus.text = getString(
            if (session.isSiruLinked) R.string.payment_siru_linked else R.string.payment_siru_unlinked
        )
        binding.tvPaymentTradeId.text = buildSiruTradeId()
    }

    private fun buildSiruTradeId(): String {
        val memberCode = session.memberId.takeIf { it > 0L } ?: 0L
        val orderCode = session.lastOrderId.takeIf { it > 0L } ?: memberCode
        return "SIRU-%06d-%04d".format(memberCode % 1_000_000, orderCode % 10_000)
    }

    private fun navigateToSiruLink(draft: PaymentDraft) {
        findNavController().navigate(
            R.id.siruLinkFragment,
            Bundle().apply {
                putBoolean("returnToPayment", true)
                putLong("storeId", draft.storeId)
                putLong("menuId", draft.menuId)
                putInt("totalPrice", draft.discountedTotal)
                putBoolean("fromCart", draft.fromCart)
                putString("pickupTime", selectedPickupTime)
            },
        )
    }

    private fun setupPickupSlots(draft: PaymentDraft) {
        val slots = pickupTimeSlots(draft.pickupStart, draft.pickupEnd)
        binding.timeSlots.removeAllViews()
        binding.pickupTimeSection.visibility = if (slots.isEmpty()) View.GONE else View.VISIBLE
        if (slots.isEmpty()) {
            selectedPickupTime = null
            return
        }

        selectedPickupTime = selectedPickupTime?.takeIf(slots::contains) ?: slots.first()
        val buttons = slots.mapIndexed { index, time ->
            Button(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.payment_slot_width),
                    resources.getDimensionPixelSize(R.dimen.payment_slot_height),
                ).apply {
                    if (index < slots.lastIndex) marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_sm)
                }
                isAllCaps = false
                stateListAnimator = null
                text = if (index == slots.lastIndex && slots.size > 1) "$time\n마감 직전" else time
                textSize = 14f
                contentDescription = "픽업 시간 $time"
                setOnClickListener {
                    selectedPickupTime = time
                    updatePickupSlotSelection(this@apply, buttons = binding.timeSlots.children())
                }
            }
        }
        buttons.forEach(binding.timeSlots::addView)
        updatePickupSlotSelection(null, buttons)
    }

    private fun LinearLayout.children(): List<Button> =
        (0 until childCount).map { getChildAt(it) as Button }

    private fun updatePickupSlotSelection(clicked: Button?, buttons: List<Button>) {
        buttons.forEach { button ->
            val selected = button === clicked || (clicked == null && button.contentDescription == "픽업 시간 $selectedPickupTime")
            button.isSelected = selected
            button.setBackgroundResource(if (selected) R.drawable.bg_time_slot_selected else R.drawable.bg_time_slot)
            button.setTextColor(requireContext().getColor(if (selected) R.color.primary else R.color.text))
        }
        binding.tvPickupTimeDisplay.text = selectedPickupTime?.let { "오늘 ${it.toDisplayHour()} 픽업" }
            ?: formatPickupRange(null, null)
    }

    private fun formatPickupRange(startTime: String?, endTime: String?): String = when {
        !startTime.isNullOrBlank() && !endTime.isNullOrBlank() ->
            "오늘 ${startTime.toDisplayHour()} ~ ${endTime.toDisplayHour()} 픽업"
        !endTime.isNullOrBlank() -> "오늘 ${endTime.toDisplayHour()}까지 픽업"
        else -> "픽업 시간 확인 중"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private data class PaymentDraft(
    val storeId: Long,
    val menuId: Long,
    val storeName: String,
    val menuSummary: String,
    val pickupStart: String?,
    val pickupEnd: String?,
    val originalTotal: Int,
    val discountedTotal: Int,
    val itemCount: Int,
    val orderItems: List<OrderItemRequest>,
    val clearCart: Boolean,
    val fromCart: Boolean,
)
