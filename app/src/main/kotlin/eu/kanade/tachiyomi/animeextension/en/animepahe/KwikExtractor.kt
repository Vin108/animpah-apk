package eu.kanade.tachiyomi.animeextension.en.animepahe

import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import okhttp3.Headers
import okhttp3.OkHttpClient

class KwikExtractor(private val client: OkHttpClient) {

    fun extract(kwikUrl: String, quality: String): Video {
        return try {
            val html   = fetch(kwikUrl)
            val packed = findPacked(html)
            val m3u8   = packed?.let { JsUnpacker.unpack(it) }?.let { findM3u8(it) }
                ?: "error://no-video"
            val headers = Headers.Builder()
                .add("Referer", kwikUrl)
                .add("Origin", "https://kwik.si")
                .add("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
                .build()
            Video(m3u8, quality, m3u8, headers)
        } catch (e: Exception) {
            Video("error://${e.message}", quality, "error://")
        }
    }

    private fun fetch(url: String): String {
        val headers = Headers.Builder()
            .add("Referer", "https://animepahe.pw/")
            .add("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
            .build()
        return client.newCall(GET(url, headers)).execute().body!!.string()
    }

    private fun findPacked(html: String) =
        Regex("""(eval\(function\(p,a,c,k,e,[rd]\).*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)

    private fun findM3u8(unpacked: String) =
        Regex("""source='(https?://[^']+\.m3u8[^']*)'""").find(unpacked)?.groupValues?.get(1)
            ?: Regex("""file:"(https?://[^"]+\.m3u8[^"]*)" """).find(unpacked)?.groupValues?.get(1)
            ?: Regex("""(https?://\S+\.m3u8)""").find(unpacked)?.groupValues?.get(1)
            ?: "error://m3u8-not-found"
}

object JsUnpacker {
    private val RE = Regex(
        """}\('(.*?)',(\d+),(\d+),'(.*?)'\.split""",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    fun unpack(js: String): String? {
        val m    = RE.find(js) ?: return null
        val payload = m.groupValues[1]
        val base    = m.groupValues[2].toIntOrNull() ?: return null
        val count   = m.groupValues[3].toIntOrNull() ?: return null
        val dict    = m.groupValues[4].split("|")
        if (dict.size != count) return null
        val lookup = buildMap {
            for (i in 0 until count) {
                val k = i.toBase(base)
                put(k, dict[i].ifEmpty { k })
            }
        }
        return Regex("""\b(\w+)\b""").replace(payload) { lookup[it.value] ?: it.value }
    }

    private fun Int.toBase(base: Int): String {
        if (this == 0) return "0"
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var n = this; val sb = StringBuilder()
        while (n > 0) { sb.append(chars[n % base]); n /= base }
        return sb.reverse().toString()
    }
}
