package com.oneui.calendar.data.weather

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.oneui.calendar.data.local.CalendarDatabaseHelper
import com.oneui.calendar.domain.model.DailyMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class OpenMeteoWeatherService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    private data class OpenMeteoResponse(
        @SerializedName("daily")
        val daily: DailyForecastPayload? = null
    )

    private data class DailyForecastPayload(
        @SerializedName("time")
        val time: List<String> = emptyList(),
        @SerializedName("weathercode")
        val weatherCode: List<Int> = emptyList(),
        @SerializedName("temperature_2m_max")
        val tempMax: List<Double> = emptyList(),
        @SerializedName("temperature_2m_min")
        val tempMin: List<Double> = emptyList()
    )

    suspend fun fetch16DayForecast(
        lat: Double = DEFAULT_LATITUDE,
        lon: Double = DEFAULT_LONGITUDE
    ): List<DailyMetadata> = withContext(Dispatchers.IO) {
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=16"
        val request = Request.Builder().url(url).get().build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string() ?: return@use emptyList()
                val parsed = gson.fromJson(body, OpenMeteoResponse::class.java)
                val daily = parsed?.daily ?: return@use emptyList()
                val count = minOf(
                    daily.time.size,
                    daily.weatherCode.size,
                    daily.tempMax.size,
                    daily.tempMin.size
                )
                (0 until count).map { idx ->
                    val code = daily.weatherCode[idx]
                    DailyMetadata(
                        dateIso = daily.time[idx],
                        stickerId = null,
                        tempHighC = daily.tempMax[idx].roundToInt(),
                        tempLowC = daily.tempMin[idx].roundToInt(),
                        weatherCode = code,
                        weatherEmoji = mapWeatherCodeToEmoji(code)
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    suspend fun fetchAndCacheForecast(
        dbHelper: CalendarDatabaseHelper,
        lat: Double = DEFAULT_LATITUDE,
        lon: Double = DEFAULT_LONGITUDE
    ): List<DailyMetadata> = withContext(Dispatchers.IO) {
        val fetched = fetch16DayForecast(lat = lat, lon = lon)
        if (fetched.isNotEmpty()) {
            dbHelper.upsertDailyMetadataBatch(fetched)
            return@withContext fetched
        }

        val cached = dbHelper.getAllDailyMetadata().values.toList()
        if (cached.isNotEmpty()) {
            return@withContext cached
        }

        listOf(fallbackMetadata(LocalDate.now()))
    }

    fun fallbackMetadata(date: LocalDate): DailyMetadata {
        return DailyMetadata(
            dateIso = date.toString(),
            stickerId = null,
            tempHighC = 12,
            tempLowC = 8,
            weatherCode = 0,
            weatherEmoji = "☀️"
        )
    }

    fun mapWeatherCodeToEmoji(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "☀️"
            in 1..3 -> "⛅"
            in 45..48 -> "🌫️"
            in 51..67 -> "🌧️"
            in 71..77 -> "❄️"
            in 80..82 -> "🌦️"
            in 95..99 -> "⛈️"
            else -> "☀️"
        }
    }

    companion object {
        private const val DEFAULT_LATITUDE = 10.8231
        private const val DEFAULT_LONGITUDE = 106.6297
    }
}
