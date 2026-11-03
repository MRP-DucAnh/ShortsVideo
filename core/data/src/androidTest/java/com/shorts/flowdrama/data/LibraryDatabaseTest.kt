package com.shorts.flowdrama.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDatabaseTest {
    private lateinit var database: LibraryDatabase
    private lateinit var dao: LibraryDao

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        dao = database.libraryDao()
    }

    @After fun tearDown() = database.close()

    @Test fun keepsProgressPerEpisodeAndRetainsWatchedOnReplay() = runBlocking {
        dao.saveProgress(EpisodeProgressEntity("show", 1, 100, 100, 1, false, true))
        dao.saveProgress(EpisodeProgressEntity("show", 2, 30, 100, 2, false, false))
        dao.saveProgress(EpisodeProgressEntity("show", 1, 5, 100, 3, false, false))
        val entries = dao.observeProgress().first().associateBy { it.episodeNumber }
        assertEquals(2, entries.size)
        assertEquals(5L, entries.getValue(1).positionMs)
        assertTrue(entries.getValue(1).watched)
        assertFalse(entries.getValue(2).watched)
        assertEquals(30L, entries.getValue(2).positionMs)
    }

    @Test fun staleWriteCannotReplaceNewerPositionButCanMarkWatched() = runBlocking {
        dao.saveProgress(EpisodeProgressEntity("show", 1, 12, 100, 20, false, false))
        dao.saveProgress(EpisodeProgressEntity("show", 1, 100, 100, 10, false, true))
        val entry = dao.observeProgress().first().single()
        assertEquals(12L, entry.positionMs)
        assertTrue(entry.watched)
    }

    @Test fun removingHistoryDeletesEveryEpisodeOnlyForThatShow() = runBlocking {
        dao.saveProgress(EpisodeProgressEntity("a", 1, 1, 10, 1, false, false))
        dao.saveProgress(EpisodeProgressEntity("a", 2, 1, 10, 2, false, false))
        dao.saveProgress(EpisodeProgressEntity("b", 1, 1, 10, 3, false, false))
        dao.toggleSaved("a")
        dao.removeHistory("a")
        assertEquals(listOf("b"), dao.observeProgress().first().map { it.showId })
        assertTrue(dao.isSaved("a"))
        dao.clearHistory()
        assertTrue(dao.observeProgress().first().isEmpty())
    }
    @Test fun removingOneEpisodeKeepsOtherEpisodesAndLikes() = runBlocking {
        dao.saveProgress(EpisodeProgressEntity("a", 1, 100, 100, 1, false, true))
        dao.saveProgress(EpisodeProgressEntity("a", 2, 50, 100, 2, false, false))
        dao.saveProgress(EpisodeProgressEntity("b", 1, 10, 100, 3, false, false))
        dao.toggleEpisodeLike("a", 1)
        dao.removeEpisodeHistory("a", 1)
        val remaining = dao.observeProgress().first().map { it.showId to it.episodeNumber }.toSet()
        assertEquals(setOf("a" to 2, "b" to 1), remaining)
        assertEquals(listOf(LikedEpisode("a", 1)), dao.observeLikedEpisodes().first())
    }

    @Test fun likesAreIndependentAndSurviveHistoryDeletion() = runBlocking {
        dao.toggleShowLike("show")
        dao.toggleEpisodeLike("show", 1)
        dao.toggleEpisodeLike("show", 2)
        dao.toggleEpisodeLike("show", 1)
        dao.clearHistory()
        assertEquals(listOf("show"), dao.observeLikedShows().first())
        assertEquals(listOf(LikedEpisode("show", 2)), dao.observeLikedEpisodes().first())
        dao.toggleShowLike("show")
        assertTrue(dao.observeLikedShows().first().isEmpty())
        assertEquals(listOf(LikedEpisode("show", 2)), dao.observeLikedEpisodes().first())
    }

    @Test fun migrationPreservesExistingSavedAndProgress() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "like_migration_test.db"
        context.deleteDatabase(name)
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                old.execSQL("CREATE TABLE saved (showId TEXT NOT NULL, PRIMARY KEY(showId))")
                old.execSQL("CREATE TABLE episode_progress (showId TEXT NOT NULL, episodeNumber INTEGER NOT NULL, positionMs INTEGER NOT NULL, durationMs INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isLastEpisode INTEGER NOT NULL, watched INTEGER NOT NULL, PRIMARY KEY(showId, episodeNumber))")
                old.execSQL("INSERT INTO saved VALUES ('show')")
                old.execSQL("INSERT INTO episode_progress VALUES ('show', 3, 50, 100, 1, 0, 0)")
                old.version = 1
            }
            val migrated = Room.databaseBuilder(context, LibraryDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2).build()
            try {
                val migratedDao = migrated.libraryDao()
                assertTrue(migratedDao.isSaved("show"))
                assertEquals(50L, migratedDao.observeProgress().first().single().positionMs)
                migratedDao.toggleShowLike("show")
                migratedDao.toggleEpisodeLike("show", 3)
                assertEquals(listOf("show"), migratedDao.observeLikedShows().first())
                assertEquals(listOf(LikedEpisode("show", 3)), migratedDao.observeLikedEpisodes().first())
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

}
