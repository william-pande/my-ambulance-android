package com.wilsofts.myambulance.utils

import android.app.Application
import android.content.ContextWrapper
import android.text.TextUtils
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.pixplicity.easyprefs.library.Prefs

class AppManager : Application() {
    private val TAG = AppManager::class.java.simpleName
    private var requestQueue: RequestQueue? = null

    override fun onCreate() {
        super.onCreate()
        controller = this

        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName("my_ambulance")
            .setUseDefaultSharedPreference(true)
            .build()
    }

    private fun getRequestQueue(): RequestQueue? {
        if (this.requestQueue == null) {
            this.requestQueue = Volley.newRequestQueue(this.applicationContext)
        }
        return this.requestQueue
    }

    fun <T> addToRequestQueue(req: Request<T>, tag: String?) { // set the default tag if tag is empty
        req.tag = if (TextUtils.isEmpty(tag)) this.TAG else tag
        this.getRequestQueue()?.add(req)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        req.tag = this.TAG
        this.getRequestQueue()?.add(req)
    }

    fun cancelPendingRequests(tag: Any?) {
        this.requestQueue?.cancelAll(tag)
    }

    companion object {
        lateinit var controller: AppManager
    }
}