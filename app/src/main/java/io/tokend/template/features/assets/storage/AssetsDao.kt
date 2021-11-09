package io.tokend.template.features.assets.storage

import androidx.room.*
import io.tokend.template.features.assets.model.AssetDbEntity

@Dao
interface AssetsDao {
    @Query("SELECT * FROM asset")
    fun selectAll(): List<AssetDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: AssetDbEntity)

    @Update
    fun update(vararg items: AssetDbEntity)

    @Delete
    fun delete(vararg items: AssetDbEntity)

    @Query("DELETE FROM asset")
    fun deleteAll()
}