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

package org.breezyweather.sources.infoplaza.json

import kotlinx.serialization.Serializable

@Serializable
data class InfoplazaForecastHourly(
    val temperature: InfoplazaForecastTemperature?,
    val precipitation: Precipitation?,
    val intervalStart: InfoplazaForecastIntervalStart,
    val weatherSymbol: InfoplazaForecastWeatherSymbol?,
    val wind: Wind?,
) {
    @Serializable
    data class Precipitation(
        val amount: Double?,
        val probability: Double?,
    )

    @Serializable
    data class Wind(
        val direction: String?,
        val speed: Speed?,
    ) {
        @Serializable
        data class Speed(
            val ms: Double?,
        )
    }
}
