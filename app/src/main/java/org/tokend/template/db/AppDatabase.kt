package org.tokend.template.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverter
import android.arch.persistence.room.TypeConverters
import org.tokend.sdk.utils.BigDecimalUtil
import org.tokend.template.features.assets.model.AssetDbEntity
import org.tokend.template.features.assets.storage.AssetsDao
import org.tokend.template.features.balances.model.BalanceDbEntity
import org.tokend.template.features.balances.storage.BalancesDao
import java.math.BigDecimal
import java.util.*

@Database(
        entities = [
            AssetDbEntity::class,
            BalanceDbEntity::class
        ],
        version = 1,
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
    }

    abstract val balances: BalancesDao
    abstract val assets: AssetsDao
}