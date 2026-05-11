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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.CartManager
import com.example.deuktemsiru_buyer.databinding.FragmentCartBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = CartAdapter(
            items = CartManager.items,
            onDelete = { item ->
                CartManager.remove(item.menuId)
                refresh()
            },
            onIncrease = { item ->
                CartManager.increaseQuantity(item.menuId)
                refresh()
            },
            onDecrease = { item ->
                CartManager.decreaseQuantity(item.menuId)
                refresh()
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
            adapter.selectedIds.toList().forEach { CartManager.remove(it) }
            refresh()
        }

        binding.btnAddMore.setOnClickListener {
            findNavController().navigate(
                R.id.action_cart_to_storeDetail,
                bundleOf("storeId" to CartManager.storeId.toInt())
            )
        }

        binding.btnCheckout.setOnClickListener {
            if (CartManager.isEmpty) return@setOnClickListener
            findNavController().navigate(
                R.id.action_cart_to_payment,
                bundleOf(
                    "storeId" to CartManager.storeId.toInt(),
                    "totalPrice" to CartManager.totalPrice,
                    "fromCart" to true,
                )
            )
        }

        refresh()
        fetchDistanceAndCarbon()
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
            binding.tvSubtotal.text = formatPrice(CartManager.totalPrice)
            binding.tvTotal.text = formatPrice(CartManager.totalPrice)
            binding.btnCheckout.text = "${formatPrice(CartManager.totalPrice)} 예약하기"
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

    private fun formatPrice(price: Int): String = "%,d원".format(price)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
