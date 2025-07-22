package com.tecly.listen

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MyNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "YapeNotifListener"
        private const val YAPE_PACKAGE = "com.bcp.innovacxion.yapeapp"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Servicio de escucha de Yape inicializado")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Conexión establecida con el sistema de notificaciones")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Conexión perdida con el sistema de notificaciones")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (!isYapeNotification(sbn)) {
                Log.d(TAG, "Notificación descartada (no es de Yape)")
                return
            }

            val notificationData = extractYapeNotificationData(sbn)
            if (notificationData == null) {
                Log.w(TAG, "Datos de notificación de Yape vacíos o inválidos")
                return
            }

            val (title, content) = notificationData
            Log.d(TAG, """
                ========== NOTIFICACIÓN DE YAPE ==========
                Título: $title
                Contenido: $content
                ==========================================
            """.trimIndent())

            updateAppState("Yape: $title")
            sendYapeDataToServer(title, content)

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación de Yape", e)
        }
    }

    private fun isYapeNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName != YAPE_PACKAGE) {
            return false
        }

        return try {
            sbn.isClearable && sbn.notification != null && sbn.notification.extras != null
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando notificación de Yape", e)
            false
        }
    }

    private fun extractYapeNotificationData(sbn: StatusBarNotification): Pair<String, String>? {
        return try {
            val extras = sbn.notification.extras ?: return null

            fun getExtraText(key: String): String {
                return when (val obj = extras.get(key)) {
                    is CharSequence -> obj.toString().trim()
                    is String -> obj.trim()
                    else -> ""
                }
            }

            // Intenta obtener el texto principal de diferentes maneras
            var title = getExtraText("android.title")
            var content = getExtraText("android.text")

            // Si el contenido está vacío, prueba con bigText
            if (content.isEmpty()) {
                content = getExtraText("android.bigText")
            }

            // Formato específico para notificaciones de pago de Yape
            if (title == "Yape!" && content.isNotEmpty()) {
                // Puedes procesar adicionalmente el contenido aquí si es necesario
                return title to content
            }

            if (title.isEmpty() && content.isEmpty()) null else title to content
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo datos de notificación de Yape", e)
            null
        }
    }

    private fun updateAppState(status: String) {
        try {
            MainActivity.lastPackage = status
            Log.d(TAG, "Estado actualizado: $status")
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando estado", e)
        }
    }

    private fun sendYapeDataToServer(title: String, content: String) {
        serviceScope.launch {
            val payload = createYapePayload(title, content)
            Log.d(TAG, """
                ======== ENVIANDO DATOS DE YAPE ========
                URL: ${MainActivity.endpoint}
                Payload: $payload
                ========================================
            """.trimIndent())

            var connection: HttpsURLConnection? = null
            try {
                connection = createSecureConnection().apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer your_token_if_needed")
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = try {
                    if (responseCode in 200..299) {
                        connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    } else {
                        connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                    }
                } catch (e: Exception) {
                    "Error leyendo respuesta: ${e.message}"
                }

                Log.d(TAG, """
                    ======= RESPUESTA DEL SERVIDOR =======
                    Código: $responseCode
                    Respuesta: $response
                    ====================================
                """.trimIndent())

            } catch (e: Exception) {
                Log.e(TAG, "Error enviando datos de Yape", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun createYapePayload(title: String, content: String): String {
        return JSONObject().apply {
            put("app", "Yape")
            put("package_name", YAPE_PACKAGE)
            put("title", title)
            put("content", content)
            put("timestamp", System.currentTimeMillis())
            // Puedes añadir más campos según lo que necesite tu servidor
        }.toString()
    }

    private fun createSecureConnection(): HttpsURLConnection {
        return URL(MainActivity.endpoint).openConnection() as HttpsURLConnection
    }

    override fun onDestroy() {
        Log.i(TAG, "Deteniendo servicio de escucha de Yape...")
        serviceJob.cancel()
        super.onDestroy()
    }
}