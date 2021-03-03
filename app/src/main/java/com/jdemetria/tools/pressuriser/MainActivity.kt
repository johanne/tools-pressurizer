package com.jdemetria.tools.pressuriser

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

/**
 * Main entry point for the app. Can be used to lock / unlock memory, or run as a service.
 */
class MainActivity : Activity() {
    private lateinit var memoryStatsTextView: TextView
    private lateinit var refreshStatsButton: Button
    private lateinit var freeMemoryButton: Button
    private lateinit var applyPressureButton: Button
    private lateinit var percentPressureEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        memoryStatsTextView = findViewById(R.id.txt_memory_stats)
        percentPressureEditText = findViewById(R.id.edit_txt_pressure)
        refreshStatsButton = findViewById(R.id.btn_refresh_stats)
        freeMemoryButton = findViewById(R.id.btn_free_memory)
        applyPressureButton = findViewById(R.id.btn_lock_memory)
        refreshStats()
        refreshStatsButton.setOnClickListener {
            refreshStats()
        }
        freeMemoryButton.setOnClickListener {
            MemoryPressureNative.freeLockedMemory()
        }
        applyPressureButton.setOnClickListener {
            lockMemory()
        }
    }

    private fun refreshStats() {
        val totalSystemMemory = MemoryPressureNative.getTotalMemory()
        val freeMemory = MemoryPressureNative.getFreeMemory()
        val usedMemory = MemoryPressureNative.getUsedMemory()
        var memoryInfo = ActivityManager.MemoryInfo()
        val activityManager: ActivityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        val textToShow: String = "Total System Memory: $totalSystemMemory\n" +
                "Used Memory: $usedMemory\n" +
                "Free Memory: $freeMemory\n" +
                "Free Memory(Android): ${memoryInfo.availMem}"
        memoryStatsTextView.text = textToShow
    }

    private fun lockMemory() {
        var tempValue = percentPressureEditText.text.toString().toFloatOrNull()
        if (tempValue != null) {
            tempValue /= 100f
            tempValue = tempValue.coerceIn(0f, 1.0f)
            MemoryPressureNative.lockPercentOfMemory(tempValue)
        }
    }
}