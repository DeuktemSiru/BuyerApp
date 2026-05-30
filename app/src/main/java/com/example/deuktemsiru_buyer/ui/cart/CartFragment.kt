package com.example.deuktemsiru_buyer.ui.cart

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.CartRepository
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentCartBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.util.formatDistanceMeters
import com.example.deuktemsiru_buyer.util.formatPrice
import com.example.deuktemsiru_buyer.util.getCurrentLocation
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CartAdapter
    private lateinit var session: SessionManager
    private lateinit var cartRepository: CartRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())
        cartRepository = CartRepository(RetrofitClient.api, session)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = CartAdapter(
            items = CartManager.items,
            onDelete = { item ->
                syncCart({ cartRepository.removeProduct(item.menuId) }) {
                    CartManager.remove(item.menuId)
                }
            },
            onIncrease = { item ->
                syncCart({ cartRepository.setQuantity(item.menuId, item.quantity + 1) }) {
                    CartManager.increaseQuantity(item.menuId)
                }
            },
            onDecrease = { item ->
                val nextQuantity = item.quantity - 1
                syncCart({
                    if (nextQuantity <= 0) cartRepository.removeProduct(item.menuId)
                    else cartRepository.setQuantity(item.menuId, nextQuantity)
                }) {
                    CartManager.decreaseQuantity(item.menuId)
                }
            },
            onSelectionChanged = { updateSelectAllState() },
        )

        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@CartFragment.adapter
        }

        binding.cbSelectAll.setOnClickListener {
            if (adapter.allSelected) adapter.deselectAll() else adapter.selectAll()
            updateSelectAllState()
        }

        binding.btnDeleteSelected.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                adapter.selectedIds.toList().forEach {
                    if (cartRepository.removeProduct(it)) CartManager.remove(it)
                }
                refresh()
            }
        }

        binding.btnAddMore.setOnClickListener {
            findNavController().navigate(
                R.id.action_cart_to_storeDetail,
                Bundle().apply { putLong("storeId", CartManager.storeId) }
            )
        }

        binding.btnCheckout.setOnClickListener {
            if (CartManager.isEmpty) return@setOnClickListener
            findNavController().navigate(
                R.id.action_cart_to_payment,
                Bundle().apply {
                    putLong("storeId", CartManager.storeId)
                    putInt("totalPrice", CartManager.totalPrice)
                    putBoolean("fromCart", true)
                }
            )
        }

        refresh()
        if (session.isLoggedIn()) {
            loadServerCart()
        } else {
            fetchDistanceAndCarbon()
        }
    }

    private fun syncCart(action: suspend () -> Boolean, onSuccess: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = action()
            if (ok) { onSuccess(); refresh() }
        }
    }

    private fun loadServerCart() {
        viewLifecycleOwner.lifecycleScope.launch {
            cartRepository.loadServerCart()
            refresh()
            fetchDistanceAndCarbon()
        }
    }

    private fun refresh() {
        if (_binding == null) return
        if (CartManager.isEmpty) {
            binding.layoutContent.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.btnCheckout.isEnabled = false
            binding.btnCheckout.text = "장바구니가 비어있어요"
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
            binding.btnCheckout.isEnabled = true
            binding.tvStoreEmoji.text = CartManager.storeEmoji
            binding.tvStoreName.text = CartManager.storeName
            binding.tvSubtotal.text = CartManager.totalPrice.formatPrice()
            binding.tvTotal.text = CartManager.totalPrice.formatPrice()
            binding.btnCheckout.text = "${CartManager.totalPrice.formatPrice()} 예약하기"
            adapter.update(CartManager.items)
            updateSelectAllState()
            updateCarbonLabel()
        }
    }

    private fun updateSelectAllState() {
        if (_binding == null) return
        binding.cbSelectAll.text = if (adapter.allSelected) "선택 해제" else "전체 선택"
        binding.cbSelectAll.isChecked = adapter.allSelected
    }

    private fun updateCarbonLabel() {
        val grams = CartManager.totalCount * 1200L
        binding.tvCarbon.text = if (grams >= 1000) "약 %.1fkg CO₂".format(grams / 1000.0)
                                else "약 ${grams}g CO₂"
    }

    private fun fetchDistanceAndCarbon() {
        val storeLat = CartManager.storeLat
        val storeLng = CartManager.storeLng
        if (_binding == null) return

        if (storeLat == 0.0 || storeLng == 0.0) {
            binding.tvDistance.text = "위치 정보 없음"
            return
        }

        getCurrentLocation(
            onMissingPermission = { _binding?.tvDistance?.text = "위치 권한 없음" },
            onUnavailable = { _binding?.tvDistance?.text = "위치 확인 불가" },
            onLocation = { location -> if (_binding != null) showDistance(location, storeLat, storeLng) },
        )
    }

    private fun showDistance(userLocation: Location, storeLat: Double, storeLng: Double) {
        val results = FloatArray(1)
        Location.distanceBetween(userLocation.latitude, userLocation.longitude, storeLat, storeLng, results)
        val distMeters = results[0].toInt()
        binding.tvDistance.text = "가게까지 ${formatDistanceMeters(distMeters)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
