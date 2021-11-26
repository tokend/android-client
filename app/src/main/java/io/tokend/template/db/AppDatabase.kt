package io.tokend.template.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.assets.model.AssetDbEntity
import io.tokend.template.features.assets.model.SimpleAsset
import io.tokend.template.features.assets.storage.AssetsDao
import io.tokend.template.features.balances.model.BalanceDbEntity
import io.tokend.template.features.balances.storage.BalancesDao
import io.tokend.template.features.history.storage.BalanceChangeDbEntity
import io.tokend.template.features.history.storage.BalanceChangesDao
import org.tokend.sdk.factory.JsonApiTools
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.util.*

@Database(
    entities = [
        AssetDbEntity::class,
        BalanceDbEntity::class,
        BalanceChangeDbEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    class Converters {
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
            return value?.let {
                JsonApiTools.objectMapper.readValue(value, SimpleAsset::class.java)
            }
        }

        @TypeConverter
        fun assetToJson(value: Asset?): String? {
            return value?.let {
                JsonApiTools.objectMapper.writeValueAsString(SimpleAsset(it))
            }
        }
    }

    abstract val balances: BalancesDao
    abstract val assets: AssetsDao
    abstract val balanceChanges: BalanceChangesDao

    companion object {
        val MIGRATIONS = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) = database.run {
                    execSQL(
                        """
                            CREATE TABLE `balance_change` (`id` INTEGER NOT NULL, 
                            `action` TEXT NOT NULL, 
                            `amount` TEXT NOT NULL, `asset` TEXT NOT NULL,
                             `balance_id` TEXT NOT NULL, `fee` TEXT NOT NULL,
                              `date` INTEGER NOT NULL, `cause` TEXT NOT NULL,
                               PRIMARY KEY(`id`))
                        """.trimIndent()
                    )
                    execSQL("CREATE INDEX `index_balance_change_balance_id` ON `balance_change` (`balance_id`)")
                }
            },
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) = database.run {
                    execSQL(
                        """
                            ALTER TABLE `asset` 
                            ADD COLUMN `is_coinpayments` INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )
                }
            },
            object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) = database.run {
                    execSQL("DELETE FROM balance_change")
                }
            }
        )
    }
}