package com.kylecorry.bell.infrastructure.persistence

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.kylecorry.bell.domain.Area
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant): Long {
        return value.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(value: Long): Instant {
        return Instant.ofEpochMilli(value)
    }

    @TypeConverter
    fun fromMap(map: Map<String, String>): String {
        return Gson().toJson(map)
    }

    @TypeConverter
    fun toMap(value: String): Map<String, String> {
        return Gson().fromJson(value, Map::class.java) as Map<String, String>
    }

    @TypeConverter
    fun fromArea(value: Area): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toArea(value: String): Area {
        return Gson().fromJson(value, Area::class.java)
    }
}