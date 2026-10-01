package com.example.littlestar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.littlestar.data.local.dao.PracticeSessionDao
import com.example.littlestar.data.local.dao.RecordingDao
import com.example.littlestar.data.local.dao.SheetDirectoryDao
import com.example.littlestar.data.local.dao.SheetMusicDao
import com.example.littlestar.data.local.dao.SheetPageDao
import com.example.littlestar.data.local.entity.PracticeSessionEntity
import com.example.littlestar.data.local.entity.RecordingEntity
import com.example.littlestar.data.local.entity.SheetDirectoryEntity
import com.example.littlestar.data.local.entity.SheetMusicEntity
import com.example.littlestar.data.local.entity.SheetPageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        SheetDirectoryEntity::class,
        SheetMusicEntity::class,
        SheetPageEntity::class,
        PracticeSessionEntity::class,
        RecordingEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class PianoFlowDatabase : RoomDatabase() {
    abstract fun sheetDirectoryDao(): SheetDirectoryDao
    abstract fun sheetMusicDao(): SheetMusicDao
    abstract fun sheetPageDao(): SheetPageDao
    abstract fun practiceSessionDao(): PracticeSessionDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        private const val DB_NAME = "pianoflow.db"

        @Volatile private var instance: PianoFlowDatabase? = null

        private val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getInstance(context: Context): PianoFlowDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        /** 복원 전에 현재 DB를 닫고 싱글턴을 초기화한다. */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        private fun build(context: Context): PianoFlowDatabase =
            Room.databaseBuilder(context, PianoFlowDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : RoomDatabase.Callback() {
                    // onOpen은 DB를 열 때마다 호출된다. SeedData.seed가 내부에서
                    // count 검사를 하므로 데이터가 없을 때만 시드된다.
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        seedScope.launch { instance?.let { SeedData.seed(it) } }
                    }
                })
                .build()
    }
}
