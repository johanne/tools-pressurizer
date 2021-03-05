/*
 * Copyright (C) 2021  JDemetria Studios AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jdemetria.tools.pressuriser

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Handler
import android.os.IBinder
import android.util.Log

private val TAG = MemoryPressureService::class.simpleName
private val NOTIFICATION_ID = 1
private val NOTIFICATION_CHANNEL_ID = "MemoryPressureChannel_Id"
private val MEMORY_PRESSURE_BROADCAST_RECEIVER = MemoryPressureBroadcastReceiver()

/**
 * Service that runs in the foreground with an on-going notification. This lets the pressure level
 * at the same value even if the system frees up memory from other tasks.
 */
class MemoryPressureService : Service() {
    private lateinit var backgroundHandler: Handler

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "$TAG being initialized...")
        backgroundHandler = Handler(mainLooper)
        startForegroundNotification()
        initializeReceiver()
        Log.i(TAG, "Initialization complete!")
    }

    override fun onTrimMemory(level: Int) {
        Log.d(TAG, "onTrimMemory - potentially need to be freed.")
        when (level) {
            TRIM_MEMORY_COMPLETE -> {
                Log.d(TAG, "TRIM_MEMORY_COMPLETE. Will send broadcast to free resources.")
                sendStopMemoryPressureBroadcast()
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.d(
                    TAG,
                    "TRIM_MEMORY_RUNNING_CRITICAL. Will send broadcast to free resources."
                )
                sendStopMemoryPressureBroadcast()
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                Log.d(TAG, "TRIM_MEMORY_RUNNING_LOW. Still keeping pressure. If triggered")
            }
            TRIM_MEMORY_MODERATE -> {
                Log.d(TAG, "TRIM_MEMORY_MODERATE. No action required.")
            }
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.d(TAG, "TRIM_MEMORY_RUNNING_MODERATE. No action required.")
            }
            TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "No action needed. Lowest trim memory levels.")
            }
        }
        super.onTrimMemory(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged. Will send broadcast to free resources.")
        sendStopMemoryPressureBroadcast()
        super.onConfigurationChanged(newConfig)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved. Will send broadcast to free resources.")
        sendStopMemoryPressureBroadcast()
        super.onTaskRemoved(rootIntent)
    }


    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun startForegroundNotification() {
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Service is running background")
            .setContentIntent(pendingIntent)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initializeReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_APPLY_PRESSURE_MB)
        intentFilter.addAction(ACTION_APPLY_PRESSURE_PERCENT)
        intentFilter.addAction(ACTION_APPLY_PRESSURE_RETAIN)
        intentFilter.addAction(ACTION_FREE_MEMORY)
        registerReceiver(
            MEMORY_PRESSURE_BROADCAST_RECEIVER, intentFilter, null, backgroundHandler
        )
    }

    private fun sendStopMemoryPressureBroadcast() {
        sendBroadcast(
            Intent(ACTION_FREE_MEMORY)
                .setClassName(PKG, "$PKG.MemoryPressureBroadcastReceiver")
        )
    }
}