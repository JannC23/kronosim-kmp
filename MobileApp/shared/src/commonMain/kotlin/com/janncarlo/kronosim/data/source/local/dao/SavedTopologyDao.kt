package com.janncarlo.kronosim.data.source.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.janncarlo.kronosim.data.source.local.entity.SavedTopologyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTopologyDao {
    @Query("SELECT * FROM saved_topologies ORDER BY created_at_epoch_millis DESC")
    fun observeAll(): Flow<List<SavedTopologyEntity>>

    @Upsert
    suspend fun upsert(entity: SavedTopologyEntity)

    @Query("DELETE FROM saved_topologies WHERE id = :id")
    suspend fun deleteById(id: Long)
}
