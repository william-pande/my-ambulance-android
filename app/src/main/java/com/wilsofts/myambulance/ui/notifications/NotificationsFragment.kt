package com.wilsofts.myambulance.ui.notifications

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
import com.wilsofts.myambulance.utils.Utils

class NotificationsFragment : Fragment() {
    private lateinit var binding: FragmentNotificationsBinding
    private lateinit var viewModel: NotificationsViewModel
    private val requests = mutableListOf<NotificationsViewModel.Request>()
    private lateinit var adapter: RequestsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        this.viewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]
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
        (this.requireActivity() as MainActivity).binding.toolbar.toolbar.visibility = View.VISIBLE
        this.viewModel.loadRequests(this.requireContext())
    }

    internal inner class RequestsAdapter : RecyclerView.Adapter<RequestsAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return this.ViewHolder(ListRequestsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val request = this@NotificationsFragment.requests[position]

            holder.binding.clientName.text = request.patient.full_name
            holder.binding.clientContact.text = request.patient.contact

            holder.binding.placeName.text = request.request.place_name
            holder.binding.placeName.setOnClickListener {
                // Create a Uri from an intent string. Use the result to create an Intent.
                //geo:
                val gmmIntentUri = Uri.parse("google.streetview:cbll=${request.request.latitude},${request.request.longitude}")
                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps")
                // Attempt to start an activity that can handle the Intent
                mapIntent.resolveActivity(this@NotificationsFragment.requireActivity().packageManager)?.let {
                    this@NotificationsFragment.startActivity(mapIntent)
                }
            }

            holder.binding.driverName.text =
                "${request.driver.driver_name}${if (request.request.vehicle_no.isNotEmpty()) " - ${request.request.vehicle_no}" else ""}"
            holder.binding.hospitalName.text = request.driver.hospital_name
            holder.binding.hospitalLocation.text = request.driver.hospital_location
            holder.binding.driverContact.text = request.driver.driver_contact

            holder.binding.dateRequested.text = request.request.date_requested
            holder.binding.requestStatus.text = request.request.request_status

            Utils.glidePath(
                context = this@NotificationsFragment.requireContext(),
                imageView = holder.binding.avatar,
                path = "${Utils.BASE_URL}/${request.driver.driver_avatar}"
            )
        }

        override fun getItemCount(): Int {
            return this@NotificationsFragment.requests.size
        }

        internal inner class ViewHolder(val binding: ListRequestsBinding) : RecyclerView.ViewHolder(binding.root)
    }
}