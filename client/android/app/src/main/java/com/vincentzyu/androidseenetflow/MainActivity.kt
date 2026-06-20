package com.vincentzyu.androidseenetflow

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val serverUrlInput = findViewById<EditText>(R.id.serverUrlInput)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        statusText.text = "Ready to stream Android netflow telemetry. ${RustBridge.describe()}."
        startButton.setOnClickListener {
            val serverUrl = serverUrlInput.text.toString().trim()
            val intent = Intent(this, NetflowForegroundService::class.java).apply {
                action = NetflowForegroundService.ACTION_START
                putExtra(NetflowForegroundService.EXTRA_SERVER_URL, serverUrl)
            }
            startForegroundService(intent)
            statusText.text = "Foreground service started. Streaming to $serverUrl"
        }

        stopButton.setOnClickListener {
            val intent = Intent(this, NetflowForegroundService::class.java).apply {
                action = NetflowForegroundService.ACTION_STOP
            }
            startService(intent)
            statusText.text = "Foreground service stop requested."
        }
    }
}
