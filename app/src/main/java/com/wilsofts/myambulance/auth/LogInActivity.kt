package com.wilsofts.myambulance.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.wilsofts.myambulance.MainActivity
import com.wilsofts.myambulance.databinding.ActivityLogInBinding
import com.wilsofts.myambulance.utils.AppPrefs
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import org.json.JSONObject


class LogInActivity : BaseActivity() {
    private lateinit var binding: ActivityLogInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityLogInBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)

        this.binding.noAccount.setOnClickListener {
            Intent(this, CreateAccountActivity::class.java).apply {
                this@LogInActivity.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                this@LogInActivity.startActivity(this)
            }
        }

        this.binding.btnLogin.setOnClickListener {
            this.loginUser()
        }
    }

    private fun loginUser() {
        val contact = this.binding.textAccountContact.text.toString().trim().replace("[^0-9]".toRegex(), "")
        if (contact.length < 9) {
            Utils.showToast(this, "Enter a valid contact")
        } else {
            val dialog = MyProgressDialog.newInstance(title = "Authenticating account, please wait")
            dialog.showDialog(activity = this)

            ApiClient.createRequest(
                call = ApiClient.getRetrofit().create(ApiService::class.java).loginUser(user_contact = contact),
                apiResponse = object : ApiClient.ApiResponse {
                    override fun getResponse(response: JSONObject?, error: Throwable?) {
                        dialog.dismiss()
                        if (response !== null) {
                            if (response.has("code")) {
                                if (response.getInt("code") == 1) {
                                    AppPrefs.logInUser(response.getJSONObject("client"))
                                    Utils.showToast(context = this@LogInActivity, message = "Logged in successfully")
                                    Handler(Looper.getMainLooper()).postDelayed({ this@LogInActivity.proceedHome() }, 1000)
                                } else if (response.getInt("code") == 2) {
                                    Utils.showToast(context = this@LogInActivity, message = "No account found")
                                }
                            } else {
                                Utils.showToast(context = this@LogInActivity, message = "Could not login into account, please retry")
                            }
                        }
                    }
                }
            )
        }
    }

    private fun proceedHome() {
        if (AppPrefs.bearer_token.isNotEmpty()) {
            this.startActivity(Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppPrefs.bearer_token.isEmpty()) {
            this.binding.layoutLogin.visibility = View.VISIBLE
        }
    }


    override fun locationInitialised(status: Boolean) {
        if (status) {
            this@LogInActivity.proceedHome()
        } else {
            Toast.makeText(this, "Location permissions denied, application is exiting", Toast.LENGTH_LONG).show()
            this.finish()
        }
    }
}