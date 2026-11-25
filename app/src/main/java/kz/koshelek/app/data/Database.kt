package kz.koshelek.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Одна операция: поступление (INCOME) или списание (EXPENSE). */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val bank: String,
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val source: String, // AUTO или MANUAL
    val rawText: String = "",
)

/** Журнал всех уведомлений от банков, чтобы видеть, что пришло и что распознано. */
@Entity(tableName = "raw_notifications")
data class RawNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val result: String, // "добавлено", "дубликат", "не распознано"
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun all(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    suspend fun between(from: Long, to: Long): List<Transaction>

    @Insert
    suspend fun insert(t: Transaction): Long

    @Update
    suspend fun update(t: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RawNotificationDao {
    @Query("SELECT * FROM raw_notifications ORDER BY timestamp DESC LIMIT 300")
    fun latest(): Flow<List<RawNotification>>

    @Insert
    suspend fun insert(n: RawNotification)

    @Query("DELETE FROM raw_notifications")
    suspend fun clear()
}

@Database(entities = [Transaction::class, RawNotification::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun rawNotifications(): RawNotificationDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "koshelek.db")
                    .build().also { instance = it }
            }
    }
}
