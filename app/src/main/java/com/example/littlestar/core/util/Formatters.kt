package com.example.littlestar.core.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 초 단위를 "mm:ss" 형태로 변환 (예: 75 -> "01:15") */
fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
}

/** 분 단위를 "1시간 20분" 형태의 읽기 쉬운 문자열로 변환 */
fun formatMinutes(minutes: Int): String = when {
    minutes >= 60 -> {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "${h}시간" else "${h}시간 ${m}분"
    }
    else -> "${minutes}분"
}

/** 초 단위를 "HH:MM" 형태로 변환 (예: 1500 -> "00:25", 5400 -> "01:30") */
fun formatHoursMinutes(totalSeconds: Long): String {
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}

/** 요일을 한국어 한 글자로 변환 */
fun DayOfWeek.koreanLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** epoch millis를 "yyyy.MM.dd" 형태로 변환 */
fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)

/** epoch millis를 "HH:mm" 형태로 변환 */
fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

/** 시작~종료를 "18:20 ~ 18:52" 형태로 변환 */
fun formatTimeRange(startMillis: Long, endMillis: Long): String =
    "${formatTime(startMillis)} ~ ${formatTime(endMillis)}"
