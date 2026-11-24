package com.focuslock.app.blocking

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

class AppDisabler(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(context, DeviceAdminReceiver::class.java)

    fun isOwner(): Boolean = try {
        dpm.isDeviceOwnerApp(context.packageName)
    } catch (e: Exception) {
        false
    }

    fun setupLockTask() {
        if (!isOwner()) return
        try {
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
            dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        } catch (e: Exception) {
            // Older devices may not support all lock-task features; lock task still works.
        }
    }

    /** Returns the original enabled state so it can be restored exactly. */
    fun disable(packageName: String): Int? {
        if (!isOwner() || isCritical(packageName) || !isInstalled(packageName)) return null
        return try {
            val original = pm.getApplicationEnabledSetting(packageName)
            if (original == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                null
            } else {
                val failed = dpm.setPackagesSuspended(admin, arrayOf(packageName), true)
                if (failed.isEmpty()) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun restore(packageName: String, originalState: Int) {
        if (!isOwner()) return
        try {
            dpm.setPackagesSuspended(admin, arrayOf(packageName), false)
        } catch (e: Exception) {
            // Ignore packages removed during the session.
        }
    }

    private fun isInstalled(packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0) != null
    } catch (e: Exception) {
        false
    }

    internal fun isCritical(packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName.startsWith("com.android.")) return true
        if (packageName.startsWith("android.")) return true
        return packageName in CRITICAL
    }

    companion object {
        private val CRITICAL = setOf(
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.play.games",
            "com.android.vending",
            "com.google.android.inputmethod.latin",
            "com.qualcomm.location",
            "com.mediatek.location"
        )
    }
}
