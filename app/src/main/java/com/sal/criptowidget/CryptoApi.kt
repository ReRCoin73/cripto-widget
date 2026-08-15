package com.sal.criptowidget

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class CoinResult(
    val priceLabel: String,
    val changeLabel: String,
    val up: Boolean,
    val closes: List<Float>
)

data class CryptoData(
    val render: CoinResult?,
    val atom: CoinResult?,
    val ckb: CoinResult?
)

object CryptoApi {

    fun fetchAll(): CryptoData {
        return CryptoData(
            render = safe { fetchBinance("RENDERUSDT") },
            atom = safe { fetchBinance("ATOMUSDT") },
            ckb = safe { fetchBinance("CKBUSDT") }
        )
    }

    private fun safe(block: () -> CoinResult): CoinResult? =
        try { block() } catch (e: Exception) { null }

    private fun httpGet(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "CriptoWidget/1.0")
        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun fmtPrice(v: Double): String {
        val pattern = when {
            v >= 1000 -> "%,.0f"
            v >= 1 -> "%,.2f"
            v >= 0.01 -> "%,.4f"
            else -> "%,.6f"
        }
        return "US$ " + String.format(Locale("pt", "BR"), pattern, v)
    }

    private fun fetchBinance(symbol: String): CoinResult {
        val ticker = JSONObject(httpGet("https://api.binance.com/api/v3/ticker/24hr?symbol=$symbol"))
        val price = ticker.getString("lastPrice").toDouble()
        val changePct = ticker.getString("priceChangePercent").toDouble()
        val up = changePct >= 0

        val klinesRaw = JSONArray(
            httpGet("https://api.binance.com/api/v3/klines?symbol=$symbol&interval=1h&limit=24")
        )
        val closes = (0 until klinesRaw.length()).map {
            klinesRaw.getJSONArray(it).getString(4).toFloat()
        }

        val sign = if (up) "+" else ""
        return CoinResult(
            priceLabel = fmtPrice(price),
            changeLabel = "$sign${String.format(Locale.US, "%.2f", changePct)}%",
            up = up,
            closes = closes
        )
    }
}
