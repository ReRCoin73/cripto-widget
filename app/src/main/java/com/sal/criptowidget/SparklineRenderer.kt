package com.sal.criptowidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

object SparklineRenderer {

    private const val W = 300
    private const val H = 90

    fun render(values: List<Float>, up: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val min = values.min()
        val max = values.max()
        val range = if (max - min == 0f) 1f else max - min
        val stepX = W.toFloat() / (values.size - 1).coerceAtLeast(1)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = H - ((v - min) / range) * H
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val color = if (up) Color.parseColor("#3ECF6A") else Color.parseColor("#F0524B")
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
        }
        canvas.drawPath(path, linePaint)

        return bmp
    }
}
