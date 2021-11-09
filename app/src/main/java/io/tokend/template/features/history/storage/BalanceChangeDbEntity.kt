package io.tokend.template.features.history.storage

import androidx.room.*
import com.google.gson.JsonObject
import io.tokend.template.features.assets.model.Asset
import io.tokend.template.features.history.model.BalanceChange
import io.tokend.template.features.history.model.BalanceChangeAction
import io.tokend.template.features.history.model.SimpleFeeRecord
import io.tokend.template.features.history.model.details.BalanceChangeCause
import org.tokend.sdk.factory.GsonFactory
import java.math.BigDecimal
import java.util.*

@Entity(
    tableName = "balance_change",
    indices = [Index("balance_id")]
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
        private val gson = GsonFactory().getBaseGson()

        @TypeConverter
        fun causeFromJson(value: String?): BalanceChangeCause? {
            val json = value?.let { gson.fromJson(it, JsonObject::class.java) }
                ?: return null
            return gson.fromJson<BalanceChangeCause>(
                json,
                Class.forName(
                    json[CAUSE_CLASS_NAME_PROPERTY].asString
                        .replace(
                            "org.tokend.template.data.model.history",
                            "org.tokend.template.features.history.model"
                        )
                        .replace(
                            "org.tokend.template.features.history.model",
                            "io.tokend.template.features.history.model"
                        )
                )
            )
        }

        @TypeConverter
        fun causeToJson(value: BalanceChangeCause?): String? {
            value ?: return null
            val tree = gson.toJsonTree(value)
            (tree as? JsonObject)?.addProperty(CAUSE_CLASS_NAME_PROPERTY, value::class.java.name)
            return tree.toString()
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
        private const val CAUSE_CLASS_NAME_PROPERTY = "_meta_class_name"

        fun fromRecord(record: BalanceChange) = record.run {
            BalanceChangeDbEntity(
                id = id,
                cause = cause,
                action = action.toString(),
                fee = fee,
                amount = amount,
                asset = asset,
                balanceId = balanceId,
                date = date
            )
        }
    }
}