package com.wilsofts.myambulance.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.wilsofts.myambulance.R
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

object Utils {
    const val SHOW_LOG: Boolean = true

    const val BASE_URL: String = "https://ambulance.wilsofts.com/"
    //const val BASE_URL: String = "http://192.168.43.205:8010"

    fun logE(tag: String, log: String, error: Throwable? = null) {
        Log.e(tag, log, error)
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun formatTime(time: String): Int {
        val times = time.split(" ")
        val length = times.size

        var minutes = 0
        if (length >= 2) {
            minutes += times[0].toInt()
        }
        if (length >= 4) {
            minutes += (times[2].toInt() * 60)
        }
        return minutes
    }

    @SuppressLint("SimpleDateFormat")
    fun createImageFile(context: Context): File {
        val imageFileName = "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, /* prefix */".jpg", /* suffix */storageDir /* directory */)
    }

    fun glidePath(context: Context, imageView: ImageView, path: String) {
        Glide
            .with(context)
            .load(path)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(false)
            .into(imageView)
    }

    fun generateBitmapDescriptorFromRes(activity: FragmentActivity, resId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(activity, resId)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun showNotification(title: String, content: String, intent: Intent? = null, context: Context) {
       // val soundUri: Uri = Uri.parse("android.resource://" + context.packageName.toString() + "/" + R.raw.arpeggio_467)

        fun createChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("DEFAULT_CHANNEL", "My Default Channel", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    this.description = "This is the default channel"
                    this.enableLights(true)
                    this.lightColor = Color.RED
                    this.enableVibration(true)
                    this.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                    /*this.setSound(
                        soundUri,
                        AudioAttributes.Builder().also {
                            it.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            it.setUsage(AudioAttributes.USAGE_ALARM)
                        }.build()
                    )*/
                }
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            }
        }
        createChannel()

        val builder = NotificationCompat.Builder(context, "DEFAULT_CHANNEL")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            //.setSound(soundUri)

        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
        }

        with(NotificationManagerCompat.from(context)) {
            this.notify(AppPrefs.notification_id, builder.build())
            AppPrefs.notification_id += 1
        }
    }

    class GeocodedAddress(var latitude: Double, var longitude: Double, var placeName: String)

    class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Use the current date as the default date in the picker
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            // Create a new instance of DatePickerDialog and return it
            return DatePickerDialog(requireContext(), this, year, month, day)
        }

        @SuppressLint("SimpleDateFormat")
        override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DATE, day)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            (this.requireArguments().getSerializable("dateInterface")!! as DateInterface)
                .dateSet(
                    date = SimpleDateFormat("EEE dd MMM yyyy").format(Date(calendar.timeInMillis)),
                    server = SimpleDateFormat("yyyy-MM-dd").format(Date(calendar.timeInMillis))
                )
        }

        companion object {
            fun newInstance(activity: FragmentActivity, dateInterface: DateInterface, date: String) {
                val newFragment = DatePickerFragment()
                newFragment.arguments = Bundle().apply {
                    this.putSerializable("dateInterface", dateInterface)
                    this.putString("date", date)
                }
                newFragment.show(activity.supportFragmentManager, "datePicker")
            }
        }

        interface DateInterface : Serializable {
            fun dateSet(date: String, server: String)
        }
    }
}