package com.catat.app.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.catat.app.data.db.CatatDatabase
import com.catat.app.data.db.entity.BugReportEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BugReportDaoTest {

    private lateinit var database: CatatDatabase
    private lateinit var dao: BugReportDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CatatDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.bugReportDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and fetch by id`() = runBlocking {
        val id = dao.insert(report(title = "Login crash"))

        val saved = dao.getById(id)

        assertNotNull(saved)
        assertEquals("Login crash", saved?.title)
    }

    @Test
    fun `filter by status`() = runBlocking {
        dao.insert(report(title = "Draft", status = "DRAFT"))
        dao.insert(report(title = "Exported", status = "EXPORTED"))

        val exported = dao.getByStatus("EXPORTED").first()

        assertEquals(1, exported.size)
        assertEquals("Exported", exported.first().title)
    }

    @Test
    fun `search via fts`() = runBlocking {
        dao.insert(report(title = "Login fails", steps = "Tap login"))
        dao.insert(report(title = "Payment fails", steps = "Tap pay"))

        val query = SimpleSQLiteQuery(
            """
            SELECT * FROM bug_reports
            WHERE id IN (
                SELECT rowid FROM bug_reports_fts
                WHERE bug_reports_fts MATCH ?
            )
            ORDER BY created_at DESC
            """.trimIndent(),
            arrayOf("login*")
        )
        val results = dao.search(query).first()

        assertEquals(1, results.size)
        assertEquals("Login fails", results.first().title)
    }

    @Test
    fun `delete by id removes report`() = runBlocking {
        val id = dao.insert(report(title = "Delete me"))

        dao.deleteById(id)

        assertEquals(null, dao.getById(id))
    }

    private fun report(
        title: String,
        status: String = "DRAFT",
        steps: String = "Step"
    ) = BugReportEntity(
        title = title,
        stepsToReproduce = steps,
        actualResult = "Actual",
        expectedResult = "Expected",
        status = status,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
