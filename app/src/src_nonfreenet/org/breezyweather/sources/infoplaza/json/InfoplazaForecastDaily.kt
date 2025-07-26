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
data class InfoplazaForecastDaily(
    val dayPart: Dayparts?,
    val intervalStart: InfoplazaForecastIntervalStart,
    val digits: Digits?,
    val sunshine: Sunshine?,
) {
    @Serializable
    data class Dayparts(
        val night: Daypart?,
        val morning: Daypart?,
        val afternoon: Daypart?,
        val evening: Daypart?,
    ) {
        @Serializable
        data class Daypart(
            val temperature: InfoplazaForecastTemperature?,
            val weatherSymbol: InfoplazaForecastWeatherSymbol?,
            val wind: Wind?,
        ) {
            @Serializable
            data class Wind(
                val direction: String?,
                val maxWindGust: Double?,
                val maxWindSpeedMs: Double?,
            )
        }
    }

    @Serializable
    data class Digits(
        val health: Health?,
    ) {
        @Serializable
        data class Health(
            val hayFever: HayFever?,
            val maxUvIndex: Double?,
        ) {
            @Serializable
            data class HayFever(
                val grasses: Grasses?,
                val trees: Trees?,
            ) {
                @Serializable
                data class Grasses(
                    val grasses: Int?,
                )

                @Serializable
                data class Trees(
                    val alder: Int?,
                    val ash: Int?,
                    val birch: Int?,
                    val cypress: Int?,
                    val hazel: Int?,
                    val oak: Int?,
                    val poplar: Int?,
                    val willow: Int?,
                )
            }
        }
    }

    @Serializable
    data class Sunshine(
        val minutes: Double?,
    )
}
