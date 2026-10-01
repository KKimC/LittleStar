package com.example.littlestar.data.local

import androidx.room.TypeConverter
import com.example.littlestar.domain.model.PracticeSessionStatus

class Converters {
    @TypeConverter fun statusToString(value: PracticeSessionStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): PracticeSessionStatus = PracticeSessionStatus.valueOf(value)
}
