package com.janncarlo.kronosim.presentation.screens.simulator

import com.janncarlo.kronosim.data.source.local.entity.SavedTopologyEntity
import com.janncarlo.kronosim.domain.engine.alivePartitions
import com.janncarlo.kronosim.domain.model.ChaosScenario
import com.janncarlo.kronosim.domain.model.ClusterNode
import com.janncarlo.kronosim.domain.model.NetworkLink
import com.janncarlo.kronosim.domain.model.NetworkPacket
import com.janncarlo.kronosim.domain.model.NodeRole
import com.janncarlo.kronosim.domain.model.SimulationEvent

enum class QuorumStatus {
    HEALTHY,
    PARTITIONED,
    LOST,
}

data class SimulatorUiState(
    val isRunning: Boolean = true,
    val tick: Long = 0L,
    val nodes: List<ClusterNode> = emptyList(),
    val links: List<NetworkLink> = emptyList(),
    val packets: List<NetworkPacket> = emptyList(),
    val events: List<SimulationEvent> = emptyList(),
    val savedTopologies: List<SavedTopologyEntity> = emptyList(),
    val selectedNodeId: Int? = null,
) {
    val activeLeader: ClusterNode?
        get() = nodes.firstOrNull { it.role == NodeRole.LEADER }

    val aliveCount: Int
        get() = nodes.count { it.isAlive }

    val quorumThreshold: Int
        get() = (nodes.size / 2) + 1

    val largestPartitionSize: Int
        get() = alivePartitions(nodes, links).maxOfOrNull { it.size } ?: 0

    val quorumStatus: QuorumStatus
        get() {
            val partitions = alivePartitions(nodes, links)
            val quorumPartition = partitions.firstOrNull { partition ->
                partition.size >= quorumThreshold &&
                    nodes.any { it.id in partition && it.role == NodeRole.LEADER && it.isAlive }
            }
            return when {
                partitions.size == 1 && quorumPartition != null -> QuorumStatus.HEALTHY
                quorumPartition != null -> QuorumStatus.PARTITIONED
                else -> QuorumStatus.LOST
            }
        }
}

sealed interface SimulatorUiEvent {
    data object TogglePlayPause : SimulatorUiEvent
    data object StepOnce : SimulatorUiEvent
    data object ProposeWrite : SimulatorUiEvent
    data class ChangeNodeCount(val count: Int) : SimulatorUiEvent
    data class NodeTapped(val nodeId: Int) : SimulatorUiEvent
    data class LinkTapped(val fromId: Int, val toId: Int) : SimulatorUiEvent
    data class ScenarioSelected(val scenario: ChaosScenario) : SimulatorUiEvent
    data class SaveCurrentTopology(val name: String) : SimulatorUiEvent
    data class LoadSavedTopology(val topology: SavedTopologyEntity) : SimulatorUiEvent
}
