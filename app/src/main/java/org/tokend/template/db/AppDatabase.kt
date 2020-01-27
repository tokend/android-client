package org.tokend.template.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverter
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.SimpleAsset
import org.tokend.template.data.model.history.BalanceChangeDbEntity
import org.tokend.template.data.repository.balancechanges.BalanceChangesDao
import org.tokend.template.features.assets.model.AssetDbEntity
import org.tokend.template.features.assets.storage.AssetsDao
import org.tokend.template.features.balances.model.BalanceDbEntity
import org.tokend.template.features.balances.storage.BalancesDao
import java.math.BigDecimal
import java.util.*

@Database(
        entities = [
            AssetDbEntity::class,
            BalanceDbEntity::class,
            BalanceChangeDbEntity::class
        ],
        version = 2,
        exportSchema = false
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    class Converters {
        private val gson = GsonFactory().getBaseGson()

        @TypeConverter
        fun dateToUnix(value: Date?): Long? {
            return value?.let { it.time / 1000 }
        }

        @TypeConverter
        fun dateFromUnix(value: Long?): Date? {
            return value?.let { Date(it * 1000) }
        }

        @TypeConverter
        fun bigDecimalToString(value: BigDecimal?): String? {
            return value?.let { BigDecimalUtil.toPlainString(it) }
        }

        @TypeConverter
        fun stringToBigDecimal(value: String?): BigDecimal? {
            return value?.let { BigDecimalUtil.valueOf(it) }
        }

        @TypeConverter
        fun assetFromJson(value: String?): Asset? {
            return value?.let { gson.fromJson(value, SimpleAsset::class.java) }
        }

        @TypeConverter
        fun assetToJson(value: Asset?): String? {
            return value?.let { gson.toJson(SimpleAsset(it)) }
        }
    }

    abstract val balances: BalancesDao
    abstract val assets: AssetsDao
    abstract val balanceChanges: BalanceChangesDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) = database.run {
                beginTransaction()
                try {
                    execSQL("""
                                    CREATE TABLE `balance_change` (`id` INTEGER NOT NULL, 
                                    `action` TEXT NOT NULL, 
                                    `amount` TEXT NOT NULL, `asset` TEXT NOT NULL,
                                     `balance_id` TEXT NOT NULL, `fee` TEXT NOT NULL,
                                      `date` INTEGER NOT NULL, `cause` TEXT NOT NULL,
                                       PRIMARY KEY(`id`))
                                """.trimIndent())
                    execSQL("CREATE INDEX `index_balance_change_balance_id` ON `balance_change` (`balance_id`)")
                    setTransactionSuccessful()
                } finally {
                    endTransaction()
                }
            }
        }
    }
}