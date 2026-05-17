package com.example.deuktemsiru_buyer.ui.cart

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.data.CartItem
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.databinding.FragmentCartBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.example.deuktemsiru_buyer.network.CartUpdateRequest
import com.example.deuktemsiru_buyer.util.formatPrice
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CartAdapter
    private lateinit var session: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = CartAdapter(
            items = CartManager.items,
            onDelete = { item ->
                syncCart({ removeServerItem(item.menuId) }) {
                    CartManager.remove(item.menuId)
                }
            },
            onIncrease = { item ->
                syncCart({ syncServerQuantity(item.menuId, item.quantity + 1) }) {
                    CartManager.increaseQuantity(item.menuId)
                }
            },
            onDecrease = { item ->
                val nextQuantity = item.quantity - 1
                syncCart({
                    if (nextQuantity <= 0) removeServerItem(item.menuId)
                    else syncServerQuantity(item.menuId, nextQuantity)
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
                    if (removeServerItem(it)) CartManager.remove(it)
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
        if (!session.isLoggedIn()) return
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { RetrofitClient.api.getCart().data }
                .onSuccess { cart ->
                    val items = cart?.items.orEmpty()
                    if (items.isNotEmpty()) {
                        val first = items.first()
                        CartManager.replaceFromServer(
                            storeId = first.storeId,
                            storeName = first.storeName,
                            storeLat = first.storeLatitude,
                            storeLng = first.storeLongitude,
                            items = items.map {
                                CartItem(
                                    menuId = it.productId,
                                    menuName = it.productName,
                                    emoji = "🛍️",
                                    originalPrice = it.originalPrice,
                                    discountedPrice = it.discountPrice,
                                    pickupStart = it.pickupStart,
                                    pickupEnd = it.pickupEnd,
                                    quantity = it.quantity,
                                )
                            },
                            serverIds = items.associate { it.productId to it.cartItemId },
                        )
                    } else {
                        CartManager.clear()
                    }
                    refresh()
                    fetchDistanceAndCarbon()
                }
                .onFailure {
                    fetchDistanceAndCarbon()
                }
        }
    }

    private suspend fun removeServerItem(productId: Long): Boolean {
        val cartItemId = CartManager.serverCartItemIds[productId] ?: return true
        if (!session.isLoggedIn()) return true
        return runCatching { RetrofitClient.api.removeCartItem(cartItemId) }
            .onFailure { loadServerCart() }
            .isSuccess
    }

    private suspend fun syncServerQuantity(productId: Long, quantity: Int): Boolean {
        val cartItemId = CartManager.serverCartItemIds[productId] ?: return true
        if (!session.isLoggedIn()) return true
        return runCatching {
            RetrofitClient.api.updateCartItem(cartItemId, CartUpdateRequest(quantity))
        }
            .onFailure { loadServerCart() }
            .isSuccess
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

    @SuppressLint("MissingPermission")
    private fun fetchDistanceAndCarbon() {
        val storeLat = CartManager.storeLat
        val storeLng = CartManager.storeLng
        if (_binding == null) return

        if (storeLat == 0.0 || storeLng == 0.0) {
            binding.tvDistance.text = "위치 정보 없음"
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            binding.tvDistance.text = "위치 권한 없음"
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (_binding == null) return@addOnSuccessListener
                if (location != null) {
                    showDistance(location, storeLat, storeLng)
                } else {
                    // lastLocation null (에뮬레이터 등) → 신선한 위치 요청
                    fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener { fresh: Location? ->
                            if (_binding == null) return@addOnSuccessListener
                            if (fresh != null) showDistance(fresh, storeLat, storeLng)
                            else binding.tvDistance.text = "위치 확인 불가"
                        }
                        .addOnFailureListener {
                            if (_binding != null) binding.tvDistance.text = "위치 확인 불가"
                        }
                }
            }
            .addOnFailureListener {
                if (_binding != null) binding.tvDistance.text = "위치 확인 불가"
            }
    }

    private fun showDistance(userLocation: Location, storeLat: Double, storeLng: Double) {
        val results = FloatArray(1)
        Location.distanceBetween(userLocation.latitude, userLocation.longitude, storeLat, storeLng, results)
        val distMeters = results[0].toInt()
        binding.tvDistance.text = if (distMeters >= 1000)
            "가게까지 %.1fkm".format(distMeters / 1000.0)
        else
            "가게까지 ${distMeters}m"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
