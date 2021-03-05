package com.jdemetria.tools.pressuriser

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.*

/**
 * Main entry point for the app. Can be used to lock / unlock memory, or run as a service.
 */
class MainActivity : Activity() {
    private lateinit var memoryStatsTextView: TextView
    private lateinit var refreshStatsButton: Button
    private lateinit var freeMemoryButton: Button
    private lateinit var applyPressureButton: Button
    private lateinit var runAsServiceButton: Button
    private lateinit var percentPressureEditText: EditText
    private lateinit var percentRadioButton: RadioButton
    private lateinit var memoryToConsumeRadioButton: RadioButton
    private lateinit var memoryToLeaveRadioButton: RadioButton
    private lateinit var memoryRadioGroup: RadioGroup
    private lateinit var progressBar: ProgressBar
    private lateinit var handler: Handler
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handler = Handler(mainLooper)
        memoryStatsTextView = findViewById(R.id.txt_memory_stats)
        percentPressureEditText = findViewById(R.id.edit_txt_pressure)
        refreshStatsButton = findViewById(R.id.btn_refresh_stats)
        freeMemoryButton = findViewById(R.id.btn_free_memory)
        applyPressureButton = findViewById(R.id.btn_lock_memory)
        runAsServiceButton = findViewById(R.id.btn_run_as_service)
        runAsServiceButton.setOnClickListener {
            startForegroundService(Intent(this, MemoryPressureService::class.java))
            finish()
        }
        refreshStatsButton.setOnClickListener {
            refreshStats()
        }
        freeMemoryButton.setOnClickListener {
            MemoryPressureNative.freeLockedMemory()
            if (runnable != null) {
                handler.removeCallbacks(runnable!!)
            }
            refreshStats()
        }
        applyPressureButton.setOnClickListener {
            lockMemory()
            refreshStats()
        }
        percentRadioButton = findViewById(R.id.radio_btn_percent)
        percentRadioButton.setOnClickListener {
            percentPressureEditText.setHint(R.string.edit_text_hint_percent_pressure)
        }
        memoryToConsumeRadioButton = findViewById(R.id.radio_btn_mem_in_mb)
        memoryToConsumeRadioButton.setOnClickListener {
            percentPressureEditText.setHint(R.string.edit_text_hint_total_memory)
        }
        memoryToLeaveRadioButton = findViewById(R.id.radio_btn_to_leave)
        memoryToLeaveRadioButton.setOnClickListener {
            percentPressureEditText.setHint(R.string.edit_text_hint_memory_to_leave)
        }
        memoryRadioGroup = findViewById(R.id.radio_group)
        percentRadioButton.isChecked = true
        progressBar = findViewById(R.id.progress_bar)
        progressBar.min = 0
        progressBar.max = 100
        refreshStats()
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
        val percentFull = (100 * (usedMemory.toFloat() / totalSystemMemory.toFloat())).toInt()
        progressBar.setProgress(percentFull, true)
    }

    private fun lockMemory() {
        val selectedRadioItem = memoryRadioGroup.checkedRadioButtonId
        var tempValue: Number? = null
        when (selectedRadioItem) {
            R.id.radio_btn_percent -> tempValue =
                percentPressureEditText.text.toString().toFloatOrNull()
            R.id.radio_btn_mem_in_mb, R.id.radio_btn_to_leave -> tempValue =
                percentPressureEditText.text.toString().toIntOrNull()
        }
        if (tempValue != null) {
            when (selectedRadioItem) {
                R.id.radio_btn_percent -> {
                    var floatValue = tempValue.toFloat() / 100f
                    floatValue = floatValue.coerceIn(0f, 1.0f)
                    MemoryPressureNative.lockPercentOfMemory(floatValue)
                    runnable = Runnable {
                        refreshStats()
                        handler.postDelayed(runnable!!, 50)
                    }
                    handler.post(runnable!!)
                }
                R.id.radio_btn_mem_in_mb -> MemoryPressureNative.lockMemoryInMB(tempValue.toLong())
                R.id.radio_btn_to_leave -> MemoryPressureNative.leaveMemoryInMB(tempValue.toLong())
            }
        }
    }
}