package com.janncarlo.kronosim.domain.model

enum class NodeRole {
    FOLLOWER,
    CANDIDATE,
    LEADER,
    OFFLINE,
}

data class ClusterNode(
    val id: Int,
    val label: String,
    val role: NodeRole = NodeRole.FOLLOWER,
    val currentTerm: Int = 1,
    val votedFor: Int? = null,
    val commitIndex: Int = 0,
    val logEntries: List<LogEntry> = emptyList(),
    val electionProgress: Float = 0f,
    val normalizedX: Float,
    val normalizedY: Float,
) {
    val isAlive: Boolean
        get() = role != NodeRole.OFFLINE
}

data class NetworkLink(
    val fromNodeId: Int,
    val toNodeId: Int,
    val isPartitioned: Boolean = false,
) {
    fun connects(a: Int, b: Int): Boolean = (fromNodeId == a && toNodeId == b) || (fromNodeId == b && toNodeId == a)
}

data class LogEntry(
    val index: Int,
    val term: Int,
    val command: String,
    val isCommitted: Boolean = false,
)

enum class PacketType {
    HEARTBEAT,
    VOTE_REQUEST,
    APPEND_ENTRIES,
}

data class NetworkPacket(
    val id: Long,
    val fromNodeId: Int,
    val toNodeId: Int,
    val type: PacketType,
    val progress: Float = 0f,
)

data class SimulationEvent(
    val id: Long,
    val tick: Long,
    val title: String,
    val detail: String,
    val isAlert: Boolean = false,
)

enum class ChaosScenario(
    val displayName: String,
    val description: String,
) {
    HEALTHY_QUORUM(
        displayName = "Healthy 5-Node Cluster",
        description = "Balanced quorum with active leader heartbeats and log replication.",
    ),
    LEADER_CRASH(
        displayName = "Leader Failover",
        description = "Kills the active leader to trigger a randomized timeout election.",
    ),
    SPLIT_BRAIN(
        displayName = "Split-Brain Partition",
        description = "Isolates 2 nodes from the 3-node majority quorum.",
    ),
}
