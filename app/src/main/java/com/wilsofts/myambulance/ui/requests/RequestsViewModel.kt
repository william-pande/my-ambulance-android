package com.wilsofts.myambulance.ui.requests

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject

class RequestsViewModel : ViewModel() {
    val loading = MutableLiveData<Boolean>()
    val requests = MutableLiveData<MutableList<Request>>()

    fun loadRequests(context: Context) {
        this.loading.postValue(true)
        ApiClient.createRequest(
            call = ApiClient.getRetrofit().create(ApiService::class.java).getRequests(),
            apiResponse = object : ApiClient.ApiResponse {
                override fun getResponse(response: JSONObject?, error: Throwable?) {
                    if (response !== null && response.getInt("code") == 1) {
                        this@RequestsViewModel.requests.postValue(Gson().fromJson(
                            response.getJSONArray("requests").toString(), Array<Request>::class.java).toMutableList()
                        )
                    } else {
                        Utils.showToast(context = context, message = "Could not load requests, please retry")
                    }
                    this@RequestsViewModel.loading.postValue(false)
                }
            }
        )
    }

    data class Request(
        val request_id: Long,
        val driver: Driver,
        val request: RequestData,
        val patient: Patient,
    )

    data class RequestData(
        val latitude: Double, val longitude: Double, val place_name: String, val vehicle_no: String, var request_status: String,
        val date_requested: String, val date_accepted: String, val date_closed: String,
    )

    data class Patient(
        val contact: String, val full_name: String, val conditions: String, val user_gender: String,
        val user_avatar: String, val date_of_birth: String, val client_id: Long
    )

    data class Driver(
        val driver_id: Long, val driver_name: String, var driver_contact: String, var driver_avatar: String,
        var hospital_name: String, val hospital_location: String,
    )
}