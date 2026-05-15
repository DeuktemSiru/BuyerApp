package com.example.deuktemsiru_buyer.ui.pickup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentPickupBinding
import com.example.deuktemsiru_buyer.network.OrderDetailResponse
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Calendar

class PickupFragment : Fragment() {

    private var _binding: FragmentPickupBinding? = null
    private val binding get() = _binding!!

    private var timerJob: Job? = null
    private var pollJob: Job? = null
    private var pickupCode = "----"
    private var storeLat = 0.0
    private var storeLng = 0.0
    private var storeName = ""

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
        val storeId = arguments?.getInt("storeId") ?: 0

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

    private fun loadOrder(orderId: Long, storeId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val order = RetrofitClient.api.getOrder(orderId).data ?: run {
                    showPendingUi()
                    startStatusPolling(orderId, storeId)
                    return@launch
                }
                applyOrderUi(order, storeId)
            } catch (e: Exception) {
                showPendingUi()
                startStatusPolling(orderId, storeId)
            }
        }
    }

    private fun applyOrderUi(order: OrderDetailResponse, storeId: Int) {
        if (order.status == "CONFIRMED") {
            showConfirmedUi(order)
        } else {
            showPendingUi()
            startStatusPolling(order.orderId, storeId)
        }
        binding.tvStoreName.text = order.storeName
        binding.tvOrderMenu.text = order.items.joinToString(", ") { "${it.productName} x${it.quantity}" }
        binding.tvPaidPrice.text = "%,d원".format(order.totalPrice)
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

        val pickupEndTime = order.pickupEnd
        binding.tvPickupTime.text = if (pickupEndTime != null) "${formatDisplayTime(pickupEndTime)}까지" else "픽업 시간 확인 중"
        binding.tvPickupCode.text = pickupCode.ifBlank { "----" }.chunked(1).joinToString(" ")
        showQrCode(pickupCode)
        startCountdown(remainingSecondsUntil(pickupEndTime))
    }

    private fun startStatusPolling(orderId: Long, storeId: Int) {
        pollJob?.cancel()
        pollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (_binding != null) {
                delay(5_000)
                try {
                    val order = RetrofitClient.api.getOrder(orderId).data ?: continue
                    if (order.status == "CONFIRMED") {
                        applyOrderUi(order, storeId)
                        break
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun loadStoreFallback(storeId: Int) {
        if (storeId <= 0) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val store = RetrofitClient.api.getStore(storeId.toLong()).data ?: return@launch
                storeLat = store.latitude
                storeLng = store.longitude
                if (storeName.isBlank()) {
                    storeName = store.name
                    binding.tvStoreName.text = store.name
                }
                binding.tvStoreAddress.text = store.address
                binding.tvStoreAddress.tag = store.phone
            } catch (_: Exception) {
                binding.tvStoreAddress.tag = ""
            }
        }
    }

    private fun showQrCode(code: String) {
        if (code.isBlank()) return
        try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bits = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 400, 400, hints)
            val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.RGB_565)
            for (x in 0 until 400) {
                for (y in 0 until 400) {
                    bitmap.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            binding.ivQrCode.setImageBitmap(bitmap)
            binding.ivQrCode.visibility = View.VISIBLE
        } catch (_: Exception) {
            binding.ivQrCode.visibility = View.GONE
        }
    }

    private fun remainingSecondsUntil(pickupEnd: String?): Long {
        if (pickupEnd == null) return 42 * 60L
        return try {
            val time = pickupEnd.substringAfter("T", pickupEnd).substringBefore(".")
            val parts = time.split(":")
            val endHour = parts.getOrNull(0)?.toIntOrNull() ?: return 42 * 60L
            val endMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val now = Calendar.getInstance()
            val nowSecs = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND)
            val endSecs = endHour * 3600 + endMin * 60
            if (endSecs >= nowSecs) (endSecs - nowSecs).toLong()
            else (24 * 3600 - nowSecs + endSecs).toLong()
        } catch (_: Exception) {
            42 * 60L
        }
    }

    private fun formatDisplayTime(pickupEnd: String): String {
        val time = pickupEnd.substringAfter("T", pickupEnd).substringBefore(".")
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return time
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return "%02d:%02d".format(hour, min)
    }

    private fun startCountdown(totalSeconds: Long) {
        timerJob?.cancel()
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                countdownFlow(totalSeconds).collect { remaining ->
                    if (_binding == null) return@collect
                    val mins = remaining / 60
                    val secs = remaining % 60
                    binding.tvCountdown.text = "%02d:%02d".format(mins, secs)
                }
            }
        }
    }

    override fun onDestroyView() {
        timerJob?.cancel()
        pollJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}

private fun countdownFlow(startSeconds: Long) = flow {
    var remaining = startSeconds
    while (remaining >= 0) {
        emit(remaining)
        if (remaining == 0L) break
        delay(1_000)
        remaining--
    }
}
