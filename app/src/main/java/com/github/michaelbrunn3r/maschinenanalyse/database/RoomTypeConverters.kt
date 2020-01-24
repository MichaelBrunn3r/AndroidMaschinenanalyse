package com.github.michaelbrunn3r.maschinenanalyse.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Collections.emptyList


class RoomTypeConverters {
    val gson = Gson()

    @TypeConverter
    fun jsonStringToList(data:String): List<Float> {
        if(data == null) {
            return emptyList()
        }

        return gson.fromJson(data, object : TypeToken<List<Float>>() {}.type)
    }

    @TypeConverter
    fun listToJsonString(list:List<Float>): String {
        return gson.toJson(list)
    }
}