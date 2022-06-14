package com.wilsofts.myambulance.ui.admin.ambulance

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.wilsofts.myambulance.R
import com.wilsofts.myambulance.databinding.FragmentAmbulanceBinding
import com.wilsofts.myambulance.databinding.LayoutAmbulanceBinding
import com.wilsofts.myambulance.databinding.ListAmbulancesBinding
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject

class AmbulanceFragment : Fragment() {
    private lateinit var binding: FragmentAmbulanceBinding
    private lateinit var ambulances: MutableList<Ambulance>
    private lateinit var ambulanceAdapter: AmbulanceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        this.binding = FragmentAmbulanceBinding.inflate(inflater, container, false)

        this.ambulances = mutableListOf()
        this.ambulanceAdapter = this.AmbulanceAdapter()
        this.binding.listAmbulances.adapter = this.ambulanceAdapter

        this.binding.swipeRefresh.setOnRefreshListener {
            this.loadAmbulances()
        }

        this.binding.btnAddAmbulance.setOnClickListener {
            this.showAmbulance(ambulance = Ambulance(), position = -1)
        }

        this.loadAmbulances()

        return this.binding.root
    }

    private fun loadAmbulances() {
        this.binding.swipeRefresh.isRefreshing = true

        (this.requireActivity() as BaseActivity).getLastLocation(
            lastLocation = object : BaseActivity.LastLocation {
                override fun locationReceived(location: Location) {
                    ApiClient.createRequest(
                        call = ApiClient.getRetrofit().create(ApiService::class.java)
                            .getAmbulances(latitude = location.latitude, longitude = location.longitude),
                        apiResponse = object : ApiClient.ApiResponse {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun getResponse(response: JSONObject?, error: Throwable?) {
                                if (response !== null) {
                                    if (response.has("code")) {
                                        if (response.getInt("code") == 1) {
                                            this@AmbulanceFragment.ambulances.clear()
                                            this@AmbulanceFragment.ambulances.addAll(
                                                Gson().fromJson(
                                                    response.getJSONArray("ambulances").toString(),
                                                    Array<Ambulance>::class.java
                                                ))
                                            this@AmbulanceFragment.ambulanceAdapter.notifyDataSetChanged()
                                            Utils.showToast(this@AmbulanceFragment.requireContext(),
                                                "Ambulances loaded successfully")
                                        } else if (response.getInt("code") == 2) {
                                            Utils.showToast(this@AmbulanceFragment.requireContext(),
                                                "No rights to view ambulances")
                                        } else {
                                            Utils.showToast(this@AmbulanceFragment.requireContext(),
                                                "Error loading ambulances, please retry")
                                        }
                                    } else {
                                        Utils.showToast(this@AmbulanceFragment.requireContext(),
                                            "Error loading ambulances, please retry")
                                    }
                                } else {
                                    Utils.showToast(this@AmbulanceFragment.requireContext(),
                                        "Error loading ambulances, please retry")
                                }

                                this@AmbulanceFragment.binding.swipeRefresh.isRefreshing = false
                            }
                        })
                }
            }
        )
    }

    private fun showAmbulance(ambulance: Ambulance, position: Int) {
        val dialog = BottomSheetDialog(this.requireContext())
        val binding = LayoutAmbulanceBinding.inflate(this.requireActivity().layoutInflater)

        dialog.setContentView(binding.root)
        dialog.setCancelable(false)

        /*vehicle status*/
        val adapter = ArrayAdapter(this.requireActivity(), R.layout.list_spinner_item, arrayOf("Offline", "Online"))
        (binding.vehicleStatus.editText as? AutoCompleteTextView)?.also { textView ->
            textView.setAdapter(adapter)
            textView.setText(ambulance.ambulance_status)
            textView.doAfterTextChanged {
                ambulance.ambulance_status = it.toString()
            }
        }

        /*vehicle number*/
        binding.vehicleNo.also { textView ->
            textView.isEnabled = position == -1
            textView.setText(ambulance.ambulance_no)
            textView.doAfterTextChanged {
                ambulance.ambulance_no = it.toString()
            }
        }

        /*ambulance description*/
        binding.vehicleDesc.also { textView ->
            textView.setText(ambulance.ambulance_desc)
            textView.doAfterTextChanged {
                ambulance.ambulance_desc = it.toString()
            }
        }

        binding.btnSave.setOnClickListener {
            if (ambulance.ambulance_no.trim().length < 4) {
                Utils.showToast(this.requireContext(), "Enter a valid vehicle number")
            } else {
                val progressDialog = MyProgressDialog.newInstance(title = "Saving ambulance data, please wait")
                progressDialog.showDialog(activity = this.requireActivity())

                ApiClient.createRequest(
                    call = ApiClient.getRetrofit().create(ApiService::class.java).saveAmbulance(
                        ambulance_desc = ambulance.ambulance_desc.trim(),
                        ambulance_no = ambulance.ambulance_no.trim(),
                        ambulance_status = ambulance.ambulance_status
                    ),
                    apiResponse = object : ApiClient.ApiResponse {
                        override fun getResponse(response: JSONObject?, error: Throwable?) {
                            dialog.dismiss()
                            if (response !== null) {
                                if (response.has("code")) {
                                    if (response.getInt("code") == 1) {
                                        if (position == -1) {
                                            this@AmbulanceFragment.ambulances.add(0, ambulance)
                                            this@AmbulanceFragment.ambulanceAdapter.notifyItemInserted(0)
                                        } else {
                                            this@AmbulanceFragment.ambulances[position] = ambulance
                                            this@AmbulanceFragment.ambulanceAdapter.notifyItemChanged(position)
                                        }
                                        Utils.showToast(
                                            context = this@AmbulanceFragment.requireContext(),
                                            message = "Ambulance data saved successfully"
                                        )
                                        dialog.dismiss()
                                    } else if (response.getInt("code") == 2) {
                                        Utils.showToast(
                                            context = this@AmbulanceFragment.requireContext(),
                                            message = "No right to perform task"
                                        )
                                    } else {
                                        Utils.showToast(
                                            context = this@AmbulanceFragment.requireContext(),
                                            message = "Error while saving data"
                                        )
                                    }
                                } else {
                                    Utils.showToast(
                                        context = this@AmbulanceFragment.requireContext(),
                                        message = "Error while saving data"
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        binding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    inner class AmbulanceAdapter : RecyclerView.Adapter<AmbulanceAdapter.MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return this.MyViewHolder(ListAmbulancesBinding.inflate(LayoutInflater.from(parent.context),
                parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val ambulance = this@AmbulanceFragment.ambulances[position]

            holder.binding.ambulanceNo.text = ambulance.ambulance_no
            holder.binding.ambulanceDistance.text = ambulance.distance
            holder.binding.ambulanceStatus.text = ambulance.ambulance_status

            holder.binding.ambulanceDesc.also { textView ->
                textView.text = ambulance.ambulance_desc
                textView.visibility = if (ambulance.ambulance_desc.trim().isEmpty()) View.GONE else View.VISIBLE
            }

            holder.binding.root.setOnClickListener {
                this@AmbulanceFragment.showAmbulance(ambulance = ambulance, position = position)
            }
        }

        override fun getItemCount(): Int {
            return this@AmbulanceFragment.ambulances.size
        }

        inner class MyViewHolder(val binding: ListAmbulancesBinding) : RecyclerView.ViewHolder(binding.root)
    }

    class Ambulance(
        var ambulance_no: String = "", var ambulance_desc: String = "",
        var ambulance_status: String = "Online", val distance: String = "-",
    )
}