package com.example.littlestar.data.storage

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 녹음 오디오 파일을 앱 내부 저장소(filesDir/recordings)에 관리한다.
 *
 * 저장 구조:
 * ```
 * filesDir/recordings/sheet-{id}/rec-{timestamp}.m4a
 * ```
 * Room에는 절대 경로가 아니라 "recordings/sheet-{id}/rec-....m4a" 형태의
 * 상대 경로만 저장한다.
 */
class RecordingFileStorage(private val context: Context) {

    private val filesDir: File get() = context.filesDir

    /** 악보 id에 해당하는 상대 디렉터리 경로 */
    fun relativeDir(sheetId: Long): String = "recordings/sheet-$sheetId"

    /** 새 녹음 파일의 상대 경로를 생성한다(파일은 만들지 않음). */
    fun newRelativePath(sheetId: Long): String =
        "recordings/sheet-$sheetId/rec-${System.currentTimeMillis()}.m4a"

    /** 상대 경로를 절대 File로 변환 */
    fun toFile(relativePath: String): File = File(filesDir, relativePath)

    /** 녹음 파일을 저장할 절대 File을 만들어 반환한다(부모 디렉터리 생성 포함). */
    fun prepareFile(relativePath: String): File {
        val file = File(filesDir, relativePath)
        file.parentFile?.mkdirs()
        return file
    }

    /** 녹음 파일 하나를 삭제한다. */
    suspend fun delete(relativePath: String) = withContext(Dispatchers.IO) {
        File(filesDir, relativePath).delete()
    }

    /** 악보에 속한 모든 녹음 파일을 삭제한다. */
    suspend fun deleteSheet(sheetId: Long) = withContext(Dispatchers.IO) {
        File(filesDir, relativeDir(sheetId)).deleteRecursively()
    }

    /** 내부 저장소의 녹음 파일을 사용자가 선택한 위치(Uri)로 내보낸다. */
    suspend fun exportRecording(relativePath: String, targetUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val source = File(filesDir, relativePath)
                if (!source.exists()) return@withContext false
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    source.inputStream().use { it.copyTo(output) }
                } ?: return@withContext false
                true
            }.getOrDefault(false)
        }

    /** 외부 오디오 파일(Uri)을 내부 저장소로 가져오고 상대 경로와 길이(초)를 반환한다. */
    suspend fun importRecording(sheetId: Long, sourceUri: Uri): ImportedRecording? =
        withContext(Dispatchers.IO) {
            runCatching {
                val relative = newRelativePath(sheetId)
                val target = prepareFile(relative)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    target.outputStream().use { input.copyTo(it) }
                } ?: return@withContext null

                val durationSeconds = runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(target.absolutePath)
                    val ms = retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    retriever.release()
                    (ms / 1000L).toInt()
                }.getOrDefault(0)

                ImportedRecording(relative, durationSeconds)
            }.getOrNull()
        }
}

/** 외부에서 가져온 녹음 파일의 정보 */
data class ImportedRecording(
    val fileName: String,
    val durationSeconds: Int,
)
