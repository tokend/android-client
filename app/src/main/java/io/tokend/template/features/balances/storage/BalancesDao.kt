package io.tokend.template.features.balances.storage

import androidx.room.*
import io.tokend.template.features.balances.model.BalanceDbEntity

@Dao
interface BalancesDao {
    @Query("SELECT * FROM balance")
    fun selectAll(): List<BalanceDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: BalanceDbEntity)

    @Update
    fun update(vararg items: BalanceDbEntity)

    @Delete
    fun delete(vararg items: BalanceDbEntity)

    @Query("DELETE FROM balance")
    fun deleteAll()
}