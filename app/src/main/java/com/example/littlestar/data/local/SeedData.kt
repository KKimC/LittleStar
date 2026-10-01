package com.example.littlestar.data.local

import com.example.littlestar.data.local.entity.SheetDirectoryEntity
import com.example.littlestar.data.local.entity.SheetMusicEntity

/**
 * 데이터베이스가 비어 있을 때 기본 디렉토리와 간추린 체르니 100 목록을 채운다.
 * 연습 세션, 악보 페이지, 그 외 악보는 모두 사용자가 직접 생성한다.
 */
object SeedData {

    suspend fun seed(db: PianoFlowDatabase) {
        val directoryDao = db.sheetDirectoryDao()
        if (directoryDao.count() > 0) return

        directoryDao.insert(SheetDirectoryEntity(id = 1, name = "간추린 체르니 100", sortOrder = 1))
        directoryDao.insert(SheetDirectoryEntity(id = 2, name = "프린트 악보", sortOrder = 2))

        val now = System.currentTimeMillis()
        // "간추린 체르니 100"은 40번 곡까지만 있다.
        val sheets = (1..40).map { n ->
            SheetMusicEntity(
                id = n.toLong(),
                title = "간추린 체르니 ${n}번 곡",
                composer = "카를 체르니",
                directoryId = 1,
                czernyNumber = n,
                lessonNote = null,
                createdAt = now,
                updatedAt = now,
            )
        }
        db.sheetMusicDao().insertAll(sheets)
    }
}
