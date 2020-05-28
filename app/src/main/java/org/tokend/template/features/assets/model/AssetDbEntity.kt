package org.tokend.template.features.assets.model

import androidx.room.*
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.v3.assets.model.AssetState
import org.tokend.sdk.factory.GsonFactory
import java.math.BigDecimal

@Entity(tableName = "asset")
@TypeConverters(AssetDbEntity.Converters::class)
data class AssetDbEntity(
        @PrimaryKey
        @ColumnInfo(name = "code")
        val code: String,
        @ColumnInfo(name = "policy")
        val policy: Int,
        @ColumnInfo(name = "name")
        val name: String?,
        @ColumnInfo(name = "logo_url")
        val logoUrl: String?,
        @ColumnInfo(name = "description")
        val description: String?,
        @ColumnInfo(name = "terms")
        val terms: RemoteFile?,
        @ColumnInfo(name = "external_system_type")
        val externalSystemType: Int?,
        @ColumnInfo(name = "issued")
        val issued: BigDecimal,
        @ColumnInfo(name = "available")
        val available: BigDecimal,
        @ColumnInfo(name = "maximum")
        val maximum: BigDecimal,
        @ColumnInfo(name = "owner_account_id")
        val ownerAccountId: String,
        @ColumnInfo(name = "trailing_digits")
        val trailingDigitsCount: Int,
        @ColumnInfo(name = "state")
        val state: String,
        @ColumnInfo(name = "is_coinpayments")
        val isConnectedToCoinpayments: Boolean
) {
    class Converters {
        private val gson = GsonFactory().getBaseGson()

        @TypeConverter
        fun remoteFileFromJson(value: String?): RemoteFile? {
            return value?.let { gson.fromJson(value, RemoteFile::class.java) }
        }

        @TypeConverter
        fun remoteFileToJson(value: RemoteFile?): String? {
            return value?.let { gson.toJson(it) }
        }
    }

    fun toRecord() = AssetRecord(
            code = code,
            policy = policy,
            name = name,
            trailingDigits = trailingDigitsCount,
            logoUrl = logoUrl,
            state = AssetState.valueOf(state),
            ownerAccountId = ownerAccountId,
            available = available,
            description = description,
            externalSystemType = externalSystemType,
            issued = issued,
            maximum = maximum,
            terms = terms,
            isConnectedToCoinpayments = isConnectedToCoinpayments
    )

    companion object {
        fun fromRecord(record: AssetRecord) = record.run {
            AssetDbEntity(
                    code = code,
                    terms = terms,
                    maximum = maximum,
                    issued = issued,
                    externalSystemType = externalSystemType,
                    description = description,
                    available = available,
                    ownerAccountId = ownerAccountId,
                    state = state.toString(),
                    logoUrl = logoUrl,
                    name = name,
                    policy = policy,
                    trailingDigitsCount = trailingDigits,
                    isConnectedToCoinpayments = isConnectedToCoinpayments
            )
        }
    }
}