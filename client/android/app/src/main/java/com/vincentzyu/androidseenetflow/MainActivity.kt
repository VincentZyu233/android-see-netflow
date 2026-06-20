package com.vincentzyu.androidseenetflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private val uiHandler = Handler(Looper.getMainLooper())
    private var previousRxBytes: Long = 0
    private var previousTxBytes: Long = 0
    private var previousSampleTimeMs: Long = 0
    private val recentSamples = ArrayDeque<Long>()
    private lateinit var rxRateText: TextView
    private lateinit var txRateText: TextView
    private lateinit var rxTotalText: TextView
    private lateinit var txTotalText: TextView
    private lateinit var interfaceText: TextView
    private lateinit var sampleTimeText: TextView
    private lateinit var sparklineText: TextView
    private lateinit var streamStateChip: TextView
    private lateinit var foregroundStateChip: TextView
    private lateinit var deviceModelText: TextView
    private lateinit var serverTargetText: TextView
    private lateinit var lastSentText: TextView
    private lateinit var statusText: TextView

    private val serviceStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra(NetflowForegroundService.EXTRA_STATUS) ?: return
            val serverUrl = intent.getStringExtra(NetflowForegroundService.EXTRA_SERVER_URL).orEmpty()
            val lastSentAt = if (intent.hasExtra(NetflowForegroundService.EXTRA_LAST_SENT_AT)) {
                intent.getLongExtra(NetflowForegroundService.EXTRA_LAST_SENT_AT, 0L)
            } else {
                null
            }
            updateServiceStatus(state, serverUrl, lastSentAt)
        }
    }

    private val statsTicker = object : Runnable {
        override fun run() {
            updateLocalStats()
            uiHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val serverUrlInput = findViewById<EditText>(R.id.serverUrlInput)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)
        rxRateText = findViewById(R.id.rxRateText)
        txRateText = findViewById(R.id.txRateText)
        rxTotalText = findViewById(R.id.rxTotalText)
        txTotalText = findViewById(R.id.txTotalText)
        interfaceText = findViewById(R.id.interfaceText)
        sampleTimeText = findViewById(R.id.sampleTimeText)
        sparklineText = findViewById(R.id.sparklineText)
        streamStateChip = findViewById(R.id.streamStateChip)
        foregroundStateChip = findViewById(R.id.foregroundStateChip)
        deviceModelText = findViewById(R.id.deviceModelText)
        serverTargetText = findViewById(R.id.serverTargetText)
        lastSentText = findViewById(R.id.lastSentText)

        statusText.text = "等待开始推流。\n${RustBridge.describe()}。"
        deviceModelText.text = "设备：${Build.MANUFACTURER} ${Build.MODEL}"
        updateServiceStatus("Disconnected", serverUrlInput.text.toString(), null)
        startButton.setOnClickListener {
            val serverUrl = serverUrlInput.text.toString().trim()
            val intent = Intent(this, NetflowForegroundService::class.java).apply {
                action = NetflowForegroundService.ACTION_START
                putExtra(NetflowForegroundService.EXTRA_SERVER_URL, serverUrl)
            }
            startForegroundService(intent)
            updateForegroundChip(true)
            statusText.text = "前台服务已启动。\n正在尝试连接并推流到：$serverUrl"
        }

        stopButton.setOnClickListener {
            val intent = Intent(this, NetflowForegroundService::class.java).apply {
                action = NetflowForegroundService.ACTION_STOP
            }
            startService(intent)
            updateForegroundChip(false)
            statusText.text = "已请求停止前台服务。"
        }
    }

    override fun onResume() {
        super.onResume()
        registerStatusReceiver()
        previousRxBytes = 0
        previousTxBytes = 0
        previousSampleTimeMs = 0
        recentSamples.clear()
        uiHandler.post(statsTicker)
    }

    override fun onPause() {
        uiHandler.removeCallbacks(statsTicker)
        unregisterReceiver(serviceStatusReceiver)
        super.onPause()
    }

    private fun updateLocalStats() {
        val now = System.currentTimeMillis()
        val totalRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        val totalTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)

        val elapsedSeconds = if (previousSampleTimeMs == 0L) {
            1.0
        } else {
            ((now - previousSampleTimeMs).coerceAtLeast(1L)) / 1000.0
        }

        val rxRate = if (previousSampleTimeMs == 0L) 0L else
            ((totalRx - previousRxBytes).coerceAtLeast(0L) / elapsedSeconds).toLong()
        val txRate = if (previousSampleTimeMs == 0L) 0L else
            ((totalTx - previousTxBytes).coerceAtLeast(0L) / elapsedSeconds).toLong()

        rxRateText.text = formatBytesPerSecond(rxRate)
        txRateText.text = formatBytesPerSecond(txRate)
        rxTotalText.text = "Total ${formatBytes(totalRx)}"
        txTotalText.text = "Total ${formatBytes(totalTx)}"
        interfaceText.text = "Interface: ${detectPrimaryInterfaceName()}"
        sampleTimeText.text = String.format(Locale.US, "%.1fs sample", elapsedSeconds)
        updateSparkline(max(rxRate, txRate))

        previousRxBytes = totalRx
        previousTxBytes = totalTx
        previousSampleTimeMs = now
    }

    private fun updateSparkline(sample: Long) {
        recentSamples.addLast(sample)
        while (recentSamples.size > 20) {
            recentSamples.removeFirst()
        }
        val levels = "▁▂▃▄▅▆▇█"
        val peak = recentSamples.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        sparklineText.text = recentSamples.joinToString(separator = "") { value ->
            val index = (((value.toDouble() / peak) * (levels.length - 1)).toInt())
                .coerceIn(0, levels.length - 1)
            levels[index].toString()
        }.padEnd(20, '▁')
    }

    private fun registerStatusReceiver() {
        val filter = IntentFilter(NetflowForegroundService.ACTION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStatusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(serviceStatusReceiver, filter)
        }
    }

    private fun updateServiceStatus(state: String, serverUrl: String, lastSentAt: Long?) {
        streamStateChip.text = state
        streamStateChip.setBackgroundResource(
            when (state) {
                "Streaming" -> R.drawable.bg_chip_active
                "Invalid URL" -> R.drawable.bg_chip_warning
                else -> R.drawable.bg_chip_idle
            }
        )
        serverTargetText.text = "Server: $serverUrl"
        lastSentText.text = if (lastSentAt == null || lastSentAt == 0L) {
            "Last sent: waiting"
        } else {
            "Last sent: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastSentAt))}"
        }
    }

    private fun updateForegroundChip(active: Boolean) {
        foregroundStateChip.text = if (active) "Foreground active" else "Foreground idle"
        foregroundStateChip.setBackgroundResource(
            if (active) R.drawable.bg_chip_active else R.drawable.bg_chip_idle
        )
    }

    private fun detectPrimaryInterfaceName(): String {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        return interfaces.firstOrNull { iface ->
            iface.isUp && !iface.isLoopback && iface.interfaceAddresses.isNotEmpty()
        }?.name ?: "unknown"
    }

    private fun formatBytesPerSecond(value: Long): String = "${formatBytes(value)}/s"

    private fun formatBytes(value: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = value.toDouble()
        var index = 0
        while (size >= 1024 && index < units.lastIndex) {
            size /= 1024
            index += 1
        }
        return String.format(Locale.US, "%.1f %s", size, units[index])
    }
}
