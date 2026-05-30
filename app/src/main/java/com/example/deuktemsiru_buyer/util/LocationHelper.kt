package com.example.deuktemsiru_buyer.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

val Fragment.hasLocationPermission: Boolean
    get() = context?.let {
        ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
    } == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
fun Fragment.getCurrentLocation(
    onMissingPermission: () -> Unit = {},
    onUnavailable: () -> Unit = {},
    onLocation: (Location) -> Unit,
) {
    val ctx = context ?: return
    if (!hasLocationPermission) {
        onMissingPermission()
        return
    }

    val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
    fusedClient.lastLocation
        .addOnSuccessListener { cached ->
            if (cached != null) {
                onLocation(cached)
            } else {
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { fresh -> fresh?.let(onLocation) ?: onUnavailable() }
                    .addOnFailureListener { onUnavailable() }
            }
        }
        .addOnFailureListener { onUnavailable() }
}
