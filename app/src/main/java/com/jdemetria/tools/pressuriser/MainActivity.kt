package com.jdemetria.tools.pressuriser

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

/**
 * Main entry point for the app. Can be used to lock / unlock memory, or run as a service.
 */
class MainActivity : Activity() {
    private lateinit var memoryStatsTextView: TextView
    private lateinit var refreshStatsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        memoryStatsTextView = findViewById(R.id.txt_memory_stats)
        refreshStatsButton = findViewById(R.id.btn_refresh_stats)
        refreshStats()
        refreshStatsButton.setOnClickListener{
            refreshStats()
        }
    }

    private fun refreshStats() {
        val totalSystemMemory = MemoryPressureNative.getTotalMemory()
        val freeMemory = MemoryPressureNative.getFreeMemory()
        val usedMemory = MemoryPressureNative.getUsedMemory()
        val textToShow: String = "Total System Memory: $totalSystemMemory\n" +
                "Used Memory: $usedMemory\n" +
                "Free Memory: $freeMemory"
        memoryStatsTextView.text =textToShow
    }
}