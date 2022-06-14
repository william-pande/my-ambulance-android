package com.wilsofts.myambulance.ui.home

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.wilsofts.myambulance.MainActivity
import com.wilsofts.myambulance.R
import com.wilsofts.myambulance.databinding.FragmentHomeBinding
import com.wilsofts.myambulance.ui.home.client.CreateRequest
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.Utils

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var googleMap: GoogleMap
    private var clientMarker: Marker? = null
    private var geocodedAddress: Utils.GeocodedAddress? = null

    @SuppressLint("MissingPermission")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        this.binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        (this.childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync { googleMap ->
            googleMap.also {
                it.setMinZoomPreference(8F)
                it.uiSettings.isZoomControlsEnabled = true

                it.isMyLocationEnabled = true
                it.uiSettings.isMyLocationButtonEnabled = true
                it.setOnMyLocationButtonClickListener {
                    (requireActivity() as MainActivity).getLastLocation(
                        lastLocation = object : BaseActivity.LastLocation {
                            override fun locationReceived(location: Location) {
                                updateClientMarker(latitude = location.latitude, longitude = location.longitude)
                            }
                        }
                    )
                    true
                }
                it.setOnMyLocationClickListener {

                }
                it.mapType = GoogleMap.MAP_TYPE_NORMAL

                it.setOnMapClickListener { latLng ->
                    this.updateClientMarker(latitude = latLng.latitude, longitude = latLng.longitude)
                }

                (requireActivity() as MainActivity).getLastLocation(
                    lastLocation = object : BaseActivity.LastLocation {
                        override fun locationReceived(location: Location) {
                            updateClientMarker(latitude = location.latitude, longitude = location.longitude)
                        }
                    }
                )
                this.googleMap = it
            }
        }

        this.initListeners()
        return this.binding.root
    }

    private fun initListeners() {
        this.binding.makeRequest.setOnClickListener {
            if (this.geocodedAddress != null) {
                CreateRequest(address = this.geocodedAddress!!, activity = this.requireActivity())
            } else {
                Utils.showToast(context = this.requireContext(), message = "First select your location to create a request")
            }
        }

        if (!Places.isInitialized()) {
            Places.initialize(this.requireContext(), this.getString(R.string.maps_key))
        }
        Places.createClient(this.requireContext())
        (this.childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment).apply {
            this.setPlaceFields(listOf(Place.Field.LAT_LNG, Place.Field.NAME))
            this.setHint("Search for your location on map")
            this.setCountry("UG")
            this.setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    this@HomeFragment.updateClientMarker(
                        latitude = place.latLng!!.latitude,
                        longitude = place.latLng!!.longitude,
                        placeName = place.name ?: null
                    )
                }

                override fun onError(status: Status) {
                    Log.e("Error", "An error occurred: $status")
                    Utils.showToast(this@HomeFragment.requireContext(), "Could not load place data")
                }
            })
        }
    }

    private fun updateClientMarker(latitude: Double, longitude: Double, placeName: String? = null) {
        if (placeName == null) {
            Thread {
                this.geocodedAddress =
                    BaseActivity.getLocationDetails(latitude = latitude, longitude = longitude, context = this.requireContext())
            }.start()
        } else {
            this.geocodedAddress = Utils.GeocodedAddress(latitude = latitude, longitude = longitude, placeName = placeName)
        }

        val latLng = LatLng(latitude, longitude)
        val cameraPosition = CameraPosition.Builder().target(latLng)
        if (this.clientMarker == null) {
            this.clientMarker = this.googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("").snippet("")
                    .draggable(true)
            )
            cameraPosition.zoom(18f)
        } else {
            this.clientMarker?.position = latLng
            cameraPosition.zoom(this.googleMap.cameraPosition.zoom)
        }

        this.googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition.build()))
    }

    private fun updateDriverMarker(location: Location) {

    }
}