package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tags: String,
    val bitrate: Int,
    val country: String = "Global",
    val isCurated: Boolean = false
)

class RadioRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api: RadioApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://de1.api.radio-browser.info/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RadioApiService::class.java)
    }

    val curatedStations = listOf(
        RadioStation(
            id = "curated_nightwave",
            name = "Nightwave Plaza",
            streamUrl = "https://plaza.one/mp3",
            tags = "Vaporwave, Synth, Future Funk",
            bitrate = 192,
            country = "Cyberdeck HQ",
            isCurated = true
        ),
        RadioStation(
            id = "curated_somafm_groovesalad",
            name = "SomaFM Groove Salad",
            streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
            tags = "Ambient, Downtempo, Chill",
            bitrate = 128,
            country = "USA",
            isCurated = true
        ),
        RadioStation(
            id = "curated_somafm_defcon",
            name = "SomaFM DEF CON Radio",
            streamUrl = "https://ice1.somafm.com/defcon-128-mp3",
            tags = "Cyberpunk, Industrial, Hacking",
            bitrate = 128,
            country = "USA",
            isCurated = true
        ),
        RadioStation(
            id = "curated_synthwave_radio",
            name = "Synthetix Radio 80s",
            streamUrl = "https://stream.zeno.fm/4wvy42wz44zuv",
            tags = "Synthwave, Retrowave, Outrun",
            bitrate = 192,
            country = "Global",
            isCurated = true
        ),
        RadioStation(
            id = "curated_somafm_dronezone",
            name = "SomaFM Drone Zone",
            streamUrl = "https://ice1.somafm.com/dronezone-128-mp3",
            tags = "Space, Ambient, Drone",
            bitrate = 128,
            country = "USA",
            isCurated = true
        ),
        RadioStation(
            id = "curated_chillhop",
            name = "Lofi & Chill Cyber Cafe",
            streamUrl = "https://stream.zeno.fm/f3wvbbqmdg8uv",
            tags = "Lo-Fi, Beats, Study, Chill",
            bitrate = 128,
            country = "Global",
            isCurated = true
        )
    )

    suspend fun getStations(tag: String? = null, query: String? = null): List<RadioStation> = withContext(Dispatchers.IO) {
        try {
            val dtoList = when {
                !query.isNullOrBlank() -> api.searchStations(name = query, limit = 25)
                !tag.isNullOrBlank() -> api.getStationsByTag(tag = tag.lowercase(), limit = 25)
                else -> api.getTopStations(limit = 25)
            }

            val mapped = dtoList
                .filter { it.name.isNotBlank() && (it.urlResolved.isNotBlank() || it.url.isNotBlank()) }
                .map { dto ->
                    RadioStation(
                        id = dto.stationUuid.ifBlank { dto.url },
                        name = dto.name.trim(),
                        streamUrl = dto.urlResolved.ifBlank { dto.url },
                        tags = dto.tags,
                        bitrate = if (dto.bitrate > 0) dto.bitrate else 128,
                        country = dto.country.ifBlank { "Global" },
                        isCurated = false
                    )
                }

            if (mapped.isNotEmpty()) {
                mapped
            } else {
                filterCurated(tag, query)
            }
        } catch (e: Exception) {
            filterCurated(tag, query)
        }
    }

    private fun filterCurated(tag: String?, query: String?): List<RadioStation> {
        return curatedStations.filter { station ->
            val matchTag = tag.isNullOrBlank() || station.tags.contains(tag, ignoreCase = true)
            val matchQuery = query.isNullOrBlank() || station.name.contains(query, ignoreCase = true) || station.tags.contains(query, ignoreCase = true)
            matchTag && matchQuery
        }
    }
}
