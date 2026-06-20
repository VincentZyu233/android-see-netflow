package com.vincentzyu.androidseenetflow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val startButton = findViewById<Button>(R.id.startButton)

        statusText.text = "Scaffold ready. Foreground service is not wired yet."
        startButton.setOnClickListener {
            val intent = Intent(this, NetflowForegroundService::class.java)
            startForegroundService(intent)
            statusText.text = "Foreground service started. Sampling integration is pending."
        }
    }
}
