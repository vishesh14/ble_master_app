package com.bleaudiocontroller.app

import android.content.Context
import android.content.pm.PackageManager


/**
 * Created by Prog on 27.05.2015.
 */
object Helper {
    fun checkBLE(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun getServiceName(uuid: String?): String {
        return when (uuid) {
            "00001800-0000-1000-8000-00805f9b34fb" -> {
                "Generic Access"
            }

            "00001801-0000-1000-8000-00805f9b34fb" -> {
                "Generic Attribute"
            }

            "00001111-0000-1000-8000-00805f9b34fb" -> {
                "Notification Service"
            }

            else -> "Unknown Service"
        }
    }
}