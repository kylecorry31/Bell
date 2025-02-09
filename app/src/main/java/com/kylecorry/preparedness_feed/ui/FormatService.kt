package com.kylecorry.preparedness_feed.ui

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import com.kylecorry.andromeda.markdown.MarkdownService
import java.time.ZonedDateTime


class FormatService private constructor(private val context: Context) {

    private val markdown = MarkdownService(context)

    fun formatDate(
        date: ZonedDateTime,
        includeWeekDay: Boolean = true,
        abbreviateMonth: Boolean = false
    ): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            DateUtils.FORMAT_SHOW_DATE or (if (includeWeekDay) DateUtils.FORMAT_SHOW_WEEKDAY else 0) or DateUtils.FORMAT_SHOW_YEAR or (if (abbreviateMonth) DateUtils.FORMAT_ABBREV_MONTH else 0)
        )
    }

    fun formatDateTime(
        date: ZonedDateTime,
        includeWeekDay: Boolean = true,
        abbreviateMonth: Boolean = false
    ): String {
        return DateUtils.formatDateTime(
            context,
            date.toEpochSecond() * 1000,
            DateUtils.FORMAT_SHOW_DATE or (if (includeWeekDay) DateUtils.FORMAT_SHOW_WEEKDAY else 0) or DateUtils.FORMAT_SHOW_YEAR or (if (abbreviateMonth) DateUtils.FORMAT_ABBREV_MONTH else 0) or DateUtils.FORMAT_SHOW_TIME
        )
    }

    fun formatMarkdown(markdownText: String): CharSequence {
        return markdown.toMarkdown(markdownText)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: FormatService? = null

        @Synchronized
        fun getInstance(context: Context): FormatService {
            if (instance == null) {
                instance = FormatService(context.applicationContext)
            }
            return instance!!
        }
    }
}