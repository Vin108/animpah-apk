package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.model.*
import okhttp3.*
import java.util.concurrent.TimeUnit

abstract class AnimeHttpSource {
    abstract val name: String
    abstract val baseUrl: String
    abstract val lang: String
    open val supportsLatest: Boolean = false
    open val id: Long get() = (name + lang).hashCode().toLong()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    open fun headersBuilder(): Headers.Builder = Headers.Builder()
    val headers: Headers get() = headersBuilder().build()

    abstract fun popularAnimeRequest(page: Int): Request
    abstract fun popularAnimeParse(response: Response): AnimesPage
    abstract fun latestUpdatesRequest(page: Int): Request
    abstract fun latestUpdatesParse(response: Response): AnimesPage
    abstract fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request
    abstract fun searchAnimeParse(response: Response): AnimesPage
    abstract fun animeDetailsRequest(anime: SAnime): Request
    abstract fun animeDetailsParse(response: Response): SAnime
    abstract fun episodeListRequest(anime: SAnime): Request
    abstract fun episodeListParse(response: Response): List<SEpisode>
    abstract fun videoListRequest(episode: SEpisode): Request
    abstract fun videoListParse(response: Response): List<Video>
}
