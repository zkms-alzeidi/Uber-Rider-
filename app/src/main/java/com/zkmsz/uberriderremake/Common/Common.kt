package com.zkmsz.uberriderremake.Common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.Marker
import com.zkmsz.uberriderremake.Model.DriverGeoModel
import com.zkmsz.uberriderremake.Model.RiderModel
import com.zkmsz.uberriderremake.R
import java.lang.StringBuilder

object Common
{

    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentRider!!.firstName)
            .append(" ")
            .append(currentRider!!.lastName)
            .toString()
    }

    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent: PendingIntent? = null
        if(intent != null)
        {
            pendingIntent = PendingIntent.getActivity(context,id,intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val NOTIFICATION_CHANNEL_ID= "edmt_dev_uber_make"
        val notificationManager= context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel= NotificationChannel(NOTIFICATION_CHANNEL_ID,"User Remake", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "Uber Remake"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder= NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_baseline_directions_car_24))

        if(pendingIntent != null)
        {
            builder.setContentIntent(pendingIntent)

        }

        val notification = builder.build()
        notificationManager.notify(id, notification)
    }

    fun buildName(firstName: String, lastName: String): String? {
        return StringBuilder(firstName).append(" ").append(lastName).toString()
    }

    val MarkerList: MutableMap<String,Marker> = HashMap<String, Marker>()
    val DRIVER_INFO_REFERENCES: String= "DriverInfo"
    val driversFound: MutableSet<DriverGeoModel> =  HashSet<DriverGeoModel>()
    val DRIVER_LOCATION_REFERENCES: String= "DriversLocation"
    val NOTI_BODY: String = "body"
    val NOTI_TITLE: String="title"

    val TOKEN_REFERENCE: String= "Token"
    var currentRider: RiderModel? = null
    val RIDER_INFO_REFERENCE: String = "Riders"
}