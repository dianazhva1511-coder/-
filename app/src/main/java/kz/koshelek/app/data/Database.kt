package kz.koshelek.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/** Цель накоплений. */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Double,
    val saved: Double,
)

/** Лимит расходов на категорию в месяц. */
@Entity(tableName = "limits")
data class CategoryLimit(
    @PrimaryKey val category: String,
    val amount: Double,
)

/** Последний известный остаток на карте банка (берётся из «Доступно …» в уведомлении). */
@Entity(tableName = "balances")
data class Balance(
    @PrimaryKey val bank: String,
    val amount: Double,
    val updatedAt: Long,
)

@Dao
interface ExtrasDao {
    @Query("SELECT * FROM goals ORDER BY id")
    fun goals(): Flow<List<Goal>>

    @Insert
    suspend fun insertGoal(g: Goal)

    @Update
    suspend fun updateGoal(g: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM limits")
    fun limits(): Flow<List<CategoryLimit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLimit(l: CategoryLimit)

    @Query("DELETE FROM limits WHERE category = :category")
    suspend fun deleteLimit(category: String)

    @Query("SELECT * FROM balances ORDER BY bank")
    fun balances(): Flow<List<Balance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBalance(b: Balance)
}

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

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `target` REAL NOT NULL, `saved` REAL NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `limits` (`category` TEXT NOT NULL, `amount` REAL NOT NULL, PRIMARY KEY(`category`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `balances` (`bank` TEXT NOT NULL, `amount` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`bank`))")
    }
}

@Database(
    entities = [Transaction::class, RawNotification::class, Goal::class, CategoryLimit::class, Balance::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionDao
    abstract fun rawNotifications(): RawNotificationDao
    abstract fun extras(): ExtrasDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "koshelek.db")
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
