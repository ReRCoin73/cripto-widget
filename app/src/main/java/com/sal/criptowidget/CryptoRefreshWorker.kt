package com.sal.criptowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CryptoRefreshWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val PERIODIC_NAME = "crypto_widget_periodic"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<CryptoRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        // Escolhe o layout de acordo com a largura atual do widget nesse aparelho
        private fun chooseLayout(minWidthDp: Int): Int = when {
            minWidthDp >= 260 -> R.layout.crypto_widget_row    // 3 lado a lado
            minWidthDp >= 170 -> R.layout.crypto_widget_2x2    // 2 em cima, 1 embaixo
            else -> R.layout.crypto_widget_stack               // empilhado
        }
    }

    override suspend fun doWork(): Result {
        val data = try {
            CryptoApi.fetchAll()
        } catch (e: Exception) {
            return Result.retry()
        }

        val context = applicationContext
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CryptoWidgetProvider::class.java))

        for (id in ids) {
            val options = manager.getAppWidgetOptions(id)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
            val layoutRes = chooseLayout(minWidthDp)

            val views = RemoteViews(context.packageName, layoutRes)
            bindCoin(views, data.render, R.id.price_render, R.id.chg_render, R.id.spark_render)
            bindCoin(views, data.atom, R.id.price_atom, R.id.chg_atom, R.id.spark_atom)
            bindCoin(views, data.ckb, R.id.price_ckb, R.id.chg_ckb, R.id.spark_ckb)

            val refreshIntent = Intent(context, CryptoWidgetProvider::class.java).apply {
                action = CryptoWidgetProvider.ACTION_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(id, views)
        }
        return Result.success()
    }

    private fun bindCoin(
        views: RemoteViews,
        coin: CoinResult?,
        priceId: Int,
        chgId: Int,
        sparkId: Int
    ) {
        if (coin == null) {
            views.setTextViewText(priceId, "erro")
            views.setTextViewText(chgId, "")
            return
        }
        views.setTextViewText(priceId, coin.priceLabel)
        views.setTextViewText(chgId, coin.changeLabel)
        val color = if (coin.up) 0xFF3ECF6A.toInt() else 0xFFF0524B.toInt()
        views.setTextColor(chgId, color)
        if (coin.closes.size >= 2) {
            val bmp = SparklineRenderer.render(coin.closes, coin.up)
            views.setImageViewBitmap(sparkId, bmp)
        }
    }
}
