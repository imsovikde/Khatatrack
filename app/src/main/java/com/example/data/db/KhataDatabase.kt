package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.ContactDao
import com.example.data.dao.IncomeExpenseEntryDao
import com.example.data.dao.PaymentModeDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.TraceLogDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryItem
import com.example.data.model.Contact
import com.example.data.model.IncomeExpenseEntry
import com.example.data.model.PaymentModeItem
import com.example.data.model.Reminder
import com.example.data.model.TraceLog
import com.example.data.model.Transaction

@Database(
    entities = [
        Contact::class,
        Transaction::class,
        Reminder::class,
        TraceLog::class,
        CategoryItem::class,
        PaymentModeItem::class,
        IncomeExpenseEntry::class
    ],
    version = 6,
    exportSchema = false
)
abstract class KhataDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun transactionDao(): TransactionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun traceLogDao(): TraceLogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentModeDao(): PaymentModeDao
    abstract fun incomeExpenseEntryDao(): IncomeExpenseEntryDao

    companion object {
        @Volatile
        private var INSTANCE: KhataDatabase? = null

        fun getDatabase(context: Context): KhataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KhataDatabase::class.java,
                    "khatatrack.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
