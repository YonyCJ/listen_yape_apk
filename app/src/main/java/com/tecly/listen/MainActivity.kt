package com.tecly.listen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    companion object {
        var lastPackage: String = "Esperando notificaciones de Yape"
        var endpoint: String = ""
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            findViewById<TextView>(R.id.txtAppName).text = "Estado: $lastPackage"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "Actividad creada")

        initViews()
        checkNotificationAccess()
        handler.post(updateRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
        Log.d(TAG, "Actividad destruida")
    }

    private fun initViews() {
        findViewById<EditText>(R.id.txtEndpoint).setText(endpoint)
        findViewById<Button>(R.id.btnMinimize).setOnClickListener {
            Log.d(TAG, "Botón minimizar presionado")
            startForegroundServiceCompat()
            moveTaskToBack(true)
        }
    }

    private fun startForegroundServiceCompat() {
        try {
            Log.d(TAG, "Intentando iniciar servicio foreground")
            val serviceIntent = Intent(this, ForegroundService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "Servicio foreground iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar servicio: ${e.message}")
            Toast.makeText(this, "Error al iniciar servicio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationAccess() {
        if (!isNotificationListenerEnabled()) {
            Toast.makeText(this, "Activa el servicio de notificaciones", Toast.LENGTH_LONG).show()
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
    }
}