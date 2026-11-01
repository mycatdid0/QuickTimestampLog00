package kr.mycatdid0.quicktimestamplog00.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

 data class CalendarOption(
    val id: Long,
    val name: String,
    val accountName: String,
    val isPrimary: Boolean
) {
    val displayName: String
        get() = if (accountName.isBlank()) name else "$name · $accountName"
}

object CalendarRepository {
    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun hasWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    fun hasCalendarPermissions(context: Context): Boolean =
        hasReadPermission(context) && hasWritePermission(context)

    fun loadWritableCalendars(context: Context): List<CalendarOption> {
        if (!hasReadPermission(context)) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()
        )

        return context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, " +
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE ASC"
        )?.use { cursor ->
            buildList {
                val idColumn = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
                )
                val accountColumn = cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars.ACCOUNT_NAME
                )
                val primaryColumn = cursor.getColumnIndexOrThrow(
                    CalendarContract.Calendars.IS_PRIMARY
                )
                while (cursor.moveToNext()) {
                    add(
                        CalendarOption(
                            id = cursor.getLong(idColumn),
                            name = cursor.getString(nameColumn).orEmpty(),
                            accountName = cursor.getString(accountColumn).orEmpty(),
                            isPrimary = cursor.getInt(primaryColumn) == 1
                        )
                    )
                }
            }
        }.orEmpty()
    }

    fun insertEvent(
        context: Context,
        calendarId: Long,
        title: String,
        startMillis: Long,
        endMillis: Long
    ) {
        check(hasWritePermission(context)) { "Calendar write permission is not granted" }
        require(title.isNotBlank()) { "Event title must not be blank" }
        require(endMillis >= startMillis) { "Event end time must not precede its start time" }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title.trim())
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        checkNotNull(context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)) {
            "Calendar provider did not create the event"
        }
    }
}
