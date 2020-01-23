package org.tokend.template.data.repository.balancechanges

import android.arch.persistence.room.*
import org.tokend.template.data.model.history.BalanceChangeDbEntity
import org.tokend.template.features.assets.model.AssetDbEntity

@Dao
interface BalanceChangesDao {
    @Query("SELECT * FROM balance_change")
    fun selectAll(): List<BalanceChangeDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: BalanceChangeDbEntity)

    @Update
    fun update(vararg items: BalanceChangeDbEntity)

    @Delete
    fun delete(vararg items: BalanceChangeDbEntity)

    @Query("DELETE FROM balance_change")
    fun deleteAll()
}