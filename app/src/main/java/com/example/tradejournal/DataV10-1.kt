package com.example.tradejournal

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val isBuy: Boolean,
    val entry: Double?,
    val exit: Double?,
    val pl: Double,          // سود (مثبت) یا زیان (منفی) به دلار
    val note: String,
    val mood: Int,           // 0 مطمئن، 1 عادی، 2 مضطرب، 3 عصبی
    val createdAt: Long = System.currentTimeMillis(),
    val strategy: String = ""   // استراتژی معامله (اختیاری)
)

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY createdAt DESC")
    fun all(): Flow<List<Trade>>

    @Insert
    suspend fun insert(t: Trade)

    @Insert
    suspend fun insertAll(list: List<Trade>)

    @Update
    suspend fun update(t: Trade)

    @Delete
    suspend fun delete(t: Trade)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE trades ADD COLUMN strategy TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Trade::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): TradeDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "journal.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { inst = it }
        }
    }
}
