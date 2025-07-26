/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.sources.infoplaza

import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.AlertSeverity
import breezyweather.domain.weather.model.Minutely
import breezyweather.domain.weather.model.Pollen
import breezyweather.domain.weather.model.Precipitation
import breezyweather.domain.weather.model.PrecipitationProbability
import breezyweather.domain.weather.model.UV
import breezyweather.domain.weather.model.WeatherCode
import breezyweather.domain.weather.model.Wind
import breezyweather.domain.weather.wrappers.DailyWrapper
import breezyweather.domain.weather.wrappers.HalfDayWrapper
import breezyweather.domain.weather.wrappers.HourlyWrapper
import breezyweather.domain.weather.wrappers.PollenWrapper
import breezyweather.domain.weather.wrappers.TemperatureWrapper
import org.breezyweather.common.extensions.toDate
import org.breezyweather.sources.infoplaza.json.InfoplazaAdviceResult
import org.breezyweather.sources.infoplaza.json.InfoplazaForecastDaily
import org.breezyweather.sources.infoplaza.json.InfoplazaForecastHourly
import org.breezyweather.sources.infoplaza.json.InfoplazaForecastWeatherSymbol
import org.breezyweather.sources.infoplaza.json.InfoplazaNowcastTimeseries
import java.util.Objects
import kotlin.time.Duration.Companion.seconds

private val iconSignificance = listOf(
    WeatherCode.THUNDERSTORM,
    WeatherCode.THUNDER,
    WeatherCode.RAIN,
    WeatherCode.SNOW,
    WeatherCode.HAIL,
    WeatherCode.FOG,
    WeatherCode.WIND,
    WeatherCode.CLOUDY,
    WeatherCode.PARTLY_CLOUDY,
    WeatherCode.CLEAR
)

internal fun getDailyForecast(
    dailyResult: List<InfoplazaForecastDaily>?,
): List<DailyWrapper>? {
    return dailyResult?.zipWithNext { current, next ->
        DailyWrapper(
            date = current.intervalStart.millis.toDate(),
            day = HalfDayWrapper(
                weatherCode = listOfNotNull(
                    getWeatherCode(current.dayPart?.afternoon?.weatherSymbol),
                    getWeatherCode(current.dayPart?.evening?.weatherSymbol)
                ).minByOrNull { iconSignificance.indexOf(it) },
                temperature = TemperatureWrapper(
                    temperature = listOfNotNull(
                        current.dayPart?.afternoon?.temperature?.air,
                        current.dayPart?.evening?.temperature?.air
                    ).maxOrNull(),
                    feelsLike = listOfNotNull(
                        current.dayPart?.afternoon?.temperature?.feelsLike,
                        current.dayPart?.evening?.temperature?.feelsLike
                    ).maxOrNull()
                ),
                cloudCover = listOfNotNull(
                    getCloudCover(current.dayPart?.afternoon?.weatherSymbol),
                    getCloudCover(next.dayPart?.evening?.weatherSymbol)
                ).maxOrNull()
            ),
            night = HalfDayWrapper(
                weatherCode = listOfNotNull(
                    getWeatherCode(current.dayPart?.night?.weatherSymbol),
                    getWeatherCode(next.dayPart?.morning?.weatherSymbol)
                ).minByOrNull { iconSignificance.indexOf(it) },
                temperature = TemperatureWrapper(
                    temperature = listOfNotNull(
                        current.dayPart?.night?.temperature?.air,
                        next.dayPart?.morning?.temperature?.air
                    ).maxOrNull(),
                    feelsLike = listOfNotNull(
                        current.dayPart?.night?.temperature?.feelsLike,
                        next.dayPart?.morning?.temperature?.feelsLike
                    ).maxOrNull()
                ),
                cloudCover = listOfNotNull(
                    getCloudCover(current.dayPart?.night?.weatherSymbol),
                    getCloudCover(next.dayPart?.morning?.weatherSymbol)
                ).maxOrNull()
            ),
            uV = UV(index = current.digits?.health?.maxUvIndex),
            sunshineDuration = current.sunshine?.minutes?.div(60)
        )
    }
}

internal fun getHourlyForecast(
    hourlyResult: List<InfoplazaForecastHourly>?,
): List<HourlyWrapper>? {
    return hourlyResult?.map { result ->
        HourlyWrapper(
            date = result.intervalStart.millis.toDate(),
            weatherCode = getWeatherCode(result.weatherSymbol),
            temperature = TemperatureWrapper(
                temperature = result.temperature?.air,
                feelsLike = result.temperature?.feelsLike
            ),
            precipitation = Precipitation(
                total = result.precipitation?.amount,
            ),
            precipitationProbability = PrecipitationProbability(
                total = result.precipitation?.probability
            ),
            wind = Wind(
                degree = directionToBearing(result.wind?.direction),
                speed = result.wind?.speed?.ms,
            ),
            cloudCover = getCloudCover(result.weatherSymbol)
        )
    }
}

