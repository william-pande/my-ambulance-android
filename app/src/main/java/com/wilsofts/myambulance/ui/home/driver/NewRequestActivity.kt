package com.wilsofts.myambulance.ui.home.driver

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.VolleyError
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.wilsofts.myambulance.R
import com.wilsofts.myambulance.databinding.ActivityNewRequestBinding
import com.wilsofts.myambulance.ui.requests.RequestsViewModel
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import com.wilsofts.myambulance.utils.network.PointsParser
import com.wilsofts.myambulance.utils.network.VolleyRequest
import org.json.JSONObject

class NewRequestActivity : BaseActivity() {
    private lateinit var binding: ActivityNewRequestBinding

    private lateinit var googleMap: GoogleMap
    private var request: RequestsViewModel.Request? = null
    private var polyLine: Polyline? = null
    private var requestID: Long = 0

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityNewRequestBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)

        this.requestID = this.intent!!.getLongExtra("request_id", 0L)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        (this.supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?).also {
            it?.getMapAsync { map ->
                map.isMyLocationEnabled = false
                map.setMinZoomPreference(8f)
                map.setMaxZoomPreference(20f)
                map.isIndoorEnabled = true
                map.uiSettings.isZoomControlsEnabled = true

                this.googleMap = map
            }
        }

        this.binding.btnRejectRequest.setOnClickListener {
            this.rejectRequest()
        }

        this.binding.btnAcceptRequest.setOnClickListener {
            this.acceptRequest()
        }

        this.binding.btnCloseRequest.setOnClickListener {
            this.finaliseRequest()
        }

        this.binding.btnGetInfo.setOnClickListener {
            this.getRequestInfo()
        }

        this.binding.btnCloseWindow.setOnClickListener {
            this.finish()
        }

        this.buttonDisplay("")
        this.getRequestInfo()
    }

    override fun locationInitialised(status: Boolean) {
        if (!status) {
            Toast.makeText(this, "Location permissions denied, application is exiting", Toast.LENGTH_LONG).show()
            this.finish()
        }
    }

    private fun buttonDisplay(status: String) {
        this.binding.btnCloseWindow.visibility = View.GONE
        this.binding.btnCloseRequest.visibility = View.GONE
        this.binding.btnRejectRequest.visibility = View.GONE
        this.binding.btnAcceptRequest.visibility = View.GONE

        when (status) {
            "", "complete" -> {
                this.binding.btnCloseWindow.visibility = View.VISIBLE
            }
            "pending" -> {
                this.binding.btnRejectRequest.visibility = View.VISIBLE
                this.binding.btnAcceptRequest.visibility = View.VISIBLE
            }
            "driving" -> {
                this.binding.btnCloseRequest.visibility = View.VISIBLE
            }
        }
    }

    private fun getRequestInfo() {
        val dialog = MyProgressDialog.newInstance(title = "Getting request data, please wait")
        dialog.showDialog(activity = this)

        ApiClient.createRequest(
            call = ApiClient.getRetrofit().create(ApiService::class.java).getRequest(request_id = this.requestID),
            apiResponse = object : ApiClient.ApiResponse {
                override fun getResponse(response: JSONObject?, error: Throwable?) {
                    if (response !== null) {
                        if (response.has("code")) {
                            if (response.getInt("code") == 1) {
                                this@NewRequestActivity.request = Gson().fromJson(
                                    response.getJSONObject("request").toString(), RequestsViewModel.Request::class.java
                                ).also {
                                    this@NewRequestActivity.binding.requestDetails.setText(it?.patient?.conditions)
                                    this@NewRequestActivity.binding.vehicleNo.setText(it?.request?.vehicle_no)
                                    this@NewRequestActivity.buttonDisplay(it?.request?.request_status ?: "")


                                    if (it?.request?.request_status == "pending") {
                                        this@NewRequestActivity.binding.vehicleSpinner.visibility = View.VISIBLE
                                        val items = Gson().fromJson(response.getJSONArray("ambulances").toString(),
                                            Array<String>::class.java
                                        )
                                        val adapter = ArrayAdapter(this@NewRequestActivity, R.layout.list_spinner_item, items)
                                        (binding.vehicleSpinner.editText as? AutoCompleteTextView)?.setAdapter(adapter)

                                    } else {
                                        this@NewRequestActivity.binding.vehicleNo.visibility = View.VISIBLE
                                    }

                                    this@NewRequestActivity.getRoutes(dialog = dialog)
                                }

                            } else if (response.getInt("code") == 2) {
                                dialog.dismiss()
                                Utils.showToast(context = this@NewRequestActivity,
                                    message = "Please log in again to load request information")
                            } else {
                                dialog.dismiss()
                                Utils.showToast(context = this@NewRequestActivity,
                                    message = "Could not load request data, please retry")
                            }
                        } else {
                            dialog.dismiss()
                            Utils.showToast(context = this@NewRequestActivity,
                                message = "Could not get request data, please retry")
                        }
                    }
                }
            }
        )
    }

    private fun getRoutes(dialog: MyProgressDialog) {

        this.getLastLocation(
            lastLocation = object : LastLocation {
                override fun locationReceived(location: Location) {
                    val departure = LatLng(location.latitude, location.longitude)
                    val destination =
                        LatLng(this@NewRequestActivity.request!!.request.latitude, this@NewRequestActivity.request!!.request.longitude)
                    val url = getUrl(end = destination, start = departure)

                    VolleyRequest.createRequest(url = url, volley_response = object : VolleyRequest.VolleyResponse {
                        override fun response(success: Boolean, error: VolleyError?, message: String) {
                            if (success) {
                                val routeData = JSONObject(message).getJSONArray("routes")
                                if (routeData.length() > 0) {
                                    this@NewRequestActivity.displayMapRoutes(departure = departure, destination = destination,
                                        path = routeData.getJSONObject(0).toString())
                                } else {
                                    Utils.showToast(context = this@NewRequestActivity, message = "Unable to parse direction")
                                }
                            } else {
                                Utils.showToast(context = this@NewRequestActivity, message = "Unable to parse direction")
                            }
                            dialog.dismiss()
                        }
                    })
                }
            }
        )
    }

    fun displayMapRoutes(departure: LatLng, destination: LatLng, path: String) {
        var addedPolyline = this.polyLine

        PointsParser(
            activity = this,
            parseFinished = object : PointsParser.ParseFinished {
                override fun onTaskDone(line_options: PolylineOptions, distance: Double, duration: Int) {
                    this@NewRequestActivity.polyLine?.remove()
                    addedPolyline = this@NewRequestActivity.googleMap.addPolyline(line_options)
                }
            }
        ).parseRoutes(JSONObject(path))

        this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(departure, 13f))
        val cameraPosition = CameraPosition.Builder()
            .target(departure)
            .zoom(15f)
            .build()
        this.googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        this.googleMap.addMarker(
            MarkerOptions().apply {
                this.position(departure)
                this.icon(Utils.generateBitmapDescriptorFromRes(this@NewRequestActivity, R.drawable.icon_my_location))
            }
        )

        this.googleMap.addMarker(
            MarkerOptions().apply {
                this.position(destination)
                this.icon(Utils.generateBitmapDescriptorFromRes(this@NewRequestActivity, R.drawable.icon_pin_drop))
            }
        )

        this.polyLine = addedPolyline
    }

    private fun rejectRequest() {
        fun proceedRejection() {
            val dialog = MyProgressDialog.newInstance(title = "Rejecting request, please wait")
            dialog.showDialog(activity = this)

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java).rejectRequest(request_id = this.requestID),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    Utils.showToast(context = this@NewRequestActivity, message = "Request rejected successfully")
                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(
                                        context = this@NewRequestActivity,
                                        message = "Please log in again to reject this request"
                                    )
                                }
                                dialog.dismiss()
                            } else {
                                Utils.showToast(
                                    context = this@NewRequestActivity,
                                    message = "Could not reject request, please retry"
                                )
                            }
                        }
                    }
                }
            )
        }

        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to continue rejecting this request?")
            .setTitle("Reject Request")
            .setPositiveButton("Proceed") { dialog, _ ->
                dialog.dismiss()
                proceedRejection()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun acceptRequest() {
        val vehicleNo = (binding.vehicleSpinner.editText as? AutoCompleteTextView)?.text.toString().trim()
        if (vehicleNo.isEmpty()) {
            Utils.showToast(this, "Select a vehicle number")
        } else {
            val dialog = MyProgressDialog.newInstance(title = "Accepting request, please wait")
            dialog.showDialog(activity = this)

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java)
                    .acceptRequest(request_id = this.requestID, vehicle_no = vehicleNo),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    Utils.showToast(context = this@NewRequestActivity, message = "Request accepted successfully")
                                    this@NewRequestActivity.request!!.request.request_status = "driving"
                                    this@NewRequestActivity.buttonDisplay("driving")

                                    this@NewRequestActivity.binding.vehicleNo.visibility = View.VISIBLE
                                    this@NewRequestActivity.binding.vehicleNo.setText(vehicleNo)
                                    this@NewRequestActivity.binding.vehicleSpinner.visibility = View.GONE

                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(
                                        context = this@NewRequestActivity,
                                        message = "Please log in again to accept this request"
                                    )
                                }
                            } else {
                                Utils.showToast(
                                    context = this@NewRequestActivity,
                                    message = "Could not reject request, please retry"
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    private fun finaliseRequest() {
        fun proceedFinalising() {
            val dialog = MyProgressDialog.newInstance(title = "Finalising request, please wait")
            dialog.showDialog(activity = this)

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java).finaliseRequest(request_id = this.requestID),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    Utils.showToast(context = this@NewRequestActivity, message = "Request finalised successfully")
                                    this@NewRequestActivity.request!!.request.request_status = "complete"
                                    this@NewRequestActivity.buttonDisplay("complete")
                                    dialog.dismiss()
                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(
                                        context = this@NewRequestActivity,
                                        message = "Please log in again to finalise this request"
                                    )
                                }
                            } else {
                                Utils.showToast(
                                    context = this@NewRequestActivity,
                                    message = "Could not reject request, please retry"
                                )
                            }
                        }
                    }
                }
            )
        }

        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to continue finalising this request?. Please make sure that the patient has been fully worked upon")
            .setTitle("Finalise Request")
            .setPositiveButton("Proceed") { dialog, _ ->
                dialog.dismiss()
                proceedFinalising()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}