package com.janncarlo.kronosim.domain.engine

import com.janncarlo.kronosim.domain.model.ChaosScenario
import com.janncarlo.kronosim.domain.model.ClusterNode
import com.janncarlo.kronosim.domain.model.LogEntry
import com.janncarlo.kronosim.domain.model.NetworkLink
import com.janncarlo.kronosim.domain.model.NetworkPacket
import com.janncarlo.kronosim.domain.model.NodeRole
import com.janncarlo.kronosim.domain.model.PacketType
import com.janncarlo.kronosim.domain.model.SimulationEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ConsensusEngine(
    private val random: Random = Random.Default,
) {
    data class ClusterSnapshot(
        val tick: Long = 0L,
        val nodes: List<ClusterNode> = emptyList(),
        val links: List<NetworkLink> = emptyList(),
        val packets: List<NetworkPacket> = emptyList(),
        val events: List<SimulationEvent> = emptyList(),
        val commandCounter: Int = 1,
    )

    fun createTopology(nodeCount: Int): ClusterSnapshot {
        val clampedCount = nodeCount.coerceIn(3, 7)
        val nodes = (1..clampedCount).map { index ->
            val angle = (2.0 * PI * (index - 1) / clampedCount) - (PI / 2.0)
            ClusterNode(
                id = index,
                label = "N$index",
                role = if (index == 1) NodeRole.LEADER else NodeRole.FOLLOWER,
                currentTerm = 1,
                electionProgress = if (index == 1) 0f else random.nextFloat() * 0.35f,
                normalizedX = (0.5 + 0.34 * cos(angle)).toFloat(),
                normalizedY = (0.5 + 0.34 * sin(angle)).toFloat(),
            )
        }

        val links = buildList {
            for (i in 1..clampedCount) {
                for (j in (i + 1)..clampedCount) {
                    add(NetworkLink(fromNodeId = i, toNodeId = j))
                }
            }
        }

        return ClusterSnapshot(
            tick = 0L,
            nodes = nodes,
            links = links,
            events = listOf(
                SimulationEvent(
                    id = 1L,
                    tick = 0L,
                    title = "Cluster Initialized ($clampedCount nodes)",
                    detail = "Node N1 elected initial leader for Term 1.",
                ),
            ),
        )
    }

    fun applyScenario(
        current: ClusterSnapshot,
        scenario: ChaosScenario,
    ): ClusterSnapshot = when (scenario) {
        ChaosScenario.HEALTHY_QUORUM -> createTopology(5)

        ChaosScenario.LEADER_CRASH -> {
            val base = if (current.nodes.size < 5) createTopology(5) else current
            val leader = base.nodes.firstOrNull { it.role == NodeRole.LEADER }
            if (leader != null) toggleNodePower(base, leader.id) else base
        }

        ChaosScenario.SPLIT_BRAIN -> {
            val base = createTopology(5)
            val updatedLinks = base.links.map { link ->
                val groupA = setOf(1, 2)
                val crossesPartition = (link.fromNodeId in groupA) != (link.toNodeId in groupA)
                link.copy(isPartitioned = crossesPartition)
            }
            appendEvent(
                snapshot = base.copy(links = updatedLinks),
                title = "Network Partition Injected",
                detail = "Minority {N1, N2} isolated from majority quorum {N3, N4, N5}.",
                isAlert = true,
            )
        }
    }

    fun toggleNodePower(
        snapshot: ClusterSnapshot,
        nodeId: Int,
    ): ClusterSnapshot {
        val target = snapshot.nodes.find { it.id == nodeId } ?: return snapshot
        val turningOff = target.isAlive
        val updatedNodes = snapshot.nodes.map { node ->
            if (node.id != nodeId) {
                node
            } else if (turningOff) {
                node.copy(role = NodeRole.OFFLINE, electionProgress = 0f, votedFor = null)
            } else {
                node.copy(role = NodeRole.FOLLOWER, electionProgress = 0.2f)
            }
        }
        return appendEvent(
            snapshot = snapshot.copy(nodes = updatedNodes),
            title = if (turningOff) "Node ${target.label} Crashed" else "Node ${target.label} Recovered",
            detail = if (turningOff) {
                "Simulated process fault on ${target.label}."
            } else {
                "${target.label} rejoined cluster as FOLLOWER in Term ${target.currentTerm}."
            },
            isAlert = turningOff,
        )
    }

    fun toggleLinkPartition(
        snapshot: ClusterSnapshot,
        fromId: Int,
        toId: Int,
    ): ClusterSnapshot {
        val updatedLinks = snapshot.links.map { link ->
            if (link.connects(fromId, toId)) link.copy(isPartitioned = !link.isPartitioned) else link
        }
        val toggled = updatedLinks.find { it.connects(fromId, toId) } ?: return snapshot
        return appendEvent(
            snapshot = snapshot.copy(links = updatedLinks),
            title = if (toggled.isPartitioned) "Link N$fromId <-> N$toId Severed" else "Link N$fromId <-> N$toId Restored",
            detail = if (toggled.isPartitioned) {
                "Packets dropped between N$fromId and N$toId."
            } else {
                "Network route healed; log reconciliation enabled."
            },
            isAlert = toggled.isPartitioned,
        )
    }

    fun proposeClientWrite(snapshot: ClusterSnapshot): ClusterSnapshot {
        val leader = snapshot.nodes.firstOrNull { it.role == NodeRole.LEADER }
            ?: return appendEvent(
                snapshot = snapshot,
                title = "Write Rejected (No Leader)",
                detail = "Cluster has no active leader with quorum to accept writes.",
                isAlert = true,
            )

        val commandName = "SET x=${snapshot.commandCounter * 10}"
        val nextIndex = (leader.logEntries.maxOfOrNull { it.index } ?: 0) + 1
        val newEntry = LogEntry(
            index = nextIndex,
            term = leader.currentTerm,
            command = commandName,
            isCommitted = false,
        )

        val updatedNodes = snapshot.nodes.map { node ->
            if (node.id == leader.id) node.copy(logEntries = node.logEntries + newEntry) else node
        }

        val replicationPackets = reachablePeers(leader.id, updatedNodes, snapshot.links)
            .mapIndexed { idx, peer ->
                NetworkPacket(
                    id = snapshot.tick * 100 + idx + 1,
                    fromNodeId = leader.id,
                    toNodeId = peer.id,
                    type = PacketType.APPEND_ENTRIES,
                    progress = 0.05f,
                )
            }

        return appendEvent(
            snapshot = snapshot.copy(
                nodes = updatedNodes,
                packets = snapshot.packets + replicationPackets,
                commandCounter = snapshot.commandCounter + 1,
            ),
            title = "Client Write -> ${leader.label}",
            detail = "Appended [$commandName] at index #$nextIndex (Term ${leader.currentTerm}).",
        )
    }

    fun step(snapshot: ClusterSnapshot): ClusterSnapshot {
        val nextTick = snapshot.tick + 1L
        var nodes = snapshot.nodes.toMutableList()
        val survivingPackets = mutableListOf<NetworkPacket>()
        var events = snapshot.events

        for (packet in snapshot.packets) {
            val nextProgress = packet.progress + 0.25f
            if (nextProgress < 1f) {
                survivingPackets += packet.copy(progress = nextProgress)
            } else {
                val receiverIdx = nodes.indexOfFirst { it.id == packet.toNodeId }
                val sender = nodes.find { it.id == packet.fromNodeId }
                if (receiverIdx != -1 && sender != null && nodes[receiverIdx].isAlive) {
                    val receiver = nodes[receiverIdx]
                    if (sender.currentTerm >= receiver.currentTerm) {
                        val syncedLogs = if (sender.logEntries.size >= receiver.logEntries.size) {
                            sender.logEntries
                        } else {
                            receiver.logEntries
                        }
                        nodes[receiverIdx] = receiver.copy(
                            role = if (receiver.role == NodeRole.CANDIDATE) NodeRole.FOLLOWER else receiver.role,
                            currentTerm = sender.currentTerm,
                            electionProgress = 0f,
                            logEntries = syncedLogs,
                            commitIndex = sender.commitIndex,
                        )
                    }
                }
            }
        }

        val quorumSize = (nodes.size / 2) + 1
        val leaderIdx = nodes.indexOfFirst { it.role == NodeRole.LEADER }
        if (leaderIdx != -1) {
            val leader = nodes[leaderIdx]
            val maxLogIndex = leader.logEntries.maxOfOrNull { it.index } ?: 0
            var newCommitIndex = leader.commitIndex
            for (idx in (leader.commitIndex + 1)..maxLogIndex) {
                val replicatedCount = nodes.count { n ->
                    n.isAlive && n.logEntries.any { it.index == idx && it.term == leader.currentTerm }
                }
                if (replicatedCount >= quorumSize) {
                    newCommitIndex = idx
                }
            }
            if (newCommitIndex > leader.commitIndex) {
                nodes = nodes.map { node ->
                    if (!node.isAlive) {
                        node
                    } else {
                        node.copy(
                            commitIndex = maxOf(node.commitIndex, newCommitIndex),
                            logEntries = node.logEntries.map { entry ->
                                if (entry.index <= newCommitIndex) entry.copy(isCommitted = true) else entry
                            },
                        )
                    }
                }.toMutableList()
                events = (
                    listOf(
                        SimulationEvent(
                            id = nextTick * 10 + 1,
                            tick = nextTick,
                            title = "Quorum Commit #$newCommitIndex",
                            detail = "Majority ($quorumSize/${nodes.size}) replicated entry #$newCommitIndex.",
                        ),
                    ) + events
                    ).take(25)
            }

            if (nextTick % 4L == 0L) {
                val activeLeader = nodes[leaderIdx]
                reachablePeers(activeLeader.id, nodes, snapshot.links).forEachIndexed { i, peer ->
                    survivingPackets += NetworkPacket(
                        id = nextTick * 1000 + i,
                        fromNodeId = activeLeader.id,
                        toNodeId = peer.id,
                        type = if (peer.logEntries.size < activeLeader.logEntries.size) {
                            PacketType.APPEND_ENTRIES
                        } else {
                            PacketType.HEARTBEAT
                        },
                        progress = 0.05f,
                    )
                }
            }
        }

        for (i in nodes.indices) {
            val node = nodes[i]
            if (!node.isAlive || node.role == NodeRole.LEADER) continue

            val incremented = node.electionProgress + (0.07f + random.nextFloat() * 0.05f)
            if (incremented < 1f) {
                nodes[i] = node.copy(electionProgress = incremented)
            } else {
                val newTerm = node.currentTerm + 1
                val voters = reachablePeers(node.id, nodes, snapshot.links)
                    .filter { it.currentTerm <= newTerm }
                val totalVotes = 1 + voters.size

                if (totalVotes >= quorumSize) {
                    for (j in nodes.indices) {
                        if (nodes[j].isAlive && nodes[j].id != node.id &&
                            isConnected(node.id, nodes[j].id, snapshot.links)
                        ) {
                            nodes[j] = nodes[j].copy(
                                role = NodeRole.FOLLOWER,
                                currentTerm = newTerm,
                                electionProgress = 0f,
                            )
                        }
                    }
                    nodes[i] = node.copy(
                        role = NodeRole.LEADER,
                        currentTerm = newTerm,
                        votedFor = node.id,
                        electionProgress = 0f,
                    )
                    events = (
                        listOf(
                            SimulationEvent(
                                id = nextTick * 10 + 2,
                                tick = nextTick,
                                title = "${node.label} Elected Leader (Term $newTerm)",
                                detail = "Won quorum with $totalVotes/${nodes.size} votes.",
                            ),
                        ) + events
                        ).take(25)
                } else {
                    nodes[i] = node.copy(
                        role = NodeRole.CANDIDATE,
                        currentTerm = newTerm,
                        electionProgress = 0.15f,
                    )
                }
            }
        }

        return snapshot.copy(
            tick = nextTick,
            nodes = nodes,
            packets = survivingPackets,
            events = events,
        )
    }

    private fun reachablePeers(
        sourceId: Int,
        nodes: List<ClusterNode>,
        links: List<NetworkLink>,
    ): List<ClusterNode> = nodes.filter { candidate ->
        candidate.id != sourceId &&
            candidate.isAlive &&
            isConnected(sourceId, candidate.id, links)
    }

    private fun isConnected(
        a: Int,
        b: Int,
        links: List<NetworkLink>,
    ): Boolean = links.any { it.connects(a, b) && !it.isPartitioned }

    private fun appendEvent(
        snapshot: ClusterSnapshot,
        title: String,
        detail: String,
        isAlert: Boolean = false,
    ): ClusterSnapshot {
        val event = SimulationEvent(
            id = snapshot.tick * 100 + snapshot.events.size + 1,
            tick = snapshot.tick,
            title = title,
            detail = detail,
            isAlert = isAlert,
        )
        return snapshot.copy(events = (listOf(event) + snapshot.events).take(25))
    }
}
