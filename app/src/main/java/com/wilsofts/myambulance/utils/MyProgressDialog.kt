package com.wilsofts.myambulance.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.wilsofts.myambulance.databinding.FragmentProgressDialogBinding

class MyProgressDialog : DialogFragment() {
    private var title: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString("title")!!
        }
    }

    fun showDialog(activity: FragmentActivity) {
        // DialogFragment.show() will take care of adding the fragment in a transaction.
        // We also want to remove any currently showing dialog, so make our own transaction and take care of that here.
        val transaction: FragmentTransaction = activity.supportFragmentManager.beginTransaction()
        val fragment: Fragment? = activity.supportFragmentManager.findFragmentByTag("progress_dialog")
        if (fragment != null) {
            transaction.remove(fragment)
        }
        transaction.addToBackStack(null)

        // Create and show the dialog.
        this.show(transaction, "progress_dialog")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // make white background transparent
        //dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        this.isCancelable = false

        val binding = FragmentProgressDialogBinding.inflate(inflater, container, false)
        binding.progressTitle.text = this.title
        return binding.root
    }

    /* override fun onStart() {
         super.onStart()
         val window: Window? = dialog!!.window
         if (window != null) {
             val windowParams: WindowManager.LayoutParams = window.attributes
             windowParams.dimAmount = 0.0f
             window.attributes = windowParams
         }
     }*/

    override fun onResume() {
        super.onResume()
        this.dialog?.window?.setLayout(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT)
    }

    companion object {
        fun newInstance(title: String): MyProgressDialog =
            MyProgressDialog().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                }
            }
    }
}