package com.wilsofts.myambulance.utils.network

import com.wilsofts.myambulance.utils.AppPrefs
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    /*Users Data*/
    @Multipart
    @POST("ambulance/auth/create")
    fun createAccount(
        @Part user_avatar: MultipartBody.Part?,
        @Part("client_id") client_id: RequestBody,
        @Part("full_name") full_name: RequestBody,
        @Part("user_contact") user_contact: RequestBody,
        @Part("user_gender") user_gender: RequestBody,
        @Part("date_of_birth") date_of_birth: RequestBody,
        @Part("residential_village") residential_village: RequestBody,
        @Part("residential_district") residential_district: RequestBody,
    ): Call<String>

    @FormUrlEncoded
    @POST("ambulance/auth/login")
    fun loginUser(@Field("user_contact") user_contact: String): Call<String>

    @GET("ambulance/users/clients")
    fun getClients(): Call<String>

    @FormUrlEncoded
    @POST("ambulance/users/admin")
    fun makeAdmin(@Field("client_id") client_id: Long, @Field("is_admin") is_admin: Int): Call<String>

    /*Ambulances data*/
    @GET("ambulance/users/ambulances")
    fun getAmbulances(@Query("latitude") latitude: Double, @Query("longitude") longitude: Double): Call<String>

    @FormUrlEncoded
    @POST("ambulance/users/status")
    fun saveAmbulance(
        @Field("ambulance_no") ambulance_no: String,
        @Field("ambulance_desc") ambulance_desc: String,
        @Field("ambulance_status") ambulance_status: String,
    ): Call<String>

    /*Drivers data*/
    @GET("ambulance/users/clients")
    fun getDrivers(@Query("latitude") latitude: Double, @Query("longitude") longitude: Double): Call<String>

    @Multipart
    @POST("ambulance/users/driver")
    fun saveDriver(
        @Part("client_id") client_id: RequestBody,
        @Part("hospital_name") hospital_name: RequestBody,
        @Part("hospital_location") hospital_location: RequestBody,
    ): Call<String>

    @FormUrlEncoded
    @POST("ambulance/auth/fcm")
    fun updateFcmToken(@Field("fcm_token") fcm_token: String = AppPrefs.fcm_token): Call<String>

    @FormUrlEncoded
    @POST("ambulance/users/location")
    fun updateLocation(@Field("latitude") latitude: Double, @Field("longitude") longitude: Double): Call<String>

    @FormUrlEncoded
    @POST("ambulance/users/status")
    fun changeDriverStatus(@Field("user_status") user_status: String): Call<String>

    /*Requests Data*/
    @FormUrlEncoded
    @POST("ambulance/requests/make")
    fun makeRequest(
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Field("place_name") place_name: String,
        @Field("conditions") conditions: String,
    ): Call<String>

    @GET("ambulance/requests/get")
    fun getRequests(): Call<String>

    @GET("ambulance/requests/info")
    fun getRequest(@Query("request_id") request_id: Long): Call<String>

    @GET("ambulance/requests/drivers")
    fun getRequestDrivers(@Query("request_id") request_id: Long): Call<String>

    @FormUrlEncoded
    @POST("ambulance/requests/broadcast")
    fun broadcastRequest(@Field("request_id") request_id: Long, @Field("driver_id") driver_id: Long): Call<String>

    @FormUrlEncoded
    @POST("ambulance/requests/accept")
    fun acceptRequest(@Field("request_id") request_id: Long, @Field("vehicle_no") vehicle_no: String): Call<String>

    @FormUrlEncoded
    @POST("ambulance/requests/reject")
    fun rejectRequest(@Field("request_id") request_id: Long): Call<String>

    @FormUrlEncoded
    @POST("ambulance/requests/finalise")
    fun finaliseRequest(@Field("request_id") request_id: Long): Call<String>
}