package com.example.eli.placebook

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.location.places.Places

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupLocationClient()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        getCurrentLocation()
        setupGoogleClient()
        mMap.setOnPoiClickListener {
            displayPOI(it)
        }
    }

    private fun setupGoogleClient() {
        googleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .addApi(Places.GEO_DATA_API)
            .build()
    }

    private fun displayPOI(pointOfInterest: PointOfInterest) {
        Places.GeoDataApi
            .getPlaceById(googleApiClient, pointOfInterest.placeId)
            .setResultCallback { places ->
                if (places.status.isSuccess) {
                    val place = places.get(0)
                    Toast.makeText(this, "{$place.name} {${place.phoneNumber}}", Toast.LENGTH_LONG).show()
                } else {
                    Log.e(TAG, "Error with getPlaceById ${places.status}")
                }

                places.release()
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "Google play connection failed: {${connectionResult.errorMessage}}")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION) {
          if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
              getCurrentLocation()
          } else {
              Log.e(TAG, "Location permissons denied")
          }
        }
    }

    private fun setupLocationClient() {
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
        } else {
            mMap.isMyLocationEnabled = true
            fusedLocationProvider.lastLocation.addOnCompleteListener {
                if (it.result != null) {
                    val latLng = LatLng(it.result?.latitude ?: 0.0, it.result?.longitude ?: 0.0)
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    mMap.moveCamera(update)
                } else {
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }
}
