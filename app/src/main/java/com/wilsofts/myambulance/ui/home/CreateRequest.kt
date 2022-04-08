package com.wilsofts.myambulance.ui.home

import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wilsofts.myambulance.databinding.LayoutCreateRequestBinding
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject

class CreateRequest(private val activity: FragmentActivity, private val address: Utils.GeocodedAddress) {
    private val dialog: BottomSheetDialog = BottomSheetDialog(this.activity)
    private val binding = LayoutCreateRequestBinding.inflate(this.activity.layoutInflater)

    init {
        this.dialog.setContentView(this.binding.root)
        this.dialog.setCancelable(false)

        this.binding.btnCreateRequest.setOnClickListener {
            val requestDetails = this.binding.requestDetails.text.toString().trim()

            when {
                requestDetails.length < 10 -> {
                    Utils.showToast(context = this.activity, message = "Describe the patient condition with at least 10 characters")
                }
                else -> {
                    this.createRequest(requestDetails = requestDetails)
                }
            }
        }

        this.binding.closeWindow.setOnClickListener {
            this.dialog.dismiss()
        }

        this.dialog.show()
    }

    private fun createRequest(requestDetails: String) {
        val dialog = MyProgressDialog.newInstance(title = "Creating request driver, please wait")
        dialog.showDialog(activity = this.activity)

        this.activity.runOnUiThread {
            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java).makeRequest(
                    latitude = this.address.latitude, longitude = this.address.longitude,
                    place_name = this.address.placeName, conditions = requestDetails,
                ),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code") && response.getInt("code") == 1) {
                                Utils.showToast(context = this@CreateRequest.activity, message = "Your request has been sent successfully")
                                dialog.dismiss()
                            } else {
                                Utils.showToast(context = this@CreateRequest.activity, message = "Could not save request, please retry")
                            }
                        }
                    }
                }
            )
        }
    }
}