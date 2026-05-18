package com.example.deuktemsiru_buyer.util

import android.os.Bundle
import com.google.android.gms.maps.MapView

class MapViewLifecycleDelegate(private val mapView: () -> MapView?) {
    fun onStart() = mapView()?.onStart()
    fun onResume() = mapView()?.onResume()
    fun onPause() = mapView()?.onPause()
    fun onStop() = mapView()?.onStop()
    fun onSaveInstanceState(outState: Bundle) = mapView()?.onSaveInstanceState(outState)
    fun onLowMemory() = mapView()?.onLowMemory()
    fun onDestroy() = mapView()?.onDestroy()
}
