package com.janncarlo.kronosim.data.source.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "saved_topologies")
data class SavedTopologyEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("id") val id: Long = 0L,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("node_count") val nodeCount: Int,
    @ColumnInfo("partitioned_pairs_encoded") val partitionedPairsEncoded: String,
    @ColumnInfo("offline_node_ids_encoded") val offlineNodeIdsEncoded: String,
    @ColumnInfo("created_at_epoch_millis") val createdAtEpochMillis: Long,
)
