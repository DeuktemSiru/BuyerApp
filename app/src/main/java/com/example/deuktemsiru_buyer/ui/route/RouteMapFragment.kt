package com.example.deuktemsiru_buyer.ui.route

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.deuktemsiru_buyer.BuildConfig
import com.example.deuktemsiru_buyer.databinding.FragmentRouteMapBinding
import com.example.deuktemsiru_buyer.network.TmapClient
import com.example.deuktemsiru_buyer.network.TmapRouteRequest
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch

class RouteMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentRouteMapBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null
    private var locationCallback: LocationCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRouteMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val destLat = arguments?.getDouble("destLat") ?: return
        val destLng = arguments?.getDouble("destLng") ?: return
        val destName = arguments?.getString("destName") ?: "도착지"
        val dest = LatLng(destLat, destLng)

        map.addMarker(
            MarkerOptions()
                .position(dest)
                .title(destName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(dest, 15f))

        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        binding.progress.visibility = View.VISIBLE
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .setMaxUpdates(1)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                fusedClient.removeLocationUpdates(this)
                locationCallback = null
                drawRoute(map, location.latitude, location.longitude, destLat, destLng, destName)
            }
        }
        fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    private fun drawRoute(
        map: GoogleMap,
        startLat: Double, startLng: Double,
        destLat: Double, destLng: Double,
        destName: String,
    ) {
        lifecycleScope.launch {
            try {
                val response = TmapClient.api.getPedestrianRoute(
                    appKey = BuildConfig.TMAP_API_KEY,
                    request = TmapRouteRequest(
                        startX = startLng.toString(),
                        startY = startLat.toString(),
                        endX = destLng.toString(),
                        endY = destLat.toString(),
                        endName = destName,
                    )
                )

                val coords = mutableListOf<LatLng>()
                response.features
                    .filter { it.geometry.type == "LineString" }
                    .forEach { feature ->
                        feature.geometry.coordinates.asJsonArray.forEach { point ->
                            val pt = point.asJsonArray
                            coords.add(LatLng(pt[1].asDouble, pt[0].asDouble))
                        }
                    }

                if (coords.size < 2) return@launch

                map.addPolyline(
                    PolylineOptions()
                        .addAll(coords)
                        .color(Color.parseColor("#FF6B00"))
                        .width(10f)
                )

                val boundsBuilder = LatLngBounds.Builder()
                coords.forEach { boundsBuilder.include(it) }
                boundsBuilder.include(LatLng(startLat, startLng))
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80))

                var totalDistance = 0
                var totalTime = 0
                response.features.forEach { feature ->
                    val props = feature.properties ?: return@forEach
                    if (props.has("totalDistance")) {
                        totalDistance = props.get("totalDistance").asInt
                        totalTime = props.get("totalTime").asInt
                    }
                }
                showRouteInfo(destName, totalDistance, totalTime)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "경로를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
            } finally {
                if (_binding != null) binding.progress.visibility = View.GONE
            }
        }
    }

    private fun showRouteInfo(destName: String, distanceMeters: Int, timeSeconds: Int) {
        if (_binding == null) return
        val minutes = (timeSeconds / 60).coerceAtLeast(1)
        val distanceText = if (distanceMeters >= 1000) "%.1fkm".format(distanceMeters / 1000.0)
                           else "${distanceMeters}m"

        binding.tvDestName.text = destName
        binding.tvWalkTime.text = "도보 약 ${minutes}분"
        binding.tvDistance.text = distanceText
        binding.tvDestLabel.text = destName
        binding.cardRouteInfo.visibility = View.VISIBLE
    }

    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onStop() { super.onStop(); binding.mapView.onStop() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (_binding != null) binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        locationCallback?.let {
            LocationServices.getFusedLocationProviderClient(requireContext()).removeLocationUpdates(it)
        }
        locationCallback = null
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }
}