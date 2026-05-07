package com.example.deuktemsiru_buyer.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.R
import com.example.deuktemsiru_buyer.data.SampleData
import com.example.deuktemsiru_buyer.data.SessionManager
import com.example.deuktemsiru_buyer.data.Store
import com.example.deuktemsiru_buyer.data.toStore
import com.example.deuktemsiru_buyer.databinding.FragmentMapBinding
import com.example.deuktemsiru_buyer.databinding.ItemMapStoreCardBinding
import com.example.deuktemsiru_buyer.network.RetrofitClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import java.util.Locale

private val SEOUL = LatLng(37.5665, 126.9780)

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var loadedStores: List<Store> = emptyList()

    private val hasLocationPermission get() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            enableMyLocation()
            moveToCurrentLocation()
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

        val koConfig = Configuration(requireContext().resources.configuration)
        koConfig.setLocale(Locale.forLanguageTag("ko-KR"))
        val koContext = requireContext().createConfigurationContext(koConfig)
        mapView = MapView(koContext)
        binding.mapContainer.addView(mapView)
        mapView.onCreate(savedInstanceState)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        mapView.getMapAsync(this)
        requestLocationPermissions()
        loadStores()

        binding.btnMyLocation.setOnClickListener { moveToCurrentLocation() }
        binding.btnZoomIn.setOnClickListener { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) }
        binding.btnZoomOut.setOnClickListener { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.apply {
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isZoomControlsEnabled = false
            moveCamera(CameraUpdateFactory.newLatLngZoom(SEOUL, 12f))
            setOnInfoWindowClickListener { marker ->
                (marker.tag as? Int)?.let { storeId ->
                    findNavController().navigate(
                        R.id.action_map_to_storeDetail,
                        Bundle().apply { putInt("storeId", storeId) }
                    )
                }
            }
        }
        renderStoreMarkers()
        enableMyLocation()
        moveToCurrentLocation()
    }

    private fun requestLocationPermissions() {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (hasLocationPermission) googleMap?.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun moveToCurrentLocation() {
        if (!hasLocationPermission) return

        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lm.getCurrentLocation(LocationManager.GPS_PROVIDER, null, requireContext().mainExecutor, ::animateTo)
        } else {
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    animateTo(location)
                    lm.removeUpdates(this)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }, Looper.getMainLooper())
        }
    }

    private fun animateTo(location: Location) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f)
        )
    }

    private fun loadStores() {
        lifecycleScope.launch {
            try {
                val userId = if (session.isLoggedIn()) session.userId else null
                val stores = RetrofitClient.api.getStores(userId = userId).map { it.toStore() }
                loadedStores = stores
                populateBottomSheetCards(stores)
                renderStoreMarkers()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "지도 매장 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderStoreMarkers() {
        val map = googleMap ?: return
        val storesWithLocation = loadedStores.filter { it.hasValidLocation() }
        if (storesWithLocation.isEmpty()) return

        map.clear()

        val boundsBuilder = LatLngBounds.builder()
        storesWithLocation.forEach { store ->
            val position = LatLng(store.latitude, store.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(store.name)
                    .snippet("${store.discountRate}% ${SampleData.formatPrice(store.discountedPrice)}")
            )
            marker?.tag = store.id
            boundsBuilder.include(position)
        }

        val bounds = boundsBuilder.build()
        binding.mapContainer.post {
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96))
        }
    }

    private fun populateBottomSheetCards(stores: List<Store>) {
        binding.llMapStores.removeAllViews()
        stores.forEach { store ->
            val cardBinding = ItemMapStoreCardBinding.inflate(
                LayoutInflater.from(requireContext()), binding.llMapStores, false
            )
            cardBinding.tvEmoji.text = store.emoji
            cardBinding.tvBadge.text = getString(R.string.label_discount_rate, store.discountRate)
            cardBinding.tvName.text = store.name
            cardBinding.tvTime.text = getString(R.string.label_minutes_left, store.minutesUntilClose)
            cardBinding.tvPrice.text = SampleData.formatPrice(store.discountedPrice)

            val (clockIcon, clockColor) = if (store.minutesUntilClose <= 30) {
                R.drawable.ic_clock to "#FF3B30".toColorInt()
            } else {
                R.drawable.ic_clock_warning to "#FF8800".toColorInt()
            }
            cardBinding.tvTime.setTextColor(clockColor)
            cardBinding.tvTime.setCompoundDrawablesWithIntrinsicBounds(clockIcon, 0, 0, 0)

            cardBinding.cardRoot.setOnClickListener {
                findNavController().navigate(
                    R.id.action_map_to_storeDetail,
                    Bundle().apply { putInt("storeId", store.id) }
                )
            }
            binding.llMapStores.addView(cardBinding.root)
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::mapView.isInitialized) mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::mapView.isInitialized) mapView.onLowMemory()
    }

    override fun onDestroyView() {
        if (::mapView.isInitialized) mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    private fun Store.hasValidLocation() = latitude != 0.0 || longitude != 0.0
}
