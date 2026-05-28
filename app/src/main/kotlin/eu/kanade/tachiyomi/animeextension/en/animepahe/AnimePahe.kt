package eu.kanade.tachiyomi.animeextension.en.animepahe

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.*
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.json.*
import okhttp3.*
import org.jsoup.Jsoup
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

class AnimePahe : AnimeHttpSource(), ConfigurableAnimeSource {

    override val name        = "AnimePahe"
    override val baseUrl     = "https://animepahe.pw"
    override val lang        = "en"
    override val supportsLatest = true

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
    private val prefs by lazy { Injekt.get<Application>().getSharedPreferences("source_$id", 0) }
    private val kwik  by lazy { KwikExtractor(client) }

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Cookie", "__ddg1_=;__ddg2_=;")
        .add("User-Agent", "Mozilla/5.0 (Linux; Android 13; K) AppleWebKit/537.36 Chrome/124.0.0.0")

    override fun popularAnimeRequest(page: Int) = GET("$baseUrl/api?m=airing&page=$page", headers)
    override fun popularAnimeParse(response: Response) = parseListPage(response)

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/api?m=airing&page=$page", headers)
    override fun latestUpdatesParse(response: Response) = parseListPage(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return if (query.isNotBlank())
            GET("$baseUrl/api?m=search&q=${query.trim().replace(" ", "+")}", headers)
        else
            GET("$baseUrl/api?m=airing&page=$page", headers)
    }
    override fun searchAnimeParse(response: Response): AnimesPage {
        val url = response.request.url.toString()
        return if (url.contains("m=search")) parseSearchPage(response) else parseListPage(response)
    }

    override fun animeDetailsRequest(anime: SAnime) = GET(anime.url.toAbs(), headers)
    override fun animeDetailsParse(response: Response): SAnime {
        val doc = Jsoup.parse(response.body.string())
        return SAnime.create().apply {
            title         = doc.selectFirst("h1.title-english, .anime-title")?.text() ?: doc.title()
            thumbnail_url = doc.selectFirst("div.anime-cover img, .anime-poster img")?.attr("data-src")
                ?: doc.selectFirst("div.anime-cover img")?.attr("src")
            description   = doc.selectFirst("div.anime-synopsis p")?.text()
            genre         = doc.select("div.anime-genre a").joinToString(", ") { it.text() }
            status        = when (doc.selectFirst("p.type span")?.text()?.lowercase()) {
                "currently airing" -> SAnime.ONGOING
                "finished airing"  -> SAnime.COMPLETED
                else               -> SAnime.UNKNOWN
            }
        }
    }

    override fun episodeListRequest(anime: SAnime) = GET(anime.url.toAbs(), headers)
    override fun episodeListParse(response: Response): List<SEpisode> {
        val session = extractSession(response.body.string()) ?: return emptyList()
        val all = mutableListOf<SEpisode>()
        var page = 1
        while (true) {
            val resp = client.newCall(GET("$baseUrl/api?m=release&id=$session&sort=episode_asc&page=$page", headers)).execute()
            val root = json.parseToJsonElement(resp.body.string()).jsonObject
            val data = root["data"]?.jsonArray ?: break
            for (item in data) {
                val ep   = item.jsonObject
                val num  = ep["episode"]?.jsonPrimitive?.floatOrNull ?: continue
                val sess = ep["session"]?.jsonPrimitive?.content     ?: continue
                val fill = ep["filler"]?.jsonPrimitive?.intOrNull == 1
                all += SEpisode.create().apply {
                    episode_number = num
                    name           = if (fill) "Episode ${num.toInt()} (Filler)" else "Episode ${num.toInt()}"
                    url            = "/play/$session/$sess"
                    scanlator      = if (fill) "Filler" else null
                    date_upload    = parseDate(ep["created_at"]?.jsonPrimitive?.content)
                }
            }
            if (page >= (root["last_page"]?.jsonPrimitive?.intOrNull ?: 1)) break
            page++
        }
        return all.sortedByDescending { it.episode_number }
    }

    override fun videoListRequest(episode: SEpisode) = GET(episode.url.toAbs(), headers)
    override fun videoListParse(response: Response): List<Video> {
        val segs = response.request.url.pathSegments
        val anS  = segs.getOrNull(segs.size - 2) ?: return emptyList()
        val epS  = segs.lastOrNull()              ?: return emptyList()
        val resp = client.newCall(GET("$baseUrl/api?m=links&id=$anS&session=$epS&p=kwik", headers)).execute()
        val root = json.parseToJsonElement(resp.body.string()).jsonObject
        val data = root["data"] ?: return emptyList()
        val videos = mutableListOf<Video>()
        data.jsonObject.values.forEach { v ->
            val obj     = v.jsonObject
            val kwikUrl = obj["kwik"]?.jsonPrimitive?.content    ?: return@forEach
            val quality = obj["quality"]?.jsonPrimitive?.content ?: "?"
            val audio   = obj["audio"]?.jsonPrimitive?.content   ?: "jpn"
            videos += kwik.extract(kwikUrl, "${quality}p [$audio]")
        }
        val pref = prefs.getString("quality", "720") ?: "720"
        return videos.sortedWith(compareByDescending { it.quality.contains(pref) })
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = "quality"; title = "Preferred quality"
            entries = arrayOf("1080p","720p","480p","360p")
            entryValues = arrayOf("1080","720","480","360")
            setDefaultValue("720")
            screen.addPreference(this)
        }
    }

    private fun parseListPage(r: Response): AnimesPage {
        val root = json.parseToJsonElement(r.body.string()).jsonObject
        val data = root["data"]?.jsonArray ?: return AnimesPage(emptyList(), false)
        val cur  = root["current_page"]?.jsonPrimitive?.intOrNull ?: 1
        val last = root["last_page"]?.jsonPrimitive?.intOrNull ?: 1
        return AnimesPage(data.map { it.toAnime() }, cur < last)
    }
    private fun parseSearchPage(r: Response): AnimesPage {
        val root = json.parseToJsonElement(r.body.string()).jsonObject
        val data = root["data"]?.jsonArray ?: return AnimesPage(emptyList(), false)
        return AnimesPage(data.map { it.toAnime() }, false)
    }
    private fun JsonElement.toAnime() = SAnime.create().apply {
        val o = jsonObject
        title         = o["title"]?.jsonPrimitive?.content ?: "Unknown"
        thumbnail_url = o["poster"]?.jsonPrimitive?.content ?: o["image"]?.jsonPrimitive?.content
        url           = "/anime/${o["session"]?.jsonPrimitive?.content}"
    }
    private fun extractSession(html: String): String? =
        Regex("""let\s+animeId\s*=\s*"([a-f0-9-]+)"""").find(html)?.groupValues?.get(1)
        ?: Regex("""data-id="([a-f0-9-]+)"""").find(html)?.groupValues?.get(1)
        ?: Regex(""""session"\s*:\s*"([a-f0-9-]+)"""").find(html)?.groupValues?.get(1)
    private fun parseDate(s: String?) = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(s!!)!!.time
    }.getOrDefault(0L)
    private fun String.toAbs() = if (startsWith("http")) this else "$baseUrl$this"
                 }
