package com.example.myapplication2.repository

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication2.R

class NotificaWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

   override fun doWork(): Result {

        sendNotification()
        Log.d("DailyNotificationWorker", "doWork completed")
        return Result.success()
    }

    private fun sendNotification() {

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1

        val notification = NotificationCompat.Builder(applicationContext, "daily_notification")
            .setSmallIcon(R.drawable.notificaicona)
            .setContentTitle("Notifica giornaliera")
            .setContentText("Come ti senti oggi?!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)

    }
}
