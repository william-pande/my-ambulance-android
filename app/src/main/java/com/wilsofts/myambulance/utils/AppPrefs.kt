package com.wilsofts.myambulance.utils

import com.pixplicity.easyprefs.library.Prefs
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object AppPrefs {
    fun logInUser(response: JSONObject) {
        bearer_token = response.getString("bearer_token")
        user_contact = response.getString("user_contact")
        driver_status = response.getString("driver_status")
        full_name = response.getString("full_name")
        client_id = response.getLong("client_id")
        driver_id = response.getLong("driver_id")
        is_admin = if (response.has("is_admin")) response.getBoolean("is_admin") else false
    }

    var driver_status: String
        get() = Prefs.getString("driver_status", "Online")
        set(value) = Prefs.putString("driver_status", value)

    var bearer_token: String
        get() = Prefs.getString("bearer_token", "")
        set(value) = Prefs.putString("bearer_token", value)

    var is_admin: Boolean
        get() = Prefs.getBoolean("is_admin", false)
        set(value) = Prefs.putBoolean("is_admin", value)

    var user_contact: String
        get() = Prefs.getString("user_contact", "")
        set(value) = Prefs.putString("user_contact", value)

    var full_name: String
        get() = Prefs.getString("full_name", "")
        set(value) = Prefs.putString("full_name", value)

    var client_id: Long
        get() = Prefs.getLong("client_id", 0L)
        set(value) = Prefs.putLong("client_id", value)

    var driver_id: Long
        get() = Prefs.getLong("driver_id", 0L)
        set(value) = Prefs.putLong("driver_id", value)

    var notification_id: Int
        get() = Prefs.getInt("notification_id", 0)
        set(value) = Prefs.putInt("notification_id", value)

    var fcm_token: String
        get() = Prefs.getString("fcm_token", "")
        set(value) = Prefs.putString("fcm_token", value)

    var fcm_updated: Boolean
        get() = Prefs.getBoolean("fcm_updated", false)
        set(value) = Prefs.putBoolean("fcm_updated", value)

    fun logoutUser() {
        val fcmToken = this.fcm_token
        Prefs.clear()
        this.fcm_token = fcmToken
    }

    fun updateFCMToken() {
        if (fcm_token.isNotEmpty() && !fcm_updated && bearer_token.isNotEmpty()) {
            ApiClient.getRetrofit()
                .create(ApiService::class.java)
                .updateFcmToken()
                .enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        if (response.code() == 200) {
                            fcm_updated = true
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {

                    }
                })
        }
    }
}