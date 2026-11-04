package com.shorts.flowdrama.data

import android.content.Context
import androidx.room.*
import com.shorts.flowdrama.domain.LibraryRepository
import com.shorts.flowdrama.domain.EpisodeKey
import com.shorts.flowdrama.domain.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Entity(tableName = "saved")
internal data class SavedShow(@PrimaryKey val showId: String)

@Entity(tableName = "liked_shows")
internal data class LikedShow(@PrimaryKey val showId: String)

@Entity(tableName = "liked_episodes", primaryKeys = ["showId", "episodeNumber"])
internal data class LikedEpisode(val showId: String, val episodeNumber: Int)

@Entity(tableName = "episode_progress", primaryKeys = ["showId", "episodeNumber"])
internal data class EpisodeProgressEntity(
    val showId: String,
    val episodeNumber: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val isLastEpisode: Boolean,
    val watched: Boolean,
) {
    fun toDomain() = Progress(showId, episodeNumber, positionMs, durationMs, updatedAt, isLastEpisode, watched)
}

@Dao
internal interface LibraryDao {
    @Query("SELECT showId FROM saved")
    fun observeSaved(): Flow<List<String>>

    @Query("SELECT showId FROM liked_shows")
    fun observeLikedShows(): Flow<List<String>>

    @Query("SELECT * FROM liked_episodes")
    fun observeLikedEpisodes(): Flow<List<LikedEpisode>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLikedShow(show: LikedShow): Long

    @Query("DELETE FROM liked_shows WHERE showId = :showId")
    suspend fun deleteLikedShow(showId: String)

    @Transaction
    suspend fun toggleShowLike(showId: String) {
        if (insertLikedShow(LikedShow(showId)) == -1L) deleteLikedShow(showId)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLikedEpisode(episode: LikedEpisode): Long

    @Query("DELETE FROM liked_episodes WHERE showId = :showId AND episodeNumber = :number")
    suspend fun deleteLikedEpisode(showId: String, number: Int)

    @Transaction
    suspend fun toggleEpisodeLike(showId: String, number: Int) {
        if (insertLikedEpisode(LikedEpisode(showId, number)) == -1L) deleteLikedEpisode(showId, number)
    }

    @Query("SELECT * FROM episode_progress ORDER BY updatedAt DESC, episodeNumber DESC")
    fun observeProgress(): Flow<List<EpisodeProgressEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved WHERE showId = :showId)")
    suspend fun isSaved(showId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSaved(show: SavedShow)

    @Query("DELETE FROM saved WHERE showId = :showId")
    suspend fun deleteSaved(showId: String)

    @Transaction
    suspend fun toggleSaved(showId: String) {
        if (isSaved(showId)) deleteSaved(showId) else insertSaved(SavedShow(showId))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgress(progress: EpisodeProgressEntity): Long

    @Query("""
        UPDATE episode_progress SET
            positionMs = CASE WHEN :updated >= updatedAt THEN :position ELSE positionMs END,
            durationMs = CASE WHEN :updated >= updatedAt THEN :duration ELSE durationMs END,
            isLastEpisode = CASE WHEN :updated >= updatedAt THEN :last ELSE isLastEpisode END,
            watched = MAX(watched, :watched),
            updatedAt = MAX(updatedAt, :updated)
        WHERE showId = :showId AND episodeNumber = :episode
    """)
    suspend fun updateProgress(showId: String, episode: Int, position: Long, duration: Long,
        updated: Long, last: Boolean, watched: Boolean)

    @Transaction
    suspend fun saveProgress(progress: EpisodeProgressEntity) {
        if (insertProgress(progress) == -1L) {
            updateProgress(progress.showId, progress.episodeNumber, progress.positionMs,
                progress.durationMs, progress.updatedAt, progress.isLastEpisode, progress.watched)
        }
    }

    @Query("DELETE FROM episode_progress")
    suspend fun clearHistory()

    @Query("DELETE FROM episode_progress WHERE showId = :showId")
    suspend fun removeHistory(showId: String)

    @Query("DELETE FROM episode_progress WHERE showId = :showId AND episodeNumber = :episodeNumber")
    suspend fun removeEpisodeHistory(showId: String, episodeNumber: Int)
}

@Database(entities = [SavedShow::class, EpisodeProgressEntity::class, LikedShow::class, LikedEpisode::class], version = 1, exportSchema = false)
internal abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}

/** Stores watch progress per episode; history exposes only the latest episode per show. */
class LocalLibraryRepository(context: Context, scope: CoroutineScope) : LibraryRepository {
    // All current entities form the initial schema for fresh installations.
    private val database = Room.databaseBuilder(context.applicationContext, LibraryDatabase::class.java, "library_room.db").build()
    private val dao = database.libraryDao()
    private val toggleMutex = Mutex()
    override val savedIds: StateFlow<Set<String>> = dao.observeSaved().map { it.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
    override val likedShowIds: StateFlow<Set<String>> = dao.observeLikedShows().map { it.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
    override val likedEpisodes: StateFlow<Set<EpisodeKey>> = dao.observeLikedEpisodes()
        .map { items -> items.map { EpisodeKey(it.showId, it.episodeNumber) }.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
    override val episodeProgress: StateFlow<List<Progress>?> = dao.observeProgress().map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, null)
    override val history: StateFlow<List<Progress>> = episodeProgress.filterNotNull().map { entries -> entries.distinctBy { it.showId } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun toggleSaved(showId: String) = toggleMutex.withLock { dao.toggleSaved(showId) }
    override suspend fun toggleShowLike(showId: String) = dao.toggleShowLike(showId)
    override suspend fun toggleEpisodeLike(showId: String, episodeNumber: Int) = dao.toggleEpisodeLike(showId, episodeNumber)
    override suspend fun saveProgress(progress: Progress) = dao.saveProgress(EpisodeProgressEntity(
        progress.showId, progress.episodeNumber, progress.positionMs, progress.durationMs,
        progress.updatedAt, progress.isLastEpisode,
        progress.watched,
    ))
    override suspend fun clearHistory() = dao.clearHistory()
    override suspend fun removeHistory(showId: String) = dao.removeHistory(showId)
    override suspend fun removeEpisodeHistory(showId: String, episodeNumber: Int) =
        dao.removeEpisodeHistory(showId, episodeNumber)
}
