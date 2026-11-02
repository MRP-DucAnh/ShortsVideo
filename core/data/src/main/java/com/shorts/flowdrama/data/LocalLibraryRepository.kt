package com.shorts.flowdrama.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.shorts.flowdrama.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** SQLite is isolated here; changing persistence never changes a feature or domain contract. */
class LocalLibraryRepository(context: Context, scope: CoroutineScope) : LibraryRepository {
    private val db =
        object : SQLiteOpenHelper(context.applicationContext, "library.db", null, 2) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("CREATE TABLE saved (show_id TEXT PRIMARY KEY)")
                db.execSQL(
                    "CREATE TABLE progress (show_id TEXT PRIMARY KEY, episode INTEGER NOT NULL, position INTEGER NOT NULL, duration INTEGER NOT NULL, updated INTEGER NOT NULL, is_last_episode INTEGER NOT NULL DEFAULT 0)"
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion < 2) {
                    db.execSQL(
                        "ALTER TABLE progress ADD COLUMN is_last_episode INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }
        }
    private val mutex = Mutex()
    private val saved = MutableStateFlow<Set<String>>(emptySet())
    private val progress = MutableStateFlow<List<Progress>>(emptyList())
    override val savedIds = saved.asStateFlow()
    override val history = progress.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { mutex.withLock { read() } }
    }

    private fun read() {
        saved.value = buildSet {
            db.readableDatabase.rawQuery("SELECT show_id FROM saved", null).use {
                while (it.moveToNext()) add(it.getString(0))
            }
        }
        progress.value = buildList {
            db.readableDatabase
                .rawQuery(
                    "SELECT show_id,episode,position,duration,updated,is_last_episode FROM progress ORDER BY updated DESC",
                    null,
                )
                .use {
                    while (it.moveToNext()) add(
                        Progress(
                            it.getString(0),
                            it.getInt(1),
                            it.getLong(2),
                            it.getLong(3),
                            it.getLong(4),
                            it.getInt(5) == 1,
                        )
                    )
                }
        }
    }

    override suspend fun toggleSaved(showId: String) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (showId in saved.value)
                    db.writableDatabase.delete("saved", "show_id=?", arrayOf(showId))
                else
                    db.writableDatabase.insertOrThrow(
                        "saved",
                        null,
                        ContentValues().apply { put("show_id", showId) },
                    )
                read()
            }
        }

    override suspend fun saveProgress(progress: Progress) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                db.writableDatabase.insertWithOnConflict(
                    "progress",
                    null,
                    ContentValues().apply {
                        put("show_id", progress.showId)
                        put("episode", progress.episodeNumber)
                        put("position", progress.positionMs)
                        put("duration", progress.durationMs)
                        put("updated", progress.updatedAt)
                        put("is_last_episode", if (progress.isLastEpisode) 1 else 0)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
                read()
            }
        }

    override suspend fun clearHistory() =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                db.writableDatabase.delete("progress", null, null)
                read()
            }
        }

    override suspend fun removeHistory(showId: String) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                db.writableDatabase.delete("progress", "show_id=?", arrayOf(showId))
                read()
            }
        }
}
