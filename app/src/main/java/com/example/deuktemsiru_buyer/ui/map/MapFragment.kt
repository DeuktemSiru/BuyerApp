package com.example.deuktemsiru_buyer.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SampleData
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentMapBinding
import com.example.deuktemsiru_buyer.databinding.ItemMapStoreCardBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.launch

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            enableLocationComponent()
        } else {
            Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        requestLocationPermissions()
        initMap()
        loadStores()

        binding.btnMyLocation.setOnClickListener { moveToCurrentLocation() }
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun initMap() {
        binding.mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            enableLocationComponent()
            moveToCurrentLocation()
        }
    }

    private fun enableLocationComponent() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            binding.mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedClient.lastLocation.addOnSuccessListener { location ->
            location ?: return@addOnSuccessListener
            binding.mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(location.longitude, location.latitude))
                    .zoom(15.0)
                    .build()
            )
        }
    }

    private fun loadStores() {
        lifecycleScope.launch {
            try {
                val userId = if (session.isLoggedIn()) session.userId else null
                val stores = RetrofitClient.api.getStores(userId = userId).map { it.toStore() }
                populateBottomSheetCards(stores)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "지도 매장 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateBottomSheetCards(stores: List<com.example.deuktemsiru_buyer.data.Store>) {
        binding.llMapStores.removeAllViews()
        stores.forEach { store ->
            val cardBinding = ItemMapStoreCardBinding.inflate(
                LayoutInflater.from(requireContext()),
                binding.llMapStores,
                false
            )
            cardBinding.tvEmoji.text = store.emoji
            cardBinding.tvBadge.text = "${store.discountRate}%"
            cardBinding.tvName.text = store.name
            cardBinding.tvTime.text = "${store.minutesUntilClose}분 후 마감"
            cardBinding.tvPrice.text = SampleData.formatPrice(store.discountedPrice)

            if (store.minutesUntilClose <= 30) {
                cardBinding.tvTime.setTextColor(android.graphics.Color.parseColor("#FF3B30"))
                cardBinding.tvTime.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock, 0, 0, 0)
            } else {
                cardBinding.tvTime.setTextColor(android.graphics.Color.parseColor("#FF8800"))
                cardBinding.tvTime.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock_warning, 0, 0, 0)
            }

            cardBinding.cardRoot.setOnClickListener {
                findNavController().navigate(
                    R.id.action_map_to_storeDetail,
                    bundleOf("storeId" to store.id)
                )
            }

            binding.llMapStores.addView(cardBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
