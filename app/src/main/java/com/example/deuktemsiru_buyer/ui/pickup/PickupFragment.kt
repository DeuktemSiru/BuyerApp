package com.example.deuktemsiru_buyer.ui.pickup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentPickupBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import kotlinx.coroutines.launch

class PickupFragment : Fragment() {

    private var _binding: FragmentPickupBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 42 * 60
    private var timerRunnable: Runnable? = null
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

        if (orderId > 0L) {
            loadOrder(orderId, storeId)
        } else {
            loadStoreFallback(storeId)
        }

        startCountdown()

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
            Toast.makeText(requireContext(), phone.ifEmpty { "전화번호를 불러오는 중..." }, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOrder(orderId: Long, storeId: Int) {
        lifecycleScope.launch {
            try {
                val order = RetrofitClient.api.getOrder(orderId).data ?: return@launch
                pickupCode = order.pickupCode

                binding.tvPickupTime.text = "${order.pickupTime}까지"
                binding.tvPickupCode.text = order.pickupCode.chunked(1).joinToString(" ")
                binding.tvStoreName.text = order.storeName
                binding.tvOrderMenu.text = order.items.joinToString(", ") { "${it.emoji} ${it.name}" }
                binding.tvPaidPrice.text = "%,d원".format(order.totalAmount)

                val store = RetrofitClient.api.getStore(order.storeId).data ?: return@launch
                binding.tvStoreAddress.text = store.address
                binding.tvStoreAddress.tag = store.phone
                storeLat = store.latitude
                storeLng = store.longitude
                storeName = order.storeName

                if (storeLat == 0.0 && storeLng == 0.0) {
                    loadStoreFallback(storeId.takeIf { it > 0 } ?: order.storeId.toInt())
                }
            } catch (e: Exception) {
                loadStoreFallback(storeId)
            }
        }
    }

    private fun loadStoreFallback(storeId: Int) {
        if (storeId <= 0) return
        lifecycleScope.launch {
            try {
                val store = RetrofitClient.api.getStore(storeId.toLong()).data ?: return@launch
                storeLat = store.latitude
                storeLng = store.longitude
                storeName = store.name
                binding.tvStoreName.text = store.name
                binding.tvStoreAddress.text = store.address
                binding.tvStoreAddress.tag = store.phone
            } catch (_: Exception) {
                binding.tvStoreAddress.tag = ""
            }
        }
    }

    private fun startCountdown() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                if (remainingSeconds > 0) {
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    binding.tvCountdown.text = "%02d:%02d".format(mins, secs)
                    remainingSeconds--
                    handler.postDelayed(this, 1000)
                } else {
                    binding.tvCountdown.text = "00:00"
                }
            }
        }
        handler.post(timerRunnable!!)
    }

    override fun onDestroyView() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroyView()
        _binding = null
    }
}
