package com.wilsofts.myambulance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.wilsofts.myambulance.auth.LogInActivity
import com.wilsofts.myambulance.databinding.ActivityMainBinding
import com.wilsofts.myambulance.ui.home.HomeFragment
import com.wilsofts.myambulance.ui.home.RequestInfo
import com.wilsofts.myambulance.ui.notifications.NotificationsFragment
import com.wilsofts.myambulance.ui.users.UsersFragment
import com.wilsofts.myambulance.utils.AppPrefs
import com.wilsofts.myambulance.utils.BaseActivity

class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMainBinding

    private val newRequest = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            AppPrefs.request_id = intent!!.getLongExtra("request_id", 0L)
            RequestInfo(
                requestID = AppPrefs.request_id, activity = this@MainActivity,
                fragmentManager = this@MainActivity.supportFragmentManager
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
                R.id.navigation_notifications -> {
                    this.showFragment(NotificationsFragment())
                }
                R.id.navigation_users -> {
                    this.showFragment(UsersFragment())
                }
            }
            true
        }

        this.binding.navView.menu.findItem(R.id.navigation_users).apply {
            this.isVisible = AppPrefs.is_admin
        }
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

        if (AppPrefs.request_id > 0L) {
            RequestInfo(requestID = AppPrefs.request_id, activity = this, fragmentManager = this.supportFragmentManager)
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.newRequest)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.main, menu)
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