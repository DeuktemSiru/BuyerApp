package com.example.deuktemsiru_buyer.ui.orders

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.databinding.BottomSheetOrderDetailBinding
import com.example.deuktemsiru_buyer.network.OrderDetailResponse
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.generateQrBitmap
import com.example.deuktemsiru_buyer.util.orderStatusLabel
import com.example.deuktemsiru_buyer.util.toHourMinute
import com.example.deuktemsiru_buyer.util.toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class OrderDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOrderDetailBinding? = null
    private val binding get() = _binding!!

    private var orderId: Long = -1
    private var onOrderCancelled: (() -> Unit)? = null

    companion object {
        fun newInstance(orderId: Long, onOrderCancelled: () -> Unit) =
            OrderDetailBottomSheet().also {
                it.arguments = Bundle().apply { putLong("orderId", orderId) }
                it.onOrderCancelled = onOrderCancelled
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getLong("orderId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDetail()
    }

    private fun loadDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val detail = RetrofitClient.api.getOrder(orderId).data ?: return@launch
                bindDetail(detail)
            } catch (e: Exception) {
                toast("상세 정보를 불러오지 못했어요.")
                dismiss()
            }
        }
    }

    private fun bindDetail(detail: OrderDetailResponse) {
        binding.tvStoreName.text = detail.storeName
        binding.tvOrderNumber.text = "#${detail.orderId}"

        val statusText = orderStatusLabel(detail.status)
        binding.tvStatusBadge.text = statusText

        binding.itemsContainer.removeAllViews()
        val textColor = ContextCompat.getColor(requireContext(), R.color.text)
        val textSubColor = ContextCompat.getColor(requireContext(), R.color.text_sub)
        detail.items.forEach { item ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dpToPx(6))
            }
            val tvName = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "${item.productName} × ${item.quantity}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(textColor)
            }
            val tvPrice = TextView(requireContext()).apply {
                text = (item.unitPrice * item.quantity).formatPrice()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(textSubColor)
            }
            row.addView(tvName)
            row.addView(tvPrice)
            binding.itemsContainer.addView(row)
        }

        binding.tvPickupTime.text = detail.pickupTime?.toHourMinute() ?: "미정"

        binding.tvTotalAmount.text = detail.totalPrice.formatPrice()

        val code = detail.pickupCode
        if (!code.isNullOrBlank()) {
            binding.layoutQr.visibility = View.VISIBLE
            binding.tvPickupCode.text = code
            generateQrBitmap(code)?.let { binding.ivQrCode.setImageBitmap(it) }
        }

        if (detail.status == "PENDING" || detail.status == "CONFIRMED") {
            binding.btnCancel.visibility = View.VISIBLE
            binding.btnCancel.setOnClickListener { confirmCancel(detail) }
        }
    }

    private fun confirmCancel(detail: OrderDetailResponse) {
        AlertDialog.Builder(requireContext())
            .setTitle("주문을 취소할까요?")
            .setMessage("${detail.storeName} 주문을 취소하면 되돌릴 수 없어요.")
            .setPositiveButton("취소하기") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        RetrofitClient.api.cancelOrder(detail.orderId)
                        toast("주문이 취소됐어요.")
                        onOrderCancelled?.invoke()
                        dismiss()
                    } catch (e: Exception) {
                        toast("취소에 실패했어요.")
                    }
                }
            }
            .setNegativeButton("돌아가기", null)
            .show()
    }

    private fun dpToPx(dp: Int) =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
