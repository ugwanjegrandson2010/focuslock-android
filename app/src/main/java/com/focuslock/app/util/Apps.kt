package com.focuslock.app.util

import android.content.Context
import android.content.Intent
import com.focuslock.app.domain.AppInfo

object Apps {
    fun launchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return try {
            pm.queryIntentActivities(intent, 0)
                .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .distinctBy { it.packageName }
                .sortedBy { it.label }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
