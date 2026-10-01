package com.example.littlestar.data.storage

import android.content.Context
import com.example.littlestar.data.local.PianoFlowDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 앱 데이터(Room DB + 악보 이미지)를 단일 ZIP 파일로 백업/복원한다.
 *
 * 최신 백업 하나만 유지한다(같은 파일을 덮어쓴다).
 * DB는 WAL 모드이므로 .db와 함께 -wal/-shm 파일까지 함께 저장한다.
 */
class BackupManager(private val context: Context) {

    private val backupFile: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, BACKUP_FILE_NAME)

    /** 현재 데이터를 백업 파일로 덮어쓰고 성공 여부를 반환한다. */
    suspend fun backup(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            backupFile.parentFile?.mkdirs()
            backupFile.outputStream().use { output ->
                ZipOutputStream(output).use { zip ->
                    addDatabaseFiles(zip)
                    addSheetFiles(zip)
                }
            }
        }.isSuccess
    }

    /** 백업 파일에서 복원하고 성공 여부를 반환한다. 복원 후 앱 재시작이 필요하다. */
    suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        if (!backupFile.exists()) return@withContext false
        runCatching {
            val staging = File(context.cacheDir, "restore").apply {
                deleteRecursively()
                mkdirs()
            }

            ZipInputStream(backupFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val target = File(staging, entry.name)
                    if (!target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                        throw SecurityException("잘못된 백업 파일 경로")
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val stagedDb = File(staging, "database/$DB_NAME")
            if (!stagedDb.exists()) {
                staging.deleteRecursively()
                return@withContext false
            }

            PianoFlowDatabase.closeInstance()

            val dbPath = context.getDatabasePath(DB_NAME)
            replaceFile(stagedDb, dbPath)
            replaceFile(File(staging, "database/$DB_NAME-wal"), File(dbPath.path + "-wal"))
            replaceFile(File(staging, "database/$DB_NAME-shm"), File(dbPath.path + "-shm"))

            val targetSheets = File(context.filesDir, "sheets")
            targetSheets.deleteRecursively()
            val stagedSheets = File(staging, "sheets")
            if (stagedSheets.exists()) {
                stagedSheets.copyRecursively(targetSheets, overwrite = true)
            }

            staging.deleteRecursively()
            true
        }.getOrDefault(false)
    }

    /** 마지막 백업 시각(epoch millis). 백업이 없으면 null. */
    fun lastBackupTimeMillis(): Long? =
        if (backupFile.exists()) backupFile.lastModified() else null

    private fun replaceFile(src: File, dest: File) {
        dest.delete()
        if (src.exists()) {
            dest.parentFile?.mkdirs()
            src.copyTo(dest, overwrite = true)
        }
    }

    private fun addDatabaseFiles(zip: ZipOutputStream) {
        val dbFile = context.getDatabasePath(DB_NAME)
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"))
            .filter { it.exists() }
            .forEach { file ->
                zip.putNextEntry(ZipEntry("database/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
    }

    private fun addSheetFiles(zip: ZipOutputStream) {
        val sheetsDir = File(context.filesDir, "sheets")
        if (!sheetsDir.exists()) return
        sheetsDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relative = file.relativeTo(context.filesDir).path.replace(File.separatorChar, '/')
                zip.putNextEntry(ZipEntry(relative))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
    }

    companion object {
        const val DB_NAME = "pianoflow.db"
        const val BACKUP_FILE_NAME = "pianoflow-backup.zip"
    }
}
