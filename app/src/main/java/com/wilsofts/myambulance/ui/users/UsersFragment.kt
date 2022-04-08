package com.wilsofts.myambulance.ui.users

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.wilsofts.myambulance.databinding.FragmentUsersBinding
import com.wilsofts.myambulance.databinding.ListUsersBinding
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject

class UsersFragment : Fragment() {
    private lateinit var binding: FragmentUsersBinding
    private val clients = mutableListOf<Client>()
    private lateinit var adapter: UsersAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        this.binding = FragmentUsersBinding.inflate(inflater, container, false)

        this.adapter = this.UsersAdapter()
        this.binding.listDrivers.adapter = this.adapter

        this.binding.swipeRefresh.setOnRefreshListener {
            this.loadDrivers()
        }

        this.loadDrivers()

        return this.binding.root
    }

    private fun loadDrivers() {
        this.binding.swipeRefresh.isRefreshing = true
        ApiClient.createRequest(
            call = ApiClient.getRetrofit().create(ApiService::class.java).getClients(),
            apiResponse = object : ApiClient.ApiResponse {
                @SuppressLint("NotifyDataSetChanged")
                override fun getResponse(response: JSONObject?, error: Throwable?) {
                    if (response !== null && response.getInt("code") == 1) {
                        this@UsersFragment.clients.clear()
                        this@UsersFragment.clients.addAll(Gson().fromJson(response.getJSONArray("clients").toString(),
                            Array<Client>::class.java))
                        this@UsersFragment.adapter.notifyDataSetChanged()
                        this@UsersFragment.binding.textNoDrivers.visibility =
                            if (this@UsersFragment.clients.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Utils.showToast(context = this@UsersFragment.requireContext(), message = "Could not load clients, please retry")
                    }
                    this@UsersFragment.binding.swipeRefresh.isRefreshing = false
                }
            }
        )
    }

    internal inner class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UserListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListViewHolder {
            return this.UserListViewHolder(ListUsersBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
            val client = this@UsersFragment.clients[position]

            holder.binding.driverName.text = client.full_name
            holder.binding.driverContact.text = client.user_contact
            holder.binding.districtVillage.text = "${client.residential_district} - ${client.residential_village}"
            holder.binding.genderDriver.text = client.user_gender + if (client.hospital_name.isNotEmpty()) " (Driver)" else ""
            Utils.glidePath(
                context = this@UsersFragment.requireContext(), imageView = holder.binding.avatar,
                path = "${Utils.BASE_URL}/${client.user_avatar}"
            )
            holder.binding.btnDriverAdd.setOnClickListener {
                AddDriver(activity = this@UsersFragment.requireActivity(), client = client)
            }

            holder.binding.btnAdmin.isChecked = client.is_admin == "1"
            holder.binding.btnDriverAdd.setOnClickListener {
                this.changeAdmin(is_admin = if (client.is_admin == "1") "0" else "1", client_id = client.client_id, position = position)
            }
        }

        private fun changeAdmin(is_admin: String, client_id: Long, position: Int) {
            fun proceedRejection() {
                val dialog = MyProgressDialog.newInstance(title = "Modifying admin status, please wait")
                dialog.showDialog(activity = this@UsersFragment.requireActivity())

                ApiClient.createRequest(
                    call = ApiClient.getRetrofit().create(ApiService::class.java)
                        .makeAdmin(client_id = client_id, is_admin = is_admin.toInt()),
                    apiResponse = object : ApiClient.ApiResponse {
                        override fun getResponse(response: JSONObject?, error: Throwable?) {
                            dialog.dismiss()
                            if (response !== null) {
                                if (response.has("code") && response.getInt("code") == 1) {
                                    Utils.showToast(context = this@UsersFragment.requireContext(),
                                        message = "Admin status changed successfully")
                                    this@UsersFragment.clients[position].is_admin = is_admin
                                    this@UsersFragment.adapter.notifyItemChanged(position)
                                } else {
                                    Utils.showToast(context = this@UsersFragment.requireContext(),
                                        message = "Admin status not changed, please retry")
                                }
                            }
                        }
                    }
                )
            }

            AlertDialog.Builder(this@UsersFragment.requireContext())
                .setMessage(
                    if (is_admin == "1") "Are you sure you want to add this user to admins"
                    else "Are you sure you want to remove this user from admins"
                )
                .setTitle("Manage Admin")
                .setPositiveButton("Proceed") { dialog, _ ->
                    dialog.dismiss()
                    proceedRejection()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        override fun getItemCount(): Int {
            return this@UsersFragment.clients.size
        }

        internal inner class UserListViewHolder(val binding: ListUsersBinding) : RecyclerView.ViewHolder(binding.root)
    }

    data class Client(
        var client_id: Long, val user_avatar: String, val full_name: String, val user_contact: String, val user_gender: String,
        val residential_village: String, val residential_district: String,
        var hospital_name: String, var hospital_location: String, var is_admin: String,
    )
}