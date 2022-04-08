package com.wilsofts.myambulance.auth

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.wilsofts.myambulance.R
import com.wilsofts.myambulance.databinding.ActivityCreateAccountBinding
import com.wilsofts.myambulance.utils.BaseActivity
import com.wilsofts.myambulance.utils.MyProgressDialog
import com.wilsofts.myambulance.utils.Utils
import com.wilsofts.myambulance.utils.network.ApiClient
import com.wilsofts.myambulance.utils.network.ApiService
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import java.io.File

class CreateAccountActivity : BaseActivity() {
    private lateinit var binding: ActivityCreateAccountBinding
    private lateinit var imageLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private var dateOfBirth = ""
    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityCreateAccountBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)

        this.binding.hasAccount.setOnClickListener {
            Intent(this, LogInActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                this@CreateAccountActivity.startActivity(this)
            }
        }

        this.binding.btnCreateAccount.setOnClickListener {
            this.createAccount()
        }

        this.binding.dateOfBirth.setOnClickListener {
            Utils.DatePickerFragment.newInstance(
                activity = this, date = this.dateOfBirth,
                dateInterface = object : Utils.DatePickerFragment.DateInterface {
                    override fun dateSet(date: String, server: String) {
                        this@CreateAccountActivity.dateOfBirth = server
                        this@CreateAccountActivity.binding.dateOfBirth.setText(date)
                    }
                }
            )
        }

        this.eventsManager()
    }

    private fun eventsManager() {
        this.requestPermissionLauncher =
            this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                var falseCount = 0
                permissions.entries.forEach {
                    if (it.value == false) {
                        falseCount++
                    }
                }
                if (falseCount > 0) {
                    Toast.makeText(this, "Permissions denied, not opening", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Permissions granted, please retry recent action", Toast.LENGTH_LONG).show()
                }
            }

        this.imageLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                this.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(this.photoFile!!.absolutePath))
            }
        }

        val items = listOf("Male", "Female")
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        (this.binding.userGender.editText as? AutoCompleteTextView)?.apply {
            this.setAdapter(adapter)
            this.setText(items[0], false)
        }

        this.binding.attachCamera.setOnClickListener {
            if (!this.hasPermissions()) {
                this.requestPermissions()
            } else {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (intent.resolveActivity(this.packageManager) != null) {
                    this.photoFile = Utils.createImageFile(context = this)
                    if (this.photoFile != null) {
                        val uri = FileProvider.getUriForFile(this, "com.wilsofts.myambulance.fileprovider", this.photoFile!!)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                        this.imageLauncher.launch(intent)
                    } else {
                        Utils.showToast(this, "Could not initialise directory")
                    }
                } else {
                    Utils.showToast(this, "No camera app found")
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
            this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            AlertDialog.Builder(this)
                .setMessage("Storage and camera permissions are required to capture driver images")
                .setTitle("Permission Request")
                .setPositiveButton("Grant") { dialog, _ ->
                    dialog.dismiss()
                    this.requestPermissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA))
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "Permissions denied, not opening", Toast.LENGTH_LONG).show()
                }
                .create()
                .show()
        } else {
            this.requestPermissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA))
        }
    }

    private fun createAccount() {
        val fullName = this.binding.fullName.text.toString().trim()
        val mobileContact = this.binding.mobileContact.text.toString().trim().replace("[^0-9]".toRegex(), "")
        val residentialDistrict = this.binding.txtLocationDistrict.text.toString().trim()
        val residentialVillage = this.binding.txtLocationVillage.text.toString().trim()

        when {
            this.photoFile == null -> {
                Utils.showToast(this, "No driver photo found")
            }
            fullName.length < 5 -> {
                Utils.showToast(this, "Enter a valid full name at least 5 characters")
            }
            mobileContact.length < 10 -> {
                Utils.showToast(this, "Enter a valid contact")
            }
            else -> {
                val dialog = MyProgressDialog.newInstance(title = "Creating account, please wait")
                dialog.showDialog(activity = this)

                Thread {
                    this.lifecycleScope.launch {
                        val file = Compressor.compress(this@CreateAccountActivity, this@CreateAccountActivity.photoFile!!) {
                            this.default(width = 350, format = Bitmap.CompressFormat.JPEG)
                        }

                        this@CreateAccountActivity.runOnUiThread {
                            this@CreateAccountActivity.proceedCreation(
                                call = ApiClient.getRetrofit().create(ApiService::class.java)
                                    .createAccount(
                                        user_avatar = MultipartBody.Part.createFormData("user_avatar",
                                            file.name, file.asRequestBody("image/*".toMediaTypeOrNull())),
                                        client_id = "0".toRequestBody("text/plan".toMediaTypeOrNull()),
                                        full_name = fullName.toRequestBody("text/plan".toMediaTypeOrNull()),
                                        user_contact = mobileContact.toRequestBody("text/plan".toMediaTypeOrNull()),
                                        user_gender = this@CreateAccountActivity.binding.userGender.editText!!.text.toString().trim()
                                            .toRequestBody("text/plan".toMediaTypeOrNull()),
                                        date_of_birth = this@CreateAccountActivity.dateOfBirth.toRequestBody("text/plan".toMediaTypeOrNull()),
                                        residential_district = residentialDistrict.toRequestBody("text/plan".toMediaTypeOrNull()),
                                        residential_village = residentialVillage.toRequestBody("text/plan".toMediaTypeOrNull()),
                                    ),
                                dialog = dialog
                            )
                        }
                    }
                }.start()
            }
        }
    }

    private fun proceedCreation(call: Call<String>, dialog: MyProgressDialog) {
        ApiClient.createRequest(
            call = call,
            apiResponse = object : ApiClient.ApiResponse {
                override fun getResponse(response: JSONObject?, error: Throwable?) {
                    dialog.dismiss()

                    if (response !== null) {
                        if (response.has("code")) {
                            if (response.getInt("code") == 1) {
                                this@CreateAccountActivity.photoFile?.delete()

                                Utils.showToast(
                                    context = this@CreateAccountActivity,
                                    message = "Created account successfully, you can now proceed logging in"
                                )

                                Intent(this@CreateAccountActivity, LogInActivity::class.java).apply {
                                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    this@CreateAccountActivity.startActivity(this)
                                }
                            } else if (response.getInt("code") == 2) {
                                Utils.showToast(context = this@CreateAccountActivity, message = "Contact is already in use")
                            }
                        } else {
                            Utils.showToast(context = this@CreateAccountActivity, message = "Could not save driver, please retry")
                        }
                    }
                }
            })
    }
}