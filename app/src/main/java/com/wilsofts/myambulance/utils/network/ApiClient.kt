package com.wilsofts.myambulance.utils.network

import android.content.Intent
import com.google.gson.GsonBuilder
import com.localebro.okhttpprofiler.OkHttpProfilerInterceptor
import com.wilsofts.myambulance.utils.AppPrefs
import com.wilsofts.myambulance.utils.Utils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient {
    companion object {
        private fun getRetrofit(headers: Intent = Intent(), baseURl: String = Utils.BASE_URL): Retrofit {
            val builder = OkHttpClient.Builder()
                .connectTimeout(30L, TimeUnit.SECONDS)
                .writeTimeout(40L, TimeUnit.SECONDS)
                .readTimeout(60L, TimeUnit.SECONDS)
                .addInterceptor { chain: Interceptor.Chain ->
                    val builder = chain.request().newBuilder()

                    val bundle = headers.extras
                    if (bundle != null) {
                        for (name in bundle.keySet()) {
                            builder.addHeader(name, bundle.getString(name)!!)
                        }
                    }
                    chain.proceed(builder.build())
                }

            if (Utils.SHOW_LOG) {
                builder
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            this.level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                    .addInterceptor(OkHttpProfilerInterceptor())
            }

            return Retrofit.Builder()
                .baseUrl(baseURl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(builder.build())
                .build()
        }

        fun getRetrofit(baseURl: String): Retrofit {
            return getRetrofit(baseURl = baseURl, headers = Intent())
        }

        fun getRetrofit(): Retrofit {
            val userIntent = Intent()
                .putExtra("Authorization", "Bearer ${AppPrefs.bearer_token}")
                .putExtra("Content-Type", "application/json")
                .putExtra("Accept", "application/json")
            return getRetrofit(headers = userIntent)
        }

        fun createRequest(call: Call<String>, apiResponse: ApiResponse) {
            call
                .enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        Utils.logE("Success", response.body() ?: response.message())
                        if (response.isSuccessful && response.code() == 200 && response.body() != null) {
                            apiResponse.getResponse(response = JSONObject(response.body()!!), error = null)
                        } else {
                            apiResponse.getResponse(response = JSONObject().put("code", 500), error = null)
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Utils.logE("Failed", t.localizedMessage ?: "Error occurred", t)
                        apiResponse.getResponse(response = null, error = t)
                    }
                })
        }
    }

    interface ApiResponse {
        fun getResponse(response: JSONObject?, error: Throwable?)
    }
}