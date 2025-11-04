package com.example.map_week_07

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // TENTUKAN TIPENYA, jangan bikin compiler nebak
    private val fused: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    try {
                        mMap.isMyLocationEnabled = true
                    } catch (se: SecurityException) {
                        Log.e("MapsActivity", "Enable myLocation failed", se)
                    }
                    getLastLocation()
                } else {
                    showPermissionRationale {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        when {
            hasLocationPermission() -> {
                try {
                    mMap.isMyLocationEnabled = true
                } catch (se: SecurityException) {
                    Log.e("MapsActivity", "Enable myLocation failed", se)
                }
                getLastLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionRationale {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // biar nggak blank saat awal
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 1f))
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (!hasLocationPermission()) return
        try {
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        val userLocation = LatLng(loc.latitude, loc.longitude)
                        updateMapLocation(userLocation)
                        addMarkerAtLocation(userLocation, "You")
                    } else {
                        requestSingleLocationUpdate()
                    }
                }
                .addOnFailureListener {
                    requestSingleLocationUpdate()
                }
        } catch (se: SecurityException) {
            Log.e("MapsActivity", "lastLocation SecurityException", se)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleLocationUpdate() {
        if (!hasLocationPermission()) return

        val request: LocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).setMaxUpdates(1).build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val here = LatLng(loc.latitude, loc.longitude)
                updateMapLocation(here)
                addMarkerAtLocation(here, "You")

                try {
                    fused.removeLocationUpdates(this)
                } catch (se: SecurityException) {
                    Log.w("MapsActivity", "removeLocationUpdates failed", se)
                }
                locationCallback = null
            }
        }

        locationCallback = cb
        try {
            fused.requestLocationUpdates(request, cb, mainLooper)
        } catch (se: SecurityException) {
            Log.e("MapsActivity", "requestLocationUpdates SecurityException", se)
        }
    }

    private fun updateMapLocation(location: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
    }

    private fun addMarkerAtLocation(location: LatLng, title: String) {
        mMap.addMarker(
            MarkerOptions()
                .title(title)
                .position(location)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationCallback?.let { fused.removeLocationUpdates(it) }
        } catch (se: SecurityException) {
            Log.w("MapsActivity", "removeLocationUpdates onDestroy failed", se)
        }
        locationCallback = null
    }
}
