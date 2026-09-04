package com.haitranduc.fittrack.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseContractTest {

    private lateinit var context: Context
    private val testDbName = "contract_test_isolated.db"
    private var database: FitTrackDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(testDbName)
    }

    data class ForeignKeyInfo(
        val table: String,
        val from: String,
        val to: String,
        val onDelete: String
    )

    data class IndexDetail(
        val name: String,
        val isUnique: Boolean,
        val columns: List<String>
    )

    private fun getForeignKeys(db: androidx.sqlite.db.SupportSQLiteDatabase, tableName: String): List<ForeignKeyInfo> {
        val list = mutableListOf<ForeignKeyInfo>()
        val cursor = db.query("PRAGMA foreign_key_list('$tableName')")
        while (cursor.moveToNext()) {
            list.add(
                ForeignKeyInfo(
                    table = cursor.getString(2),
                    from = cursor.getString(3),
                    to = cursor.getString(4),
                    onDelete = cursor.getString(6).uppercase()
                )
            )
        }
        cursor.close()
        return list
    }

    private fun getIndexes(db: androidx.sqlite.db.SupportSQLiteDatabase, tableName: String): List<IndexDetail> {
        val indexes = mutableListOf<IndexDetail>()
        val listCursor = db.query("PRAGMA index_list('$tableName')")
        val indexNames = mutableListOf<Pair<String, Boolean>>()
        while (listCursor.moveToNext()) {
            val name = listCursor.getString(1)
            val isUnique = listCursor.getInt(2) == 1
            indexNames.add(name to isUnique)
        }
        listCursor.close()

        for ((name, isUnique) in indexNames) {
            val cols = mutableListOf<String>()
            val infoCursor = db.query("PRAGMA index_info('$name')")
            while (infoCursor.moveToNext()) {
                cols.add(infoCursor.getString(2))
            }
            infoCursor.close()
            indexes.add(IndexDetail(name = name, isUnique = isUnique, columns = cols))
        }
        return indexes
    }

    private fun assertIndexMatches(
        indexes: List<IndexDetail>,
        tableName: String,
        expectedColumns: List<String>,
        expectedUnique: Boolean
    ) {
        val found = indexes.any { it.columns == expectedColumns && it.isUnique == expectedUnique }
        assertTrue(
            "Expected index on $tableName with columns $expectedColumns and unique=$expectedUnique was not found in: $indexes",
            found
        )
    }

    @Test
    fun databaseContract_verifiesSchemaVersionTablesIndexesForeignKeysAndDaos() {
        val db = Room.databaseBuilder(context, FitTrackDatabase::class.java, testDbName)
            .allowMainThreadQueries()
            .build()
        database = db

        val sqliteDb = db.openHelper.readableDatabase

        // 1. Assert Room Version
        assertEquals("Database version must be 1", 1, sqliteDb.version)

        // 2. Assert Tables exist
        val tables = mutableListOf<String>()
        val tableCursor = sqliteDb.query("SELECT name FROM sqlite_master WHERE type='table'")
        while (tableCursor.moveToNext()) {
            tables.add(tableCursor.getString(0))
        }
        tableCursor.close()

        assertTrue("Missing exercises table", tables.contains("exercises"))
        assertTrue("Missing workouts table", tables.contains("workouts"))
        assertTrue("Missing workout_exercises table", tables.contains("workout_exercises"))
        assertTrue("Missing workout_sessions table", tables.contains("workout_sessions"))
        assertTrue("Missing set_logs table", tables.contains("set_logs"))

        // 3. Verify Foreign Keys mapping (table, from, to, on_delete)
        val weFks = getForeignKeys(sqliteDb, "workout_exercises")
        assertTrue("workout_exercises must reference workouts(id) with CASCADE",
            weFks.any { it.table == "workouts" && it.from == "workoutId" && it.to == "id" && it.onDelete == "CASCADE" })
        assertTrue("workout_exercises must reference exercises(id)",
            weFks.any { it.table == "exercises" && it.from == "exerciseId" && it.to == "id" && (it.onDelete == "NO ACTION" || it.onDelete == "RESTRICT") })

        val wsFks = getForeignKeys(sqliteDb, "workout_sessions")
        assertTrue("workout_sessions must reference workouts(id) with SET NULL",
            wsFks.any { it.table == "workouts" && it.from == "workoutId" && it.to == "id" && it.onDelete == "SET NULL" })

        val slFks = getForeignKeys(sqliteDb, "set_logs")
        assertTrue("set_logs must reference workout_sessions(id) with CASCADE",
            slFks.any { it.table == "workout_sessions" && it.from == "sessionId" && it.to == "id" && it.onDelete == "CASCADE" })

        // 4. Verify Exact Ordered Columns and Unique Properties for all required indexes:
        val exIndexes = getIndexes(sqliteDb, "exercises")
        assertIndexMatches(exIndexes, "exercises", listOf("name"), false)
        assertIndexMatches(exIndexes, "exercises", listOf("bodyPart"), false)
        assertIndexMatches(exIndexes, "exercises", listOf("equipment"), false)

        val weIndexes = getIndexes(sqliteDb, "workout_exercises")
        assertIndexMatches(weIndexes, "workout_exercises", listOf("workoutId"), false)
        assertIndexMatches(weIndexes, "workout_exercises", listOf("exerciseId"), false)
        assertIndexMatches(weIndexes, "workout_exercises", listOf("workoutId", "orderIndex"), true)

        val wsIndexes = getIndexes(sqliteDb, "workout_sessions")
        assertIndexMatches(wsIndexes, "workout_sessions", listOf("workoutId"), false)
        assertIndexMatches(wsIndexes, "workout_sessions", listOf("finishedAt"), false)

        val slIndexes = getIndexes(sqliteDb, "set_logs")
        assertIndexMatches(slIndexes, "set_logs", listOf("sessionId"), false)
        assertIndexMatches(slIndexes, "set_logs", listOf("sessionId", "exerciseId", "setNumber"), true)

        // 5. Verify DAOs resolution from database instance
        assertNotNull(db.exerciseDao())
        assertNotNull(db.workoutDao())
        assertNotNull(db.workoutSessionDao())
        assertNotNull(db.setLogDao())

        // 6. Verify DatabaseModule providers with isolated database instance
        assertNotNull(DatabaseModule.provideExerciseDao(db))
        assertNotNull(DatabaseModule.provideWorkoutDao(db))
        assertNotNull(DatabaseModule.provideWorkoutSessionDao(db))
        assertNotNull(DatabaseModule.provideSetLogDao(db))
        val seedReader = DatabaseModule.provideAssetSeedReader(context)
        assertNotNull(seedReader)
        val seedImporter = DatabaseModule.provideExerciseSeedImporter(db, db.exerciseDao(), seedReader)
        assertNotNull(seedImporter)
    }
}
