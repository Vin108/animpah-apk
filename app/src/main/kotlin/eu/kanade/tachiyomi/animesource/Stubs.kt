package eu.kanade.tachiyomi.animesource.model

import okhttp3.*

data class AnimesPage(val animes: List<SAnime>, val hasNextPage: Boolean)
class AnimeFilterList

interface SAnime {
    var title: String; var thumbnail_url: String?; var url: String
    var description: String?; var genre: String?; var status: Int
    companion object {
        const val UNKNOWN = 0; const val ONGOING = 1; const val COMPLETED = 2
        fun create(): SAnime = object : SAnime {
            override var title = ""; override var thumbnail_url: String? = null
            override var url = ""; override var description: String? = null
            override var genre: String? = null; override var status = UNKNOWN
        }
    }
}

interface SEpisode {
    var url: String; var name: String; var date_upload: Long
    var episode_number: Float; var scanlator: String?
    companion object {
        fun create(): SEpisode = object : SEpisode {
            override var url = ""; override var name = ""
            override var date_upload = 0L; override var episode_number = -1f
            override var scanlator: String? = null
        }
    }
}

class Video(val url: String, val quality: String, val videoUrl: String?, val headers: Headers? = null)

// ── Source base classes ───────────────────────────────────────────────────────
package eu.kanade.tachiyomi.animesource

import androidx.preference.PreferenceScreen
interface ConfigurableAnimeSource { fun setupPreferenceScreen(screen: PreferenceScreen) }

package eu.kanade.tachiyomi.animesource.online
import eu.kanade.tachiyomi.animesource.model.*
import okhttp3.*
import java.util.concurrent.TimeUnit

abstract class AnimeHttpSource {
    abstract val name: String; abstract val baseUrl: String
    abstract val lang: String; open val supportsLatest = false
    open val id: Long get() = (name + lang).hashCode().toLong()
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
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
