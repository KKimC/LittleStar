package com.example.littlestar.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 악보 이미지 파일을 앱 내부 저장소(filesDir/sheets)에 관리한다.
 *
 * 저장 구조:
 * ```
 * filesDir/sheets/sheet-{id}/page-001.jpg
 * ```
 * Room에는 절대 경로가 아니라 "sheets/sheet-{id}/page-001.jpg" 형태의
 * 상대 경로만 저장한다.
 */
class SheetFileStorage(private val context: Context) {

    private val filesDir: File get() = context.filesDir

    /** 악보 id에 해당하는 상대 디렉터리 경로 */
    fun relativeDir(sheetId: Long): String = "sheets/sheet-$sheetId"

    /** 페이지 상대 경로 (pageOrder는 1부터) */
    fun pageRelativePath(sheetId: Long, pageOrder: Int): String =
        "sheets/sheet-$sheetId/page-%03d.jpg".format(pageOrder)

    /** 상대 경로를 절대 File로 변환 */
    fun toFile(relativePath: String): File = File(filesDir, relativePath)

    /**
     * 페이지 이미지 바이트들을 순서대로 저장하고, 저장된 상대 경로 목록을 반환한다.
     * 페이지 순서는 images 리스트 순서를 따른다.
     */
    suspend fun savePages(sheetId: Long, images: List<ByteArray>): List<String> =
        withContext(Dispatchers.IO) {
            val dir = File(filesDir, relativeDir(sheetId)).apply { mkdirs() }
            images.mapIndexed { index, bytes ->
                val relative = pageRelativePath(sheetId, index + 1)
                File(filesDir, relative).writeBytes(bytes)
                relative
            }
        }

    /** 악보 폴더와 모든 이미지 파일을 삭제한다. */
    suspend fun deleteSheet(sheetId: Long) = withContext(Dispatchers.IO) {
        File(filesDir, relativeDir(sheetId)).deleteRecursively()
    }

    /**
     * 악보의 페이지 이미지를 새 목록으로 교체한다.
     * 기존 page-*.jpg 파일을 모두 삭제한 뒤 새 이미지를 저장하고 상대 경로 목록을 반환한다.
     */
    suspend fun replacePages(sheetId: Long, images: List<ByteArray>): List<String> =
        withContext(Dispatchers.IO) {
            val dir = File(filesDir, relativeDir(sheetId)).apply { mkdirs() }
            dir.listFiles()
                ?.filter { it.name.matches(Regex("page-\\d+\\.jpg")) }
                ?.forEach { it.delete() }
            images.mapIndexed { index, bytes ->
                val relative = pageRelativePath(sheetId, index + 1)
                File(filesDir, relative).writeBytes(bytes)
                relative
            }
        }
}
