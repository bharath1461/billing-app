package com.example.srchicken.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomerEntity::class,
        BillEntity::class,
        BillItemEntity::class,
        ProductEntity::class,
        SettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun billDao(): BillDao
    abstract fun productDao(): ProductDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billing_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default products
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.productDao()?.apply {
                                insertProduct(ProductEntity(name = "Product A", unit = "pcs", active = true))
                                insertProduct(ProductEntity(name = "Product B", unit = "pcs", active = true))
                            }
                            INSTANCE?.settingsDao()?.saveSettings(SettingsEntity())
                        }
                    }
                })
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN customerCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN openingBalance REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bills ADD COLUMN openingBalance REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bills ADD COLUMN totalDue REAL NOT NULL DEFAULT 0")
                // Existing invoices had no carried balance, so their amount due equals their total.
                db.execSQL("UPDATE bills SET totalDue = total")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bills ADD COLUMN customerCode TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
