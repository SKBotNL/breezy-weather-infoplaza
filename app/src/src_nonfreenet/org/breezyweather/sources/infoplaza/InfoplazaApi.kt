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

import io.reactivex.rxjava3.core.Observable
import org.breezyweather.sources.infoplaza.json.InfoplazaAdviceResult
import org.breezyweather.sources.infoplaza.json.InfoplazaForecastDaily
import org.breezyweather.sources.infoplaza.json.InfoplazaForecastHourly
import org.breezyweather.sources.infoplaza.json.InfoplazaResult
import retrofit2.http.GET
import retrofit2.http.Query

interface InfoplazaApi {
    @GET("v3/en/forecast?interval=1H&offsetStart=0&offsetEnd=23")
    fun getHourly(
        @Query("geoAreaId") geoAreaId: String,
    ): Observable<InfoplazaResult<InfoplazaForecastHourly>>

    @GET("v3/en/forecast?interval=1D&offsetStart=0&offsetEnd=14")
    fun getDaily(
        @Query("geoAreaId") geoAreaId: String,
    ): Observable<InfoplazaResult<InfoplazaForecastDaily>>

    @GET("v3/advice")
    fun getAdvice(
        @Query("geoAreaId") geoAreaId: String,
    ): Observable<InfoplazaAdviceResult>
}
