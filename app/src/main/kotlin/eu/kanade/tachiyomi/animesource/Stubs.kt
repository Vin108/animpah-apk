package eu.kanade.tachiyomi.animesource.model

import okhttp3.Headers

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