private fun directionToBearing(direction: String?): Double? {
    return when (direction) {
        "N" -> 0.0
        "NNO" -> 22.5
        "NO" -> 45.0
        "ONO" -> 67.5
        "O" -> 90.0
        "OZO" -> 112.5
        "ZO" -> 135.0
        "ZZO" -> 157.5
        "Z" -> 180.0
        "ZZW" -> 202.5
        "ZW" -> 225.0
        "WZW" -> 247.5
        "W" -> 270.0
        "WNW" -> 292.5
        "NW" -> 315.0
        "NNW" -> 337.5
        else -> null
    }
}

internal fun getMinutelyForecast(minutelyResult: List<InfoplazaNowcastTimeseries>): List<Minutely> {
    return minutelyResult.map { result ->
        Minutely(
            date = result.timestamp.seconds.inWholeMilliseconds.toDate(),
            minuteInterval = 5,
            precipitationIntensity = result.precipitationRate
        )
    }
}

internal fun getPollen(dailyResult: List<InfoplazaForecastDaily>?): PollenWrapper? {
    if (dailyResult == null) return null
    return PollenWrapper(
        dailyForecast = dailyResult.associate { result ->
            result.intervalStart.millis.toDate() to Pollen(
                alder = result.digits?.health?.hayFever?.trees?.alder,
                ash = result.digits?.health?.hayFever?.trees?.ash,
                birch = result.digits?.health?.hayFever?.trees?.birch,
                cypress = result.digits?.health?.hayFever?.trees?.cypress,
                grass = result.digits?.health?.hayFever?.grasses?.grasses,
                hazel = result.digits?.health?.hayFever?.trees?.hazel,
                oak = result.digits?.health?.hayFever?.trees?.oak,
                poplar = result.digits?.health?.hayFever?.trees?.poplar,
                willow = result.digits?.health?.hayFever?.trees?.willow
            )
        }
    )
}

internal fun getAlert(adviceResult: InfoplazaAdviceResult): List<Alert> {
    val severity = when (adviceResult.animationId) {
        5 -> AlertSeverity.MINOR
        6 -> AlertSeverity.SEVERE
        7 -> AlertSeverity.EXTREME
        else -> return emptyList()
    }
    return listOf(
        Alert(
            alertId = Objects.hash(adviceResult.message, adviceResult.fullText, adviceResult.animationId).toString(),
            headline = adviceResult.message,
            description = adviceResult.fullText,
            severity = severity,
            color = Alert.colorFromSeverity(severity)
        )
    )
}

private fun getWeatherCode(result: InfoplazaForecastWeatherSymbol?): WeatherCode? {
    if (result == null) return null
    if (result.thunder == true) {
        return when (result.precipitation?.amount) {
            "NoDrops" -> WeatherCode.THUNDER
            else -> WeatherCode.THUNDERSTORM
        }
    }
    if (result.precipitation?.amount != "NoDrops" && result.precipitation?.kind == "Rain") {
        when (result.precipitation.kind) {
            "Rain" -> return WeatherCode.RAIN
            "Snow" -> return WeatherCode.SNOW
            "FrozenRain" -> return WeatherCode.HAIL
            "Drizzle" -> return WeatherCode.RAIN
            else -> {}
        }
    }
    if (result.fogPresence != "NoFog") {
        return WeatherCode.FOG
    }
    if (result.windGusts == true) {
        return WeatherCode.WIND
    }
    return when (result.cloudCoverage) {
        "Overcast" -> WeatherCode.CLOUDY
        "HeavilyClouded" -> WeatherCode.CLOUDY
        "HalfClouded" -> WeatherCode.PARTLY_CLOUDY
        "VeilClouded" -> WeatherCode.PARTLY_CLOUDY
        "LightClouded" -> WeatherCode.PARTLY_CLOUDY
        "Unclouded" -> WeatherCode.CLEAR
        else -> null
    }
}

private fun getCloudCover(result: InfoplazaForecastWeatherSymbol?): Int? {
    return when (result?.cloudCoverage) {
        "Overcast" -> 100
        "HeavilyClouded" -> 75
        "HalfClouded" -> 50
        "VeilClouded" -> 38
        "LightClouded" -> 16
        "Unclouded" -> 0
        else -> null
    }
}
