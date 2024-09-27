package com.example.myapplication2.repository

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.myapplication2.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessageService  : FirebaseMessagingService() {

        override fun onMessageReceived(remoteMessage: RemoteMessage) {
            super.onMessageReceived(remoteMessage)

            // Logica per gestire la notifica ricevuta
            remoteMessage.notification?.let {
                sendNotification(it.title, it.body)
            }
        }

        private fun sendNotification(title: String?, messageBody: String?) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = 1

            val notification = NotificationCompat.Builder(this, "daily_notification")
                .setSmallIcon(R.drawable.notificaicona)
                .setContentTitle(title ?: "Notifica giornaliera")
                .setContentText(messageBody ?: "Ecco il tuo promemoria quotidiano!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(notificationId, notification)
        }

        override fun onNewToken(token: String) {
        }
    }

