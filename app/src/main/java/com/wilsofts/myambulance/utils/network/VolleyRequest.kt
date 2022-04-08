package com.wilsofts.myambulance.utils.network

import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.wilsofts.myambulance.utils.AppManager

object VolleyRequest {
    fun  createRequest(parameters: HashMap<String, String> = HashMap(), url: String, volley_response: VolleyResponse) {
        val request: StringRequest = object : StringRequest(Method.POST, url,
                Response.Listener {
                    volley_response.response(success = true, error = null, message = it)
                },
                Response.ErrorListener {
                    volley_response.response(success = false, error = it, message = "")
                }) {
            override fun getParams(): Map<String, String> {
                return parameters
            }
        }

        AppManager.controller.addToRequestQueue(request)
        request.retryPolicy = DefaultRetryPolicy(10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
    }

    interface VolleyResponse {
        fun response(success: Boolean, error: VolleyError?, message: String)
    }
}