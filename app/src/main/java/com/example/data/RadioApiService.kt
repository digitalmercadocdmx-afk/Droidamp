package com.example.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class RadioStationDto(
    @SerialName("stationuuid") val stationUuid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    @SerialName("homepage") val homepage: String = "",
    @SerialName("favicon") val favicon: String = "",
    @SerialName("tags") val tags: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("language") val language: String = "",
    @SerialName("bitrate") val bitrate: Int = 128,
    @SerialName("votes") val votes: Int = 0
)

interface RadioApiService {
    @GET("json/stations/topvote")
    suspend fun getTopStations(
        @Query("limit") limit: Int = 30
    ): List<RadioStationDto>

    @GET("json/stations/bytag/{tag}")
    suspend fun getStationsByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 30
    ): List<RadioStationDto>

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String,
        @Query("limit") limit: Int = 30
    ): List<RadioStationDto>
}
