package com.wilsofts.myambulance.ui.admin.drivers

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.wilsofts.myambulance.databinding.LayoutAddDriverBinding
import com.wilsofts.myambulance.ui.admin.clients.ClientsFragment
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AddDriver(val activity: FragmentActivity, val client: ClientsFragment.Client) {
    private val dialog = BottomSheetDialog(this.activity)
    private val binding = LayoutAddDriverBinding.inflate(this.activity.layoutInflater)

    init {
        dialog.setContentView(this.binding.root)
        setContent()
    }

    private fun setContent() {
        this.binding.hospitalName.setText(this.client.hospital_name)
        this.binding.hospitalLocation.setText(this.client.hospital_location)

        if (this.client.user_avatar != "") {
            Utils.glidePath(context = this.activity,
                imageView = this.binding.userAvatar,
                path = "${Utils.BASE_URL}/${this.client.user_avatar}")
        }
        this.binding.userAvatar.visibility = if (this.client.user_avatar.isEmpty()) View.GONE else View.VISIBLE

        this.binding.btnAddDriver.setOnClickListener {
            saveDriver()
        }

        this.binding.closeWindow.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun saveDriver() {
        val hospitalName = this.binding.hospitalName.text.toString().trim()
        val hospitalLocation = this.binding.hospitalLocation.text.toString().trim()

        when {
            hospitalName.length < 5 -> {
                Utils.showToast(this.activity, "Enter a valid hospital name")
            }
            hospitalLocation.length < 5 -> {
                Utils.showToast(this.activity, "Enter a valid hospital location")
            }
            else -> {
                val dialog = MyProgressDialog.newInstance(title = "Saving driver, please wait")
                dialog.showDialog(activity = this.activity)

                ApiClient.createRequest(
                    call = ApiClient.getRetrofit().create(ApiService::class.java)
                        .saveDriver(
                            client_id = this.client.client_id.toString().toRequestBody("text/plan".toMediaTypeOrNull()),
                            hospital_name = hospitalName.toRequestBody("text/plan".toMediaTypeOrNull()),
                            hospital_location = hospitalLocation.toRequestBody("text/plan".toMediaTypeOrNull()),
                        ),
                    apiResponse = object : ApiClient.ApiResponse {
                        override fun getResponse(response: JSONObject?, error: Throwable?) {
                            dialog.dismiss()
                            if (response !== null) {
                                if (response.has("code")) {
                                    if (response.getInt("code") == 1) {
                                        Utils.showToast(context = activity, message = "Saved driver successfully")
                                        this@AddDriver.dialog.dismiss()
                                    } else if (response.getInt("code") == 2) {
                                        Utils.showToast(context = activity, message = "Contact is already in use")
                                    }
                                } else {
                                    Utils.showToast(context = activity, message = "Could not save driver, please retry")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}