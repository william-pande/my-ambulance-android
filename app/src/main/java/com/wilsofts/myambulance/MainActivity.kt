package com.wilsofts.myambulance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.wilsofts.myambulance.auth.LogInActivity
import com.wilsofts.myambulance.databinding.ActivityMainBinding
import com.wilsofts.myambulance.ui.admin.ambulance.AmbulanceFragment
import com.wilsofts.myambulance.ui.admin.clients.ClientsFragment
import com.wilsofts.myambulance.ui.admin.drivers.DriversFragment
import com.wilsofts.myambulance.ui.home.HomeFragment
import com.wilsofts.myambulance.ui.home.driver.NewRequestActivity
import com.wilsofts.myambulance.ui.requests.RequestsFragment
import com.wilsofts.myambulance.utils.AppPrefs
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject


class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMainBinding

    private val newRequest = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            this@MainActivity.startActivity(
                Intent(this@MainActivity, NewRequestActivity::class.java)
                    .putExtra("request_id", intent!!.getLongExtra("request_id", 0L))
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityMainBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)

        this.setSupportActionBar(this.binding.toolbar.toolbar)

        this.binding.navView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    this.showFragment(HomeFragment())
                }
                R.id.navigation_requests -> {
                    this.showFragment(RequestsFragment())
                }
                R.id.navigation_clients -> {
                    this.showFragment(ClientsFragment())
                }
                R.id.navigation_drivers -> {
                    this.showFragment(DriversFragment())
                }
                R.id.navigation_ambulances -> {
                    this.showFragment(AmbulanceFragment())
                }
            }
            true
        }

        this.binding.navView.menu.findItem(R.id.navigation_clients).isVisible = AppPrefs.is_admin
        this.binding.navView.menu.findItem(R.id.navigation_drivers).isVisible = AppPrefs.is_admin
        this.binding.navView.menu.findItem(R.id.navigation_ambulances).isVisible = AppPrefs.is_admin

        this.showFragment(HomeFragment())
    }

    private fun showFragment(fragment: Fragment) {
        this.supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        AppPrefs.updateFCMToken()
        LocalBroadcastManager.getInstance(this).registerReceiver(this.newRequest, IntentFilter("new_request"))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.newRequest)
    }

    override fun locationInitialised(status: Boolean) {
        if (!status) {
            Toast.makeText(this, "Location permissions denied, application is exiting", Toast.LENGTH_LONG).show()
            this.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.main, menu)

        menu.findItem(R.id.nav_log_out).isVisible = AppPrefs.driver_id == 0L

        menu.findItem(R.id.switch_action_bar).also { switchItem ->
            switchItem.isVisible = AppPrefs.driver_id > 0

            switchItem.setActionView(R.layout.layout_switch)
            switchItem.actionView.findViewById<SwitchCompat>(R.id.switch_status).also { switchButton ->
                switchButton.isChecked = AppPrefs.driver_status == "Online"
                switchButton.setOnCheckedChangeListener { _, isChecked ->
                    fun changeStatus() {
                        val dialog = MyProgressDialog.newInstance(title = "Changing status, please wait")
                        dialog.showDialog(activity = this)

                        ApiClient.createRequest(
                            call = ApiClient.getRetrofit().create(ApiService::class.java)
                                .changeDriverStatus(user_status = if (isChecked) "Online" else "Offline"),
                            apiResponse = object : ApiClient.ApiResponse {
                                override fun getResponse(response: JSONObject?, error: Throwable?) {
                                    dialog.dismiss()
                                    if (response !== null) {
                                        if (response.has("code")) {
                                            if (response.getInt("code") == 1) {
                                                AppPrefs.driver_status = if (isChecked) "Online" else "Offline"
                                            } else if (response.getInt("code") == 2) {
                                                Utils.showToast(context = this@MainActivity,
                                                    message = "Could not change status, please retry")
                                            }
                                        } else {
                                            Utils.showToast(context = this@MainActivity,
                                                message = "Could not change status, please retry")
                                        }
                                        switchButton.isChecked = AppPrefs.driver_status == "Online"
                                        this@MainActivity.updateStatusBarColor()
                                    }
                                }
                            }
                        )
                    }

                    if (!isChecked) {
                        AlertDialog.Builder(this)
                            .setMessage("Are you sure you want to become offline? Patients shall miss your services.")
                            .setTitle("Become Offline")
                            .setPositiveButton("Proceed") { dialog, _ ->
                                dialog.dismiss()
                                changeStatus()
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .create()
                            .show()
                    } else {
                        changeStatus()
                    }
                }
            }
        }


        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_log_out) {
            AlertDialog.Builder(this)
                .setMessage("Are you sure you want to log out?")
                .setTitle("Log Out")
                .setPositiveButton("Proceed") { dialog, _ ->
                    dialog.dismiss()
                    AppPrefs.logoutUser()
                    Handler(Looper.getMainLooper()).postDelayed({
                        this.startActivity(Intent(this, LogInActivity::class.java).apply {
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }, 500)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
        return super.onOptionsItemSelected(item)
    }
}