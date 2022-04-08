package com.wilsofts.myambulance.ui.home

import android.location.Location
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.android.volley.VolleyError
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.wilsofts.myambulance.R
import com.wilsofts.myambulance.databinding.LayoutAcceptRejectRequestBinding
import com.wilsofts.myambulance.ui.notifications.NotificationsViewModel
import com.wilsofts.myambulance.utils.AppPrefs
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import com.wilsofts.myambulance.utils.network.VolleyRequest
import org.json.JSONObject

class RequestInfo(val activity: FragmentActivity, private val requestID: Long, private val fragmentManager: FragmentManager) {
    private val dialog: BottomSheetDialog = BottomSheetDialog(this.activity)
    private val binding = LayoutAcceptRejectRequestBinding.inflate(this.activity.layoutInflater)
    private lateinit var googleMap: GoogleMap
    private var request: NotificationsViewModel.Request? = null

    init {
        this.dialog.setContentView(this.binding.root)
        this.dialog.setCancelable(false)
        this.initMap()

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
            this.dialog.dismiss()
        }

        this.buttonDisplay("")
        this.dialog.show()
        this.getRequestInfo()
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
        dialog.showDialog(activity = this.activity)

        ApiClient.createRequest(
            call = ApiClient.getRetrofit().create(ApiService::class.java).getRequest(request_id = this.requestID),
            apiResponse = object : ApiClient.ApiResponse {
                override fun getResponse(response: JSONObject?, error: Throwable?) {
                    dialog.dismiss()
                    if (response !== null) {
                        if (response.has("code")) {
                            if (response.getInt("code") == 1) {
                                this@RequestInfo.request = Gson().fromJson(
                                    response.getJSONObject("request").toString(),
                                    NotificationsViewModel.Request::class.java
                                ).also {
                                    this@RequestInfo.binding.requestDetails.setText(it?.patient?.conditions)
                                    this@RequestInfo.binding.vehicleNo.setText(it?.request?.vehicle_no)
                                    this@RequestInfo.buttonDisplay(it?.request?.request_status ?: "")
                                    this@RequestInfo.binding.vehicleNo.isEnabled = it?.request?.request_status == "pending"
                                }

                            } else if (response.getInt("code") == 2) {
                                Utils.showToast(this@RequestInfo.activity, "Please log in again to load request information")
                            }
                        } else {
                            Utils.showToast(context = this@RequestInfo.activity, message = "Could not get request data, please retry")
                        }
                    }
                }
            }
        )
    }

    private fun getRoutes() {
        val dialog = MyProgressDialog.newInstance(title = "Fetching available routes, please wait")
        dialog.showDialog(activity = this.activity)

        (this.activity as BaseActivity).getLastLocation(
            lastLocation = object : BaseActivity.LastLocation {
                override fun locationReceived(location: Location) {
                    val url = BaseActivity.getUrl(
                        end = LatLng(this@RequestInfo.request!!.request.latitude, this@RequestInfo.request!!.request.longitude),
                        start = LatLng(location.latitude, location.longitude)
                    )
                    //  LibUtils.logE(url)
                    VolleyRequest.createRequest(url = url, volley_response = object : VolleyRequest.VolleyResponse {
                        override fun response(success: Boolean, error: VolleyError?, message: String) {
                            if (success) {
                                val routeData = JSONObject(message).getJSONArray("routes").length()
                                Log.e("Routes", routeData.toString(2))
                            }
                            dialog.dismiss()
                        }
                    })
                }
            }
        )
    }

    private fun rejectRequest() {
        fun proceedRejection() {
            val dialog = MyProgressDialog.newInstance(title = "Rejecting request, please wait")
            dialog.showDialog(activity = this.activity)

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java).rejectRequest(request_id = this.requestID),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    Utils.showToast(context = this@RequestInfo.activity, message = "Request rejected successfully")
                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(this@RequestInfo.activity, "Please log in again to reject this request")
                                }
                                AppPrefs.request_id = 0L
                                this@RequestInfo.dialog.dismiss()
                            } else {
                                Utils.showToast(context = this@RequestInfo.activity, message = "Could not reject request, please retry")
                            }
                        }
                    }
                }
            )
        }

        AlertDialog.Builder(this.activity)
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
        val vehicleNo = this.binding.vehicleNo.text.toString().trim()
        if (vehicleNo.length < 5) {
            Utils.showToast(this.activity, "Enter a valid vehicle number")
        } else {
            val dialog = MyProgressDialog.newInstance(title = "Accepting request, please wait")
            dialog.showDialog(activity = this.activity)

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java)
                    .acceptRequest(request_id = this.requestID, vehicle_no = vehicleNo),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    AppPrefs.request_id = this@RequestInfo.requestID
                                    Utils.showToast(context = this@RequestInfo.activity, message = "Request accepted successfully")
                                    this@RequestInfo.request!!.request.request_status = "driving"
                                    this@RequestInfo.buttonDisplay("driving")
                                    binding.vehicleNo.isEnabled = false
                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(this@RequestInfo.activity, "Please log in again to accept this request")
                                }
                            } else {
                                Utils.showToast(context = this@RequestInfo.activity, message = "Could not reject request, please retry")
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
            dialog.showDialog(activity = this.activity)

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java).finaliseRequest(request_id = this.requestID),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    AppPrefs.request_id = 0L
                                    Utils.showToast(context = this@RequestInfo.activity, message = "Request finalised successfully")
                                    this@RequestInfo.request!!.request.request_status = "complete"
                                    this@RequestInfo.buttonDisplay("complete")
                                    dialog.dismiss()
                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(this@RequestInfo.activity, "Please log in again to finalise this request")
                                }
                            } else {
                                Utils.showToast(context = this@RequestInfo.activity, message = "Could not reject request, please retry")
                            }
                        }
                    }
                }
            )
        }

        AlertDialog.Builder(this.activity)
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

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        (this.fragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync { googleMap ->
            googleMap.also {
                it.setMinZoomPreference(8F)
                it.mapType = GoogleMap.MAP_TYPE_NORMAL
                this.googleMap = it
            }
        }
    }
}