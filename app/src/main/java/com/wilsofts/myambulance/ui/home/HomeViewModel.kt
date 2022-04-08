package com.wilsofts.myambulance.ui.home

import android.content.Context
import android.location.Location
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    fun cancelRequest(context: Context, requestID: Long) {
        AlertDialog.Builder(context)
            .setMessage("Are you sure you want to reject the clients request for an ambulance")
            .setTitle("Reject")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}