package com.tecly.listen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {
    private val TAG = "ForegroundService"
    private val CHANNEL_ID = "notify_channel_01" // Cambiado para evitar conflictos
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Servicio creado")
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Servicio iniciado")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notification Catcher",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Canal para servicio en primer plano"
                setShowBadge(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificación creado")
        }
    }

    private fun startForegroundService() {
        try {
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "Servicio en primer plano iniciado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar foreground: ${e.message}")
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle("Notification Catcher")
            setContentText("Capturando notificaciones...")
            setSmallIcon(R.drawable.ic_notification) // ¡Verifica que este recurso existe!
            setPriority(NotificationCompat.PRIORITY_LOW)
            setOngoing(true)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setCategory(Notification.CATEGORY_SERVICE)
            setShowWhen(false)
            color = getColor(android.R.color.system_accent1_200) // Color del ícono
        }.build()
    }
}