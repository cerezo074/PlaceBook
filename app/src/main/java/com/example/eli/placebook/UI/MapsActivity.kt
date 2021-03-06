package com.example.eli.placebook.UI

import android.Manifest
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.example.eli.placebook.Adapter.BookmarkInfoWindowAdapter
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import android.view.View
import android.widget.ProgressBar
import com.example.eli.placebook.R
import com.example.eli.placebook.ViewModel.MapsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var loader: ProgressBar
    private lateinit var mapsViewModel: MapsViewModel
    private var enableTouch: Boolean = true

    private val placeFields = Arrays.asList(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.PHONE_NUMBER,
        Place.Field.PHOTO_METADATAS,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        loader = findViewById(R.id.loader)
        loader.visibility = View.GONE
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupLocationClient()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        getCurrentLocation()
        setupPlacesClient()
        setupMapListeners()
        setViewModel()
    }

    private fun setupMapListeners() {
        mMap.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        mMap.setOnPoiClickListener {
            displayPOI(it)
        }

        mMap.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
    }

    private fun setViewModel() {
        mapsViewModel = ViewModelProviders.of(this).get(MapsViewModel::class.java)
    }

    private fun setupPlacesClient() {
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
    }

    private fun displayPOI(pointOfInterest: PointOfInterest) {
        if (!enableTouch) {
            return
        }

        loader.visibility = View.VISIBLE
        enableTouch = false
        diplayPoiGetPlaceStep(pointOfInterest)
    }

    private fun diplayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val request = FetchPlaceRequest.builder(pointOfInterest.placeId, placeFields).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener {
                displayPoiGetPhotoStep(it.place)
            }
            .addOnFailureListener {
                if (it is ApiException) {
                    val statusCode = it.statusCode
                    Log.e(TAG, "Place not found: ${it.message}, statusCode:  ${statusCode}")
                }
            }
    }

    private fun displayPoiGetPhotoStep(place: Place) {
        val photoMetadata = place.photoMetadatas?.get(0) ?: return

        val photoRequest = FetchPhotoRequest.builder(photoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(R.dimen.default_image_width))
            .setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height))
            .build()

        placesClient.fetchPhoto(photoRequest).addOnSuccessListener { fetchPhotoResponse ->
            val bitmap = fetchPhotoResponse.bitmap
            displayPoiDisplayStep(place, bitmap)
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                val statusCode = exception.statusCode
                Log.e(TAG, "Place not found: ${exception.message}, statusCode: ${statusCode}")
            }

            displayPoiDisplayStep(place, null)
        }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        val marketOptions = MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)

        val marker = mMap.addMarker(marketOptions)
        marker.tag = PlaceInfo(place, photo)
        loader.visibility = View.GONE
        enableTouch = true
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
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION
        )
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

    private fun handleInfoWindowClick(marker: Marker) {
        val placeInfo =  marker.tag as PlaceInfo

        if (placeInfo.place != null) {
            GlobalScope.launch {
                mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
            }
        }

        marker.remove()
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }

    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)

}
