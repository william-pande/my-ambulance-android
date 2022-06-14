package com.wilsofts.myambulance.ui.admin.drivers

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.wilsofts.myambulance.databinding.FragmentDriversBinding
import com.wilsofts.myambulance.databinding.ListDriversBinding
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject


class DriversFragment : Fragment() {
    private lateinit var binding: FragmentDriversBinding
    private val adapter = this.DriversAdapter()
    val drivers = mutableListOf<Driver>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        this.binding = FragmentDriversBinding.inflate(inflater, container, false)

        this.binding.listDrivers.adapter = this.adapter

        this.binding.swipeRefresh.setOnRefreshListener {
            loadDrivers()
        }
        loadDrivers()

        return binding.root
    }

    private fun loadDrivers() {
        if (this.binding.swipeRefresh.isRefreshing) {
            return
        }
        this.binding.swipeRefresh.isRefreshing = true

        binding.swipeRefresh.isRefreshing = true

        (requireActivity() as BaseActivity).getLastLocation(
            lastLocation = object : BaseActivity.LastLocation {
                override fun locationReceived(location: Location) {
                    ApiClient.createRequest(
                        call = ApiClient.getRetrofit().create(ApiService::class.java)
                            .getDrivers(latitude = location.latitude, longitude = location.longitude),
                        apiResponse = object : ApiClient.ApiResponse {
                            override fun getResponse(response: JSONObject?, error: Throwable?) {

                                binding.swipeRefresh.isRefreshing = false
                            }
                        })
                }
            }
        )
    }

    inner class DriversAdapter : RecyclerView.Adapter<DriversAdapter.UserListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
            return this.UserListViewHolder(ListDriversBinding.inflate(LayoutInflater.from(parent.context),
                parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
            val driver = drivers[position]

            holder.binding.driverName.text = driver.full_name
            holder.binding.driverDistance.text = "${driver.distance} KM"
            holder.binding.driverContact.text = driver.user_contact
            holder.binding.hospitalLocation.text = driver.hospital_location
            holder.binding.hospitalName.text = driver.hospital_name
            Utils.glidePath(
                context = requireContext(), imageView = holder.binding.avatar,
                path = "${Utils.BASE_URL}content/avatars/${driver.user_avatar}"
            )

            holder.binding.root.setOnClickListener {

            }
        }


        override fun getItemCount(): Int {
            return drivers.size
        }

        inner class UserListViewHolder(val binding: ListDriversBinding) : RecyclerView.ViewHolder(binding.root)
    }

    class Driver(
        val driver_id: Long,
        val full_name: String,
        val user_contact: String,
        val hospital_name: String,
        val hospital_location: String,
        val user_avatar: String,
        val distance: Double,
    )
}