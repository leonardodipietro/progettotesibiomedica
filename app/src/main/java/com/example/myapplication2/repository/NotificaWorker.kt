package com.example.myapplication2.repository

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication2.R
import java.util.Locale

class NotificaWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

   override fun doWork(): Result {

        sendNotification()
        Log.d("DailyNotificationWorker", "doWork completed")
        return Result.success()
    }

    private fun sendNotification() {
        // Carica la lingua scelta dall'utente
        val sharedPref = applicationContext.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("LANGUAGE", "it")
        val locale = Locale(languageCode ?: "it")
        Locale.setDefault(locale)
        val config = applicationContext.resources.configuration
        config.setLocale(locale)
        applicationContext.resources.updateConfiguration(config, applicationContext.resources.displayMetrics)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = 1

        // Ottieni il titolo e il testo della notifica dalle risorse di stringa
        val title = applicationContext.getString(R.string.notification_title)
        val text = applicationContext.getString(R.string.notification_text)

        val notification = NotificationCompat.Builder(applicationContext, "daily_notification")
            .setSmallIcon(R.drawable.notificaicona)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationId, notification)
    }

}
