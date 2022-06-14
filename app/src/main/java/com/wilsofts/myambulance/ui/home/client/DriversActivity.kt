package com.wilsofts.myambulance.ui.home.client

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.wilsofts.myambulance.databinding.ActivityDriversBinding
import com.wilsofts.myambulance.databinding.ListDriversBinding
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import kotlinx.parcelize.Parcelize
import org.json.JSONObject


class DriversActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDriversBinding

    private val adapter = this.DriversAdapter()
    val drivers = mutableListOf<AvailableDriver>()
    var requestID: Long = 0L

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityDriversBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)
        this.setSupportActionBar(this.binding.toolbar.toolbar)

        this.requestID = this.intent.getLongExtra("request_id", 0L)
        this.binding.listDrivers.adapter = this.adapter
        this.drivers.addAll(Gson().fromJson(this.intent.getStringExtra("drivers"),
            Array<AvailableDriver>::class.java).toCollection(mutableListOf()))

        this.binding.swipeRefresh.setOnRefreshListener {
            this.binding.swipeRefresh.isRefreshing = false
        }

        this.adapter.notifyDataSetChanged()
    }

    inner class DriversAdapter : RecyclerView.Adapter<DriversAdapter.UserListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
            return this.UserListViewHolder(ListDriversBinding.inflate(LayoutInflater.from(parent.context),
                parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
            val driver = this@DriversActivity.drivers[position]

            holder.binding.driverName.text = driver.full_name
            holder.binding.driverDistance.text = "${driver.distance} KM"
            holder.binding.driverContact.text = driver.user_contact
            holder.binding.hospitalLocation.text = driver.hospital_location
            holder.binding.hospitalName.text = driver.hospital_name
            Utils.glidePath(
                context = this@DriversActivity, imageView = holder.binding.avatar,
                path = "${Utils.BASE_URL}content/avatars/${driver.user_avatar}"
            )

            holder.binding.root.setOnClickListener {
                this.broadcastRequest(driver_id = driver.driver_id, driver_name = driver.full_name)
            }
        }

        private fun broadcastRequest(driver_name: String, driver_id: Long) {
            fun proceedBroadcast() {
                val dialog = MyProgressDialog.newInstance(title = "Sending request to driver, please wait")
                dialog.showDialog(activity = this@DriversActivity)

                ApiClient.createRequest(
                    call = ApiClient.getRetrofit().create(ApiService::class.java)
                        .broadcastRequest(driver_id = driver_id, request_id = this@DriversActivity.requestID),
                    apiResponse = object : ApiClient.ApiResponse {
                        override fun getResponse(response: JSONObject?, error: Throwable?) {
                            dialog.dismiss()
                            if (response !== null) {
                                if (response.has("code")) {
                                    if (response.getInt("code") == 1) {
                                        AlertDialog.Builder(this@DriversActivity)
                                            .setMessage("Driver has been notified of your request, in case of no response please try other available drivers")
                                            .setTitle("Request Broadcast")
                                            .setPositiveButton("Ok") { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                            .create()
                                            .show()
                                    }else if (response.getInt("code") == 2) {
                                        Utils.showToast(this@DriversActivity, "Driver is not available to handle your request")
                                    }else{
                                        Utils.showToast(this@DriversActivity, "Could not broadcast to driver, please retry")
                                    }
                                }
                            } else {
                                Utils.showToast(this@DriversActivity, "Could not broadcast to driver, please retry")
                            }
                        }
                    }
                )
            }

            AlertDialog.Builder(this@DriversActivity)
                .setMessage("Are you sure you want to proceed broadcasting request to \"$driver_name\"")
                .setTitle("Broadcast Request")
                .setPositiveButton("Proceed") { dialog, _ ->
                    dialog.dismiss()
                    proceedBroadcast()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        override fun getItemCount(): Int {
            return this@DriversActivity.drivers.size
        }

        inner class UserListViewHolder(val binding: ListDriversBinding) : RecyclerView.ViewHolder(binding.root)
    }

    @Parcelize
    class AvailableDriver(
        val driver_id: Long,
        val full_name: String,
        val user_contact: String,
        val hospital_name: String,
        val hospital_location: String,
        val user_avatar: String,
        val distance: Double,
    ) : Parcelable
}