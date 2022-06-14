package com.wilsofts.myambulance.ui.requests

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.wilsofts.myambulance.MainActivity
import com.wilsofts.myambulance.databinding.FragmentNotificationsBinding
import com.wilsofts.myambulance.databinding.ListRequestsBinding
import com.wilsofts.myambulance.ui.home.client.DriversActivity
import com.wilsofts.myambulance.utils.AppPrefs
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject

class RequestsFragment : Fragment() {
    private lateinit var binding: FragmentNotificationsBinding
    private lateinit var viewModel: RequestsViewModel
    private val requests = mutableListOf<RequestsViewModel.Request>()
    private lateinit var adapter: RequestsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        this.viewModel = ViewModelProvider(this)[RequestsViewModel::class.java]
        this.binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        this.adapter = this.RequestsAdapter()
        this.binding.listRequests.adapter = this.adapter

        this.binding.swipeRefresh.setOnRefreshListener {
            this.viewModel.loadRequests(this.requireContext())
        }

        this.viewModel.loading.observe(this.viewLifecycleOwner) {
            this.binding.swipeRefresh.isRefreshing = it
        }

        this.viewModel.requests.observe(this.viewLifecycleOwner) {
            if (it != null) {
                this.requests.clear()
                this.requests.addAll(it)
                this.adapter.notifyDataSetChanged()
                if (this.requests.isEmpty()) {
                    Utils.showToast(this.requireContext(), "List is empty")
                }
            }
            this.binding.textNoDrivers.visibility = if (this.requests.isEmpty()) View.VISIBLE else View.GONE
        }

        return this.binding.root
    }

    override fun onResume() {
        super.onResume()
        this.viewModel.loadRequests(this.requireContext())
    }

    internal inner class RequestsAdapter : RecyclerView.Adapter<RequestsAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return this.ViewHolder(ListRequestsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val request = this@RequestsFragment.requests[position]

            holder.binding.clientName.text = request.patient.full_name
            holder.binding.clientContact.text = request.patient.contact
            holder.binding.placeName.text = request.request.place_name

            if (request.driver.driver_id == 0L) {
                holder.binding.layoutDriver.visibility = View.GONE
            } else {
                holder.binding.driverName.text =
                    "${request.driver.driver_name}${if (request.request.vehicle_no.isNotEmpty()) " - ${request.request.vehicle_no}" else ""}"
                holder.binding.hospitalName.text = request.driver.hospital_name
                holder.binding.hospitalLocation.text = request.driver.hospital_location
                holder.binding.driverContact.text = request.driver.driver_contact

                Utils.glidePath(
                    context = this@RequestsFragment.requireContext(),
                    imageView = holder.binding.avatar,
                    path = "${Utils.BASE_URL}/${request.driver.driver_avatar}"
                )
                holder.binding.layoutDriver.visibility = View.VISIBLE
            }

            holder.binding.dateRequested.text = request.request.date_requested
            holder.binding.requestStatus.text = request.request.request_status.uppercase()

            holder.binding.root.setOnClickListener {
                if (request.patient.client_id == AppPrefs.client_id && request.request.request_status == "pending") {
                    getAvailableDrivers(requestID = request.request_id)

                } else if (request.driver.driver_id == AppPrefs.driver_id || AppPrefs.is_admin) {
                    // Create a Uri from an intent string. Use the result to create an Intent.
                    //geo:
                    val gmmIntentUri = Uri.parse("google.streetview:cbll=${request.request.latitude},${request.request.longitude}")
                    // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    // Make the Intent explicit by setting the Google Maps package
                    mapIntent.setPackage("com.google.android.apps.maps")
                    // Attempt to start an activity that can handle the Intent
                    mapIntent.resolveActivity(this@RequestsFragment.requireActivity().packageManager)?.let {
                        this@RequestsFragment.startActivity(mapIntent)
                    }
                }
            }
        }

        private fun getAvailableDrivers(requestID: Long) {
            val dialog = MyProgressDialog.newInstance(title = "Creating request driver, please wait")
            dialog.showDialog(activity = requireActivity())

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java).getRequestDrivers(request_id = requestID),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    val drivers = response.getJSONArray("drivers")
                                    if (drivers.length() == 0) {
                                        Utils.showToast(requireContext(), "No drivers are available to take on your request")
                                    } else {
                                        startActivity(
                                            Intent(requireContext(), DriversActivity::class.java).apply {
                                                this.putExtra("drivers", drivers.toString())
                                                this.putExtra("request_id", requestID)
                                            }
                                        )
                                    }
                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(context = requireContext(), message = "Request not found or no rights")
                                } else if (response.getInt("code") == 3) {
                                    Utils.showToast(context = requireContext(),
                                        message = "Request was already accepted, you cannot get its drivers")
                                } else if (response.getInt("code") == 4) {
                                    Utils.showToast(context = requireContext(), message = "Cannot get drivers at the moment")
                                }

                            } else {
                                Utils.showToast(context = requireContext(), message = "Could not load drivers, please retry")
                            }
                        }
                    }
                }
            )
        }

        override fun getItemCount(): Int {
            return this@RequestsFragment.requests.size
        }

        internal inner class ViewHolder(val binding: ListRequestsBinding) : RecyclerView.ViewHolder(binding.root)
    }
}