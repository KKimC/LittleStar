package com.example.littlestar.core.util

/**
 * 문자열 안의 숫자를 숫자 크기로 비교하는 자연 정렬 비교기.
 * 예: "간추린 체르니 2번 곡" < "간추린 체르니 10번 곡"
 */
fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var endA = i
            while (endA < a.length && a[endA].isDigit()) endA++
            var endB = j
            while (endB < b.length && b[endB].isDigit()) endB++

            val numA = a.substring(i, endA).trimStart('0').ifEmpty { "0" }
            val numB = b.substring(j, endB).trimStart('0').ifEmpty { "0" }
            val cmp = when {
                numA.length != numB.length -> numA.length - numB.length
                else -> numA.compareTo(numB)
            }
            if (cmp != 0) return cmp
            i = endA
            j = endB
        } else {
            val cmp = ca.compareTo(cb)
            if (cmp != 0) return cmp
            i++
            j++
        }
    }
    return (a.length - i) - (b.length - j)
}
