package com.jdemetria.tools.pressuriser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "MemoryPressureBroadcastReceiver"
private const val INTENT_EXTRA = "val"
internal const val PKG = "com.jdemetria.tools.pressuriser"
internal const val ACTION_APPLY_PRESSURE_PERCENT = "$PKG.APPLY_MEM_PRESSURE_PERCENT"
internal const val ACTION_APPLY_PRESSURE_MB = "$PKG.APPLY_MEM_PRESSURE_MB"
internal const val ACTION_APPLY_PRESSURE_RETAIN = "$PKG.APPLY_MEM_PRESSURE_RETAIN"
internal const val ACTION_FREE_MEMORY = "$PKG.STOP_MEM_PRESSURE"

/**
 * Receiver that will listen to broadcasts for memory pressure.
 */
class MemoryPressureBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast.")
        when (intent.action) {
            ACTION_APPLY_PRESSURE_PERCENT -> {
                Log.d(TAG, "$ACTION_APPLY_PRESSURE_PERCENT received.")
                var percentPressure = intent.getFloatExtra(INTENT_EXTRA, 0f)
                percentPressure /= 100f
                percentPressure = percentPressure.coerceIn(0f, 1.0f)
                Log.d(TAG, "Percent of memory to consume: $percentPressure")
                MemoryPressureNative.lockPercentOfMemory(percentPressure)
            }
            ACTION_APPLY_PRESSURE_MB -> {
                Log.d(TAG, "$ACTION_APPLY_PRESSURE_MB received.")
                val memoryInMB = intent.getLongExtra(INTENT_EXTRA, 0)
                Log.d(TAG, "Memory in MB to consume: $memoryInMB")
                MemoryPressureNative.lockMemoryInMB(memoryInMB)
            }
            ACTION_APPLY_PRESSURE_RETAIN -> {
                Log.d(TAG, "$ACTION_APPLY_PRESSURE_RETAIN received.")
                val memoryInMB = intent.getLongExtra(INTENT_EXTRA, -1L)
                Log.d(TAG, "Memory in MB to keep free: $memoryInMB")
                if (memoryInMB == -1L) {
                    Log.d(TAG, "Invalid value passed. Ignoring broadcast.")
                }
                MemoryPressureNative.leaveMemoryInMB(memoryInMB)
            }
            ACTION_FREE_MEMORY -> {
                Log.d(TAG, "$ACTION_FREE_MEMORY received.")
                MemoryPressureNative.freeLockedMemory()
            }
        }
    }
}