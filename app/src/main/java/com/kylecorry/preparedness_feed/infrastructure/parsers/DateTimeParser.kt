package com.kylecorry.preparedness_feed.infrastructure.parsers

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object DateTimeParser {

    fun parse(dateString: String, defaultZone: ZoneId = ZoneId.systemDefault()): ZonedDateTime? {
        val parsed = recursiveParseZonedDateTime(dateString)
            ?: recursiveParseLocalDateTime(dateString)?.atZone(defaultZone)
            ?: recursiveParseLocalDate(dateString)?.atStartOfDay(
                defaultZone
            )

        if (parsed != null) {
            return parsed
        }

        return null
    }

    private fun recursiveParseLocalDateTime(dateString: String): LocalDateTime? {
        if (dateString.isEmpty()) {
            return null
        }

        val localDateTime = parseLocalDateTime(dateString)
        if (localDateTime != null) {
            return localDateTime
        }

        // One path removes characters from the front of the string
        // The other path removes characters from the back of the string
        val front = recursiveParseLocalDateTime(dateString.substring(1))
        if (front != null) {
            return front
        }

        return null
//        return recursiveParseLocalDateTime(dateString.substring(0, dateString.length - 1))
    }

    private fun recursiveParseLocalDate(dateString: String): LocalDate? {
        if (dateString.isEmpty()) {
            return null
        }

        val localDateTime = parseLocalDate(dateString)
        if (localDateTime != null) {
            return localDateTime
        }

        // One path removes characters from the front of the string
        // The other path removes characters from the back of the string
        val front = recursiveParseLocalDate(dateString.substring(1))
        if (front != null) {
            return front
        }

        return null
//        return recursiveParseLocalDate(dateString.substring(0, dateString.length - 1))
    }

    private fun recursiveParseZonedDateTime(dateString: String): ZonedDateTime? {
        if (dateString.isEmpty()) {
            return null
        }

        val zonedDateTime = parseZonedDateTime(dateString)
        if (zonedDateTime != null) {
            return zonedDateTime
        }

        // One path removes characters from the front of the string
        // The other path removes characters from the back of the string
        val front = recursiveParseZonedDateTime(dateString.substring(1))
        if (front != null) {
            return front
        }

        return null
//        return recursiveParseZonedDateTime(dateString.substring(0, dateString.length - 1))
    }

    private fun parseZonedDateTime(dateString: String): ZonedDateTime? {
        val patterns = listOf(
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss z")
        )

        for (pattern in patterns) {
            try {
                return ZonedDateTime.parse(dateString, pattern)
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    private fun parseLocalDate(dateString: String): LocalDate? {
        val patterns = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE
        )

        for (pattern in patterns) {
            try {
                return LocalDate.parse(dateString, pattern)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun parseLocalDateTime(dateString: String): LocalDateTime? {
        val patterns = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy, h:mm a")
        )

        for (pattern in patterns) {
            try {
                return LocalDateTime.parse(dateString, pattern)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
}