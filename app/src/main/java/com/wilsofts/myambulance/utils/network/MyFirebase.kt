package com.wilsofts.myambulance.utils.network

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wilsofts.myambulance.auth.LogInActivity
import com.wilsofts.myambulance.utils.AppPrefs
import com.wilsofts.myambulance.utils.Utils
import org.json.JSONObject

class MyFirebase : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppPrefs.fcm_token = token
        AppPrefs.updateFCMToken()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.e("App Notification", "Received Notification")
        if (remoteMessage.notification != null) {
            Utils.showNotification(
                title = remoteMessage.notification?.title ?: "Notification Title", context = this,
                content = remoteMessage.notification?.body ?: "Notification Body"
            )
        }

        if (remoteMessage.data.isNotEmpty()) {
            val params: Map<String?, String?> = remoteMessage.data
            val response = JSONObject(params)
            Utils.logE("Notification", response.toString(2))

            if (response.has("type")) {
                if (response.getString("type") == "new_request") {
                    Utils.showNotification(title = "New Request", content = "Hello, there is a new request coming in", context = this,
                        intent =
                        Intent(this, LogInActivity::class.java)
                            .apply {
                                this.putExtra("type", "new_request")
                                this.putExtra("request_id", JSONObject(response.getString("data")).getLong("request_id"))
                            }
                    )

                    LocalBroadcastManager.getInstance(this).sendBroadcast(
                        Intent("new_request").apply {
                            this.putExtra("request_id", JSONObject(response.getString("data")).getLong("request_id"))
                        }
                    )
                } else if (response.getString("type") == "request_accepted") {
                    Utils.showNotification(title = "Request Accepted", content = response.getString("body"), context = this)

                } else if (response.getString("type") == "request_complete") {
                    Utils.showNotification(title = "Request Complete", content = response.getString("body"), context = this)

                } else if (response.getString("type") == "request_rejected") {
                    Utils.showNotification(title = "Request Rejected", content = response.getString("body"), context = this)

                } else if (response.getString("type") == "admin_status") {
                    Utils.showNotification(title = "Admin Status", content = response.getString("body"), context = this)
                }
            }
        }
    }

}