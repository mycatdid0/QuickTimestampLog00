package kr.mycatdid0.quicktimestamplog00.data

import android.content.Context
import org.json.JSONArray

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getDefaultCalendarId(): Long? = preferences.getLong(KEY_DEFAULT_CALENDAR_ID, NO_CALENDAR)
        .takeIf { it != NO_CALENDAR }

    fun setDefaultCalendarId(calendarId: Long) {
        preferences.edit().putLong(KEY_DEFAULT_CALENDAR_ID, calendarId).apply()
    }

    fun getRecentTitles(): List<String> {
        val encoded = preferences.getString(KEY_RECENT_TITLES, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(encoded)
            List(json.length()) { index -> json.getString(index) }
                .filter(String::isNotBlank)
        }.getOrDefault(emptyList())
    }

    fun addRecentTitle(title: String) {
        val updated = RecentTitleHistory.add(getRecentTitles(), title)
        preferences.edit().putString(KEY_RECENT_TITLES, JSONArray(updated).toString()).apply()
    }

    fun removeRecentTitle(title: String) {
        val updated = RecentTitleHistory.remove(getRecentTitles(), title)
        preferences.edit().putString(KEY_RECENT_TITLES, JSONArray(updated).toString()).apply()
    }

    fun clearRecentTitles() {
        preferences.edit().remove(KEY_RECENT_TITLES).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "quick_timestamp_preferences"
        const val KEY_DEFAULT_CALENDAR_ID = "default_calendar_id"
        const val KEY_RECENT_TITLES = "recent_titles"
        const val NO_CALENDAR = -1L
    }
}

object RecentTitleHistory {
    const val MAX_TITLES = 30

    fun add(titles: List<String>, title: String): List<String> {
        val normalized = title.trim()
        if (normalized.isEmpty()) return titles
        return (listOf(normalized) + titles.filterNot { it.equals(normalized, ignoreCase = true) })
            .take(MAX_TITLES)
    }

    fun remove(titles: List<String>, title: String): List<String> =
        titles.filterNot { it == title }
}
