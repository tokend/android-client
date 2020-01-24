package org.tokend.template.data.model.history

import android.arch.persistence.room.*
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.tokend.sdk.factory.GsonFactory
import org.tokend.template.data.model.Asset
import org.tokend.template.data.model.history.details.BalanceChangeCause
import java.math.BigDecimal
import java.util.*

@Entity(
        tableName = "balance_change"
)
@TypeConverters(BalanceChangeDbEntity.Converters::class)
data class BalanceChangeDbEntity(
        @PrimaryKey
        @ColumnInfo(name = "id")
        val id: Long,
        @ColumnInfo(name = "action")
        val action: String,
        @ColumnInfo(name = "amount")
        val amount: BigDecimal,
        @ColumnInfo(name = "asset")
        val asset: Asset,
        @ColumnInfo(name = "balance_id")
        val balanceId: String,
        @ColumnInfo(name = "fee")
        val fee: SimpleFeeRecord,
        @ColumnInfo(name = "date")
        val date: Date,
        @ColumnInfo(name = "cause")
        val cause: BalanceChangeCause
) {
    class Converters {
        private class BalanceChangeCauseContainer(
                @SerializedName("class_name")
                val className: String,
                @SerializedName("data")
                val data: JsonElement
        )

        private val gson = GsonFactory().getBaseGson()

        @TypeConverter
        fun causeFromJson(value: String?): BalanceChangeCause? {
            val container = value?.let { gson.fromJson(it, BalanceChangeCauseContainer::class.java) }
                    ?: return null
            return gson.fromJson<BalanceChangeCause>(
                    container.data,
                    Class.forName(container.className)
            )
        }

        @TypeConverter
        fun causeToJson(value: BalanceChangeCause?): String? {
            val container = value?.let { BalanceChangeCauseContainer(
                    className = it::class.java.name,
                    data = gson.toJsonTree(it)
            ) }
                    ?: return null
            return gson.toJson(container)
        }

        @TypeConverter
        fun feeFromJson(value: String?): SimpleFeeRecord? {
            return value?.let { gson.fromJson(value, SimpleFeeRecord::class.java) }
        }

        @TypeConverter
        fun feeToJson(value: SimpleFeeRecord?): String? {
            return value?.let { gson.toJson(it) }
        }
    }

    fun toRecord() = BalanceChange(
            id = id,
            date = date,
            balanceId = balanceId,
            asset = asset,
            amount = amount,
            fee = fee,
            action = BalanceChangeAction.valueOf(action),
            cause = cause
    )

    companion object {
        fun fromRecord(record: BalanceChange) = record.run { BalanceChangeDbEntity(
                id = id,
                cause = cause,
                action = action.toString(),
                fee = fee,
                amount = amount,
                asset = asset,
                balanceId = balanceId,
                date = date
        ) }
    }
}