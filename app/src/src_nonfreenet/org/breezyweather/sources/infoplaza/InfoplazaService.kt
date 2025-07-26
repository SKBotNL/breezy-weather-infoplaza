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

import android.content.Context
import breezyweather.domain.location.model.Location
import breezyweather.domain.source.SourceContinent
import breezyweather.domain.source.SourceFeature
import breezyweather.domain.weather.wrappers.WeatherWrapper
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.common.exceptions.InvalidLocationException
import org.breezyweather.common.source.HttpSource
import org.breezyweather.common.source.LocationParametersSource
import org.breezyweather.common.source.WeatherSource
import org.breezyweather.sources.infoplaza.json.InfoplazaAdviceResult
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

class InfoplazaService @Inject constructor(
    @Named("JsonClient") client: Retrofit.Builder,
) : HttpSource(), WeatherSource, LocationParametersSource {
    override fun needsLocationParametersRefresh(
        location: Location,
        coordinatesChanged: Boolean,
        features: List<SourceFeature>,
    ): Boolean {
        if (SourceFeature.FORECAST !in features && SourceFeature.POLLEN !in features && SourceFeature.ALERT !in features) return false
        return coordinatesChanged
    }

    override fun requestLocationParameters(context: Context, location: Location): Observable<Map<String, String>> {
        return mSearchApi.search(location.city + " " + location.country).map { list ->
            mapOf(
                "geoAreaId" to list.firstOrNull { it.locationId != null }?.locationId.toString()
            )
        }
    }

    override val id = "infoplaza"
    override val name = "Infoplaza"
    override val continent = SourceContinent.WORLDWIDE
    override val privacyPolicyUrl = "https://www.infoplaza.com/en/disclaimer-and-privacy"

    private val mApi by lazy {
        client
            .baseUrl(INFOPLAZA_BASE_URL)
            .build()
            .create(InfoplazaApi::class.java)
    }

    private val mNowcastApi by lazy {
        client
            .baseUrl(INFOPLAZA_NOWCAST_URL)
            .build()
            .create(InfoplazaNowcastApi::class.java)
    }

    private val mSearchApi by lazy {
        client
            .baseUrl(INFOPLAZA_SEARCH_URL)
            .build()
            .create(InfoplazaSearchApi::class.java)
    }

    override val supportedFeatures = mapOf(
        SourceFeature.FORECAST to name,
        SourceFeature.MINUTELY to name,
        SourceFeature.POLLEN to name,
        SourceFeature.ALERT to name
    )

    override val attributionLinks = mapOf(
        name to "https://infoplaza.com/"
    )

    override fun requestWeather(
        context: Context,
        location: Location,
        requestedFeatures: List<SourceFeature>,
    ): Observable<WeatherWrapper> {
        val geoAreaId = location.parameters.getOrElse(id) { null }?.getOrElse("geoAreaId") { null }

        if ((SourceFeature.FORECAST in requestedFeatures ||
                SourceFeature.POLLEN in requestedFeatures ||
                SourceFeature.ALERT in requestedFeatures)
            && geoAreaId.isNullOrEmpty()
        ) {
            return Observable.error(InvalidLocationException())
        }

        val hourly = if (SourceFeature.FORECAST in requestedFeatures) {
            mApi.getHourly(
                geoAreaId!!
            ).map {
                it.data
            }
        } else {
            Observable.just(emptyList())
        }

        val daily = if (SourceFeature.FORECAST in requestedFeatures || SourceFeature.POLLEN in requestedFeatures) {
            mApi.getDaily(
                geoAreaId!!
            ).map {
                it.data
            }
        } else {
            Observable.just(emptyList())
        }

        val minutely = if (SourceFeature.MINUTELY in requestedFeatures) {
            mNowcastApi.getTimeseries(
                location.latitude,
                location.longitude
            ).map {
                it.data
            }
        } else {
            Observable.just(emptyList())
        }

        val advice = if (SourceFeature.ALERT in requestedFeatures) {
            mApi.getAdvice(
                geoAreaId!!
            )
        } else {
            Observable.just(InfoplazaAdviceResult())
        }

        return Observable.zip(
            hourly,
            daily,
            minutely,
            advice,
        ) {
                hourlyResults,
                dailyResults,
                minutelyResults,
                adviceResults,
            ->
            WeatherWrapper(
                hourlyForecast = if (SourceFeature.FORECAST in requestedFeatures) {
                    getHourlyForecast(hourlyResults)
                } else {
                    null
                },
                dailyForecast = if (SourceFeature.FORECAST in requestedFeatures) {
                    getDailyForecast(dailyResults)
                } else {
                    null
                },
                minutelyForecast = if (SourceFeature.MINUTELY in requestedFeatures) {
                    getMinutelyForecast(minutelyResults)
                } else {
                    null
                },
                pollen = if (SourceFeature.POLLEN in requestedFeatures) {
                    getPollen(dailyResults)
                } else {
                    null
                },
                alertList = if (SourceFeature.ALERT in requestedFeatures) {
                    getAlert(adviceResults)
                } else {
                    null
                }
            )
        }
    }

    override val testingLocations: List<Location> = emptyList()

    companion object {
        private const val INFOPLAZA_BASE_URL = "https://api.meteoplaza.com/"
        private const val INFOPLAZA_NOWCAST_URL = "https://imn-rust-lb.infoplaza.io/"
        private const val INFOPLAZA_SEARCH_URL = "https://geosearch.meteoplaza.com/"
    }
}
