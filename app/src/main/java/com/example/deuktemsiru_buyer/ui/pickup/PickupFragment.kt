package com.example.deuktemsiru_buyer.ui.pickup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.OrderRepository
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.StoreRepository
import com.example.deuktemsiru_buyer.databinding.FragmentPickupBinding
import com.example.deuktemsiru_buyer.network.OrderDetailResponse
import com.example.deuktemsiru_buyer.data.minutesUntilClose
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.Result
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.generateQrBitmap
import com.example.deuktemsiru_buyer.util.startTimerInto
import com.example.deuktemsiru_buyer.util.toHourMinute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PickupFragment : Fragment() {

    private var _binding: FragmentPickupBinding? = null
    private val binding get() = _binding!!

    private var timerJob: Job? = null
    private var pollJob: Job? = null
    private var pickupCode = "----"
    private var storeLat = 0.0
    private var storeLng = 0.0
    private var storeName = ""
    private val orderRepository by lazy { OrderRepository(RetrofitClient.api) }
    private val storeRepository by lazy { StoreRepository(RetrofitClient.api) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPickupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val orderId = session.lastOrderId
        val storeId = arguments?.getLong("storeId") ?: 0L

        loadStoreFallback(storeId)

        if (orderId > 0L) {
            loadOrder(orderId, storeId)
        } else {
            showPendingUi()
        }

        binding.llCode.setOnLongClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("픽업코드", pickupCode))
            Toast.makeText(requireContext(), "픽업 코드가 복사되었어요", Toast.LENGTH_SHORT).show()
            true
        }

        binding.btnDirections.setOnClickListener {
            if (storeLat == 0.0 && storeLng == 0.0) {
                Toast.makeText(requireContext(), "가게 위치를 불러오는 중이에요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(
                R.id.action_pickup_to_routeMap,
                Bundle().apply {
                    putDouble("destLat", storeLat)
                    putDouble("destLng", storeLng)
                    putString("destName", storeName)
                }
            )
        }

        binding.btnCall.setOnClickListener {
            val phone = binding.tvStoreAddress.tag as? String ?: ""
            if (phone.isBlank()) {
                Toast.makeText(requireContext(), "전화번호를 불러오는 중...", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }
        }
    }

    private fun loadOrder(orderId: Long, storeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val order = runCatching { orderRepository.getOrder(orderId) }.getOrNull()
            if (order != null) applyOrderUi(order, storeId) else waitForConfirmedOrder(orderId, storeId)
        }
    }

    private fun applyOrderUi(order: OrderDetailResponse, storeId: Long) {
        if (order.status == "CONFIRMED") {
            showConfirmedUi(order)
        } else {
            waitForConfirmedOrder(order.orderId, storeId)
        }
        binding.tvStoreName.text = order.storeName
        binding.tvOrderMenu.text = order.items.joinToString(", ") { "${it.productName} x${it.quantity}" }
        binding.tvPaidPrice.text = order.totalPrice.formatPrice()
        storeName = order.storeName
    }

    private fun showPendingUi() {
        if (_binding == null) return
        binding.tvStatusBadge.text = "수락 대기 중"
        binding.tvPendingMessage.visibility = View.VISIBLE
        binding.llPickupContent.visibility = View.GONE
    }

    private fun showConfirmedUi(order: OrderDetailResponse) {
        if (_binding == null) return
        pickupCode = order.pickupCode.orEmpty()

        binding.tvStatusBadge.text = "픽업 대기 중"
        binding.tvPendingMessage.visibility = View.GONE
        binding.llPickupContent.visibility = View.VISIBLE

        val pickupEndTime = order.pickupTime?.substringAfter("~", order.pickupTime)
        binding.tvPickupTime.text = if (pickupEndTime != null) "${pickupEndTime.toHourMinute()}까지" else "픽업 시간 확인 중"
        binding.tvPickupCode.text = pickupCode.ifBlank { "----" }.chunked(1).joinToString(" ")
        showQrCode(pickupCode)
        startCountdown(remainingSecondsUntil(pickupEndTime))
    }

    private fun waitForConfirmedOrder(orderId: Long, storeId: Long) {
        showPendingUi()
        pollJob?.cancel()
        pollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (_binding != null) {
                delay(5_000)
                val order = runCatching { orderRepository.getOrder(orderId) }.getOrNull() ?: continue
                if (order.status == "CONFIRMED") {
                    applyOrderUi(order, storeId)
                    break
                }
            }
        }
    }

    private fun loadStoreFallback(storeId: Long) {
        if (storeId <= 0L) return
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = storeRepository.getStore(storeId)) {
                is Result.Success -> {
                    val store = result.data
                storeLat = store.latitude
                storeLng = store.longitude
                if (storeName.isBlank()) {
                    storeName = store.name
                    binding.tvStoreName.text = store.name
                }
                binding.tvStoreAddress.text = store.address
                binding.tvStoreAddress.tag = store.phone
                }
                is Result.Error -> binding.tvStoreAddress.tag = ""
                is Result.Loading -> Unit
            }
        }
    }

    private fun showQrCode(code: String) {
        if (code.isBlank()) return
        val bitmap = generateQrBitmap(code, size = 400)
        if (bitmap != null) {
            binding.ivQrCode.setImageBitmap(bitmap)
            binding.ivQrCode.visibility = View.VISIBLE
        } else {
            binding.ivQrCode.visibility = View.GONE
        }
    }

    private fun remainingSecondsUntil(pickupEnd: String?): Long {
        if (pickupEnd == null) return 42 * 60L
        return minutesUntilClose(pickupEnd) * 60L
    }

    private fun startCountdown(totalSeconds: Long) {
        timerJob = startTimerInto(totalSeconds, timerJob) { remaining ->
            if (_binding == null) return@startTimerInto
            val mins = remaining / 60
            val secs = remaining % 60
            binding.tvCountdown.text = "%02d:%02d".format(mins, secs)
        }
    }

    override fun onDestroyView() {
        timerJob?.cancel()
        pollJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
