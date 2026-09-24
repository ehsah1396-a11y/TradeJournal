package com.example.tradejournal

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val isBuy: Boolean,
    val entry: Double?,
    val exit: Double?,
    val pl: Double,
    val note: String,
    val mood: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY createdAt DESC")
    fun all(): Flow<List<Trade>>

    @Insert
    suspend fun insert(t: Trade)

    @Delete
    suspend fun delete(t: Trade)
}

@Database(entities = [Trade::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): TradeDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "journal.db")
                .build().also { inst = it }
        }
    }
}
