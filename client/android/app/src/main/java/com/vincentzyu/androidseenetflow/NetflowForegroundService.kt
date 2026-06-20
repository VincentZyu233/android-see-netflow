package com.vincentzyu.androidseenetflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.NetworkInterface
import java.util.Collections

class NetflowForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var streamJob: Job? = null
    private var currentServerUrl: String = DEFAULT_SERVER_URL
    private var previousSnapshot: InterfaceSnapshot? = null
    private var previousTimestampMs: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopStreaming()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                currentServerUrl = intent?.getStringExtra(EXTRA_SERVER_URL)
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_SERVER_URL
                startForeground(NOTIFICATION_ID, buildNotification("Connecting to $currentServerUrl"))
                startStreaming()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopStreaming()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startStreaming() {
        if (streamJob?.isActive == true) {
            return
        }

        connectWebSocket()
        previousSnapshot = null
        previousTimestampMs = 0L

        streamJob = serviceScope.launch {
            while (isActive) {
                val payload = collectTelemetryPayload()
                webSocket?.send(payload.toString())
                updateNotification("Streaming to $currentServerUrl")
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    private fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        webSocket?.close(1000, "Service stopping")
        webSocket = null
    }

    private fun connectWebSocket() {
        webSocket?.cancel()
        val request = Request.Builder()
            .url(currentServerUrl)
            .build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {})
    }

    private fun collectTelemetryPayload(): JSONObject {
        val now = System.currentTimeMillis()
        val current = readSnapshot()
        val deltaSeconds = if (previousTimestampMs == 0L) {
            SAMPLE_INTERVAL_MS / 1000.0
        } else {
            ((now - previousTimestampMs).coerceAtLeast(1L)) / 1000.0
        }

        val previous = previousSnapshot
        val rxRate = if (previous == null) 0L else
            ((current.rxBytes - previous.rxBytes).coerceAtLeast(0L) / deltaSeconds).toLong()
        val txRate = if (previous == null) 0L else
            ((current.txBytes - previous.txBytes).coerceAtLeast(0L) / deltaSeconds).toLong()

        previousSnapshot = current
        previousTimestampMs = now

        val interfaces = JSONArray().put(
            JSONObject()
                .put("name", current.primaryInterface)
                .put("rx_bytes", current.rxBytes)
                .put("tx_bytes", current.txBytes)
                .put("rx_rate", rxRate)
                .put("tx_rate", txRate)
        )

        return JSONObject()
            .put("device_id", Build.MODEL.lowercase().replace(" ", "-"))
            .put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("timestamp", now)
            .put("network_type", "android")
            .put("interfaces", interfaces)
    }

    private fun readSnapshot(): InterfaceSnapshot {
        val mobileRx = TrafficStats.getMobileRxBytes().coerceAtLeast(0L)
        val mobileTx = TrafficStats.getMobileTxBytes().coerceAtLeast(0L)
        val totalRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        val totalTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
        val rxBytes = if (totalRx > 0) totalRx else mobileRx
        val txBytes = if (totalTx > 0) totalTx else mobileTx
        return InterfaceSnapshot(
            rxBytes = rxBytes,
            txBytes = txBytes,
            primaryInterface = detectPrimaryInterfaceName(),
        )
    }

    private fun detectPrimaryInterfaceName(): String {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        return interfaces.firstOrNull { iface ->
            iface.isUp && !iface.isLoopback && iface.interfaceAddresses.isNotEmpty()
        }?.name ?: "unknown"
    }

    private fun buildNotification(text: String): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Android See Netflow")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Netflow Service",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    data class InterfaceSnapshot(
        val rxBytes: Long,
        val txBytes: Long,
        val primaryInterface: String,
    )

    companion object {
        const val ACTION_START = "com.vincentzyu.androidseenetflow.action.START"
        const val ACTION_STOP = "com.vincentzyu.androidseenetflow.action.STOP"
        const val EXTRA_SERVER_URL = "server_url"

        private const val CHANNEL_ID = "netflow_service"
        private const val NOTIFICATION_ID = 1001
        private const val SAMPLE_INTERVAL_MS = 1000L
        private const val DEFAULT_SERVER_URL = "ws://192.168.1.2:8765"
    }
}
