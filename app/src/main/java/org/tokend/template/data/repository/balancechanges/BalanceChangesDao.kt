package org.tokend.template.data.repository.balancechanges

import android.arch.persistence.room.*
import org.tokend.template.data.model.history.BalanceChangeDbEntity

@Dao
interface BalanceChangesDao {
    @Query("SELECT * FROM balance_change")
    fun selectAll(): List<BalanceChangeDbEntity>

    @Query("SELECT * FROM balance_change WHERE id<:cursor ORDER BY id DESC LIMIT :limit ")
    fun selectPageDesc(limit: Int,
                       cursor: Long): List<BalanceChangeDbEntity>

    @Query("SELECT * FROM balance_change WHERE balance_id=:balanceId AND id<:cursor ORDER BY id DESC LIMIT :limit ")
    fun selectPageByBalanceIdDesc(balanceId: String,
                                  limit: Int,
                                  cursor: Long): List<BalanceChangeDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: BalanceChangeDbEntity)

    @Update
    fun update(vararg items: BalanceChangeDbEntity)

    @Delete
    fun delete(vararg items: BalanceChangeDbEntity)

    @Query("DELETE FROM balance_change")
    fun deleteAll()
}