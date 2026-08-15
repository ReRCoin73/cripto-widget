package com.sal.criptowidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class CryptoWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.sal.criptowidget.REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        scheduleRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            scheduleRefresh(context)
        }
    }

    override fun onEnabled(context: Context) {
        CryptoRefreshWorker.schedulePeriodic(context)
        scheduleRefresh(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CryptoRefreshWorker.PERIODIC_NAME)
    }

    private fun scheduleRefresh(context: Context) {
        val request = OneTimeWorkRequestBuilder<CryptoRefreshWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
