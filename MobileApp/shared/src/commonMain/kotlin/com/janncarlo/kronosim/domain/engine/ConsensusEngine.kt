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

    private data class StepContext(
        var nextEventId: Long,
    )

    fun createTopology(nodeCount: Int): ClusterSnapshot {
        val clampedCount = nodeCount.coerceIn(MIN_NODES, MAX_NODES)
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
        ChaosScenario.HEALTHY_QUORUM ->
            createTopology(current.nodes.size.coerceIn(MIN_NODES, MAX_NODES))

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
            if (link.connects(fromId, toId)) {
                link.copy(isPartitioned = !link.isPartitioned)
            } else {
                link
            }
        }
        val toggled = updatedLinks.find { it.connects(fromId, toId) } ?: return snapshot
        return appendEvent(
            snapshot = snapshot.copy(
                links = updatedLinks,
                packets = snapshot.packets.filter { packet ->
                    isConnected(packet.fromNodeId, packet.toNodeId, updatedLinks)
                },
            ),
            title = if (toggled.isPartitioned) {
                "Link N$fromId <-> N$toId Severed"
            } else {
                "Link N$fromId <-> N$toId Restored"
            },
            detail = if (toggled.isPartitioned) {
                "Packets dropped between N$fromId and N$toId."
            } else {
                "Network route healed; log reconciliation enabled."
            },
            isAlert = toggled.isPartitioned,
        )
    }

    fun healNetwork(snapshot: ClusterSnapshot): ClusterSnapshot {
        val restoredLinks = snapshot.links.map { it.copy(isPartitioned = false) }
        return appendEvent(
            snapshot = snapshot.copy(
                links = restoredLinks,
                packets = snapshot.packets.filter { packet ->
                    isConnected(packet.fromNodeId, packet.toNodeId, restoredLinks)
                },
            ),
            title = "Network Partition Healed",
            detail = "All links restored; the cluster can reconcile logs and converge.",
            isAlert = false,
        )
    }

    fun proposeClientWrite(snapshot: ClusterSnapshot): ClusterSnapshot {
        val leader = snapshot.nodes
            .filter { it.role == NodeRole.LEADER && it.isAlive }
            .maxByOrNull { it.currentTerm }
            ?: return appendEvent(
                snapshot = snapshot,
                title = "Write Rejected (No Leader)",
                detail = "Cluster has no active leader to accept writes.",
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

        val leaderWithWrite = updatedNodes.first { it.id == leader.id }
        val packets = reachablePeers(leader.id, updatedNodes, snapshot.links).map { peer ->
            NetworkPacket(
                id = nextEventId(snapshot),
                fromNodeId = leader.id,
                toNodeId = peer.id,
                type = PacketType.APPEND_ENTRIES,
                progress = PACKET_SPEED,
            )
        }

        return appendEvent(
            snapshot = snapshot.copy(
                nodes = updatedNodes,
                packets = snapshot.packets + packets,
                commandCounter = snapshot.commandCounter + 1,
            ),
            title = "Client Write -> ${leader.label}",
            detail = "Appended [$commandName] at index #$nextIndex (Term ${leaderWithWrite.currentTerm}).",
        )
    }

    fun step(snapshot: ClusterSnapshot): ClusterSnapshot {
        val nextTick = snapshot.tick + 1L
        var nodes = snapshot.nodes.toMutableList()
        var events = snapshot.events
        var survivingPackets = mutableListOf<NetworkPacket>()
        val context = StepContext(
            nextEventId = (snapshot.events.maxOfOrNull { it.id } ?: 0L) + 1L,
        )

        advancePackets(snapshot, nodes, survivingPackets)

        commitReplicatedEntries(snapshot, nodes, context)?.let { commitEvent ->
            events = (listOf(commitEvent) + events).take(MAX_EVENTS)
        }

        sendHeartbeats(snapshot, nextTick, nodes, survivingPackets)

        advanceElectionTimers(nextTick, snapshot, nodes, context)?.let { timerEvent ->
            events = (listOf(timerEvent) + events).take(MAX_EVENTS)
        }

        val electionEvents = holdElections(nextTick, snapshot, nodes, context)
        events = (electionEvents + events).take(MAX_EVENTS)

        return snapshot.copy(
            tick = nextTick,
            nodes = nodes,
            packets = survivingPackets,
            events = events,
        )
    }

    private fun advancePackets(
        snapshot: ClusterSnapshot,
        nodes: MutableList<ClusterNode>,
        survivingPackets: MutableList<NetworkPacket>,
    ) {
        for (packet in snapshot.packets) {
            if (!isConnected(packet.fromNodeId, packet.toNodeId, snapshot.links)) continue

            val nextProgress = packet.progress + PACKET_SPEED
            if (nextProgress < 1f) {
                survivingPackets += packet.copy(progress = nextProgress)
                continue
            }

            val receiverIdx = nodes.indexOfFirst { it.id == packet.toNodeId }
            val sender = nodes.find { it.id == packet.fromNodeId }
            if (receiverIdx == -1 || sender == null) continue

            val receiver = nodes[receiverIdx]
            if (!receiver.isAlive || !sender.isAlive) continue

            when (packet.type) {
                PacketType.HEARTBEAT,
                PacketType.APPEND_ENTRIES,
                -> deliverAppendEntries(nodes, receiverIdx, sender)

                PacketType.VOTE_REQUEST -> Unit
            }
        }
    }

    private fun deliverAppendEntries(
        nodes: MutableList<ClusterNode>,
        receiverIdx: Int,
        sender: ClusterNode,
    ) {
        val receiver = nodes[receiverIdx]
        if (sender.role != NodeRole.LEADER) return

        if (sender.currentTerm > receiver.currentTerm) {
            nodes[receiverIdx] = receiver.copy(
                role = NodeRole.FOLLOWER,
                currentTerm = sender.currentTerm,
                electionProgress = 0f,
                votedFor = null,
            )
        } else if (sender.currentTerm == receiver.currentTerm &&
            receiver.role != NodeRole.LEADER
        ) {
            nodes[receiverIdx] = receiver.copy(
                role = NodeRole.FOLLOWER,
                electionProgress = 0f,
            )
        } else if (sender.currentTerm < receiver.currentTerm &&
            sender.role == NodeRole.LEADER
        ) {
            val senderIdx = nodes.indexOfFirst { it.id == sender.id }
            if (senderIdx != -1) {
                nodes[senderIdx] = sender.copy(
                    role = NodeRole.FOLLOWER,
                    currentTerm = receiver.currentTerm,
                    electionProgress = 0f,
                )
            }
            return
        }

        if (sender.currentTerm >= receiver.currentTerm) {
            val reconciled = reconcileLogs(receiver.logEntries, sender.logEntries)
            val finalReceiver = nodes[receiverIdx]
            nodes[receiverIdx] = finalReceiver.copy(
                logEntries = reconciled,
                commitIndex = maxOf(finalReceiver.commitIndex, sender.commitIndex),
            ).withCommittedEntries()
        }
    }

    private fun reconcileLogs(
        receiverLog: List<LogEntry>,
        senderLog: List<LogEntry>,
    ): List<LogEntry> {
        val committedIndexes = receiverLog
            .filter { it.isCommitted }
            .associateBy { it.index }
        val reconciled = senderLog.map { entry ->
            val committed = committedIndexes[entry.index]?.isCommitted == true
            entry.copy(isCommitted = committed || entry.isCommitted)
        }.toMutableList()

        for (committed in committedIndexes.values) {
            if (reconciled.none { it.index == committed.index }) {
                reconciled += committed
            }
        }
        return reconciled.sortedBy { it.index }
    }

    private fun commitReplicatedEntries(
        snapshot: ClusterSnapshot,
        nodes: MutableList<ClusterNode>,
        context: StepContext,
    ): SimulationEvent? {
        val quorumSize = snapshot.nodes.size / 2 + 1
        var newCommitEvent: SimulationEvent? = null

        for (leader in nodes.filter { it.role == NodeRole.LEADER && it.isAlive }.toList()) {
            val partition = reachablePeers(leader.id, nodes, snapshot.links) + leader
            val partitionIds = partition.map { it.id }.toSet()
            if (partition.size < quorumSize) continue

            val maxLogIndex = leader.logEntries.maxOfOrNull { it.index } ?: 0
            var newCommitIndex = leader.commitIndex
            for (index in (leader.commitIndex + 1)..maxLogIndex) {
                val entry = leader.logEntries.firstOrNull { it.index == index } ?: continue
                val replicas = nodes.count { node ->
                    node.id in partitionIds &&
                        node.isAlive &&
                        node.logEntries.any { it.index == index && it.term == entry.term }
                }
                if (replicas >= quorumSize) newCommitIndex = index
            }

            if (newCommitIndex <= leader.commitIndex) continue

            for (i in nodes.indices) {
                val node = nodes[i]
                if (node.id in partitionIds && node.isAlive) {
                    nodes[i] = node.copy(commitIndex = maxOf(node.commitIndex, newCommitIndex))
                        .withCommittedEntries(newCommitIndex)
                }
            }

            newCommitEvent = SimulationEvent(
                id = context.nextEventId++,
                tick = snapshot.tick + 1L,
                title = "Quorum Commit #$newCommitIndex",
                detail = "Partition replicated entry #$newCommitIndex to $quorumSize/${snapshot.nodes.size} nodes.",
            )
        }
        return newCommitEvent
    }

    private fun sendHeartbeats(
        snapshot: ClusterSnapshot,
        nextTick: Long,
        nodes: List<ClusterNode>,
        survivingPackets: MutableList<NetworkPacket>,
    ) {
        if (nextTick % HEARTBEAT_INTERVAL_TICKS != 0L) return

        for (leader in nodes.filter { it.role == NodeRole.LEADER && it.isAlive }) {
            for (peer in reachablePeers(leader.id, nodes, snapshot.links)) {
                survivingPackets += NetworkPacket(
                    id = nextEventId(snapshot),
                    fromNodeId = leader.id,
                    toNodeId = peer.id,
                    type = if (peer.logEntries.size < leader.logEntries.size) {
                        PacketType.APPEND_ENTRIES
                    } else {
                        PacketType.HEARTBEAT
                    },
                    progress = PACKET_SPEED,
                )
            }
        }
    }

    private fun advanceElectionTimers(
        nextTick: Long,
        snapshot: ClusterSnapshot,
        nodes: MutableList<ClusterNode>,
        context: StepContext,
    ): SimulationEvent? {
        var event: SimulationEvent? = null
        for (i in nodes.indices) {
            val node = nodes[i]
            if (!node.isAlive || node.role == NodeRole.LEADER) continue

            val increment = ELECTION_BASE_PROGRESS + random.nextFloat() * ELECTION_JITTER
            val incremented = node.electionProgress + increment
            if (incremented < 1f) {
                nodes[i] = node.copy(electionProgress = incremented)
            } else if (node.role == NodeRole.CANDIDATE) {
                nodes[i] = node.copy(electionProgress = CANDIDATE_RETRY_PROGRESS)
            } else {
                nodes[i] = node.copy(
                    role = NodeRole.CANDIDATE,
                    electionProgress = CANDIDATE_RETRY_PROGRESS,
                )
                event = SimulationEvent(
                    id = context.nextEventId++,
                    tick = nextTick,
                    title = "${node.label} Election Timeout",
                    detail = "Term ${node.currentTerm + 1} election started.",
                    isAlert = true,
                )
            }
        }
        return event
    }

    private fun holdElections(
        nextTick: Long,
        snapshot: ClusterSnapshot,
        nodes: MutableList<ClusterNode>,
        context: StepContext,
    ): List<SimulationEvent> {
        val quorumSize = snapshot.nodes.size / 2 + 1
        val events = mutableListOf<SimulationEvent>()
        val candidates = nodes.filter { it.role == NodeRole.CANDIDATE && it.isAlive }.toList()

        for (candidate in candidates) {
            val candidateIndex = nodes.indexOfFirst { it.id == candidate.id }
            if (candidateIndex == -1) continue
            if (nodes[candidateIndex].role != NodeRole.CANDIDATE) continue

            val newTerm = candidate.currentTerm + 1
            val reachable = reachablePeers(candidate.id, nodes, snapshot.links) +
                nodes[candidateIndex]
            val voters = reachable.filter { peer ->
                peer.currentTerm <= newTerm &&
                    logIsAtLeastAsCurrent(candidate, peer)
            }

            if (reachable.size >= quorumSize && voters.size >= quorumSize) {
                for (i in nodes.indices) {
                    val peer = nodes[i]
                    if (peer.id in reachable.map { it.id }.toSet()) {
                        nodes[i] = peer.copy(
                            role = if (peer.id == candidate.id) NodeRole.LEADER else NodeRole.FOLLOWER,
                            currentTerm = newTerm,
                            electionProgress = 0f,
                            votedFor = if (peer.id == candidate.id) candidate.id else peer.votedFor,
                        )
                    }
                }
                events += SimulationEvent(
                    id = context.nextEventId++,
                    tick = nextTick,
                    title = "${candidate.label} Elected Leader (Term $newTerm)",
                    detail = "Won $quorumSize-node quorum in a reachable partition.",
                )
            } else {
                nodes[candidateIndex] = candidate.copy(
                    role = NodeRole.CANDIDATE,
                    currentTerm = newTerm,
                    electionProgress = CANDIDATE_RETRY_PROGRESS,
                )
            }
        }
        return events
    }

    private fun logIsAtLeastAsCurrent(
        candidate: ClusterNode,
        peer: ClusterNode,
    ): Boolean {
        val candidateLast = candidate.logEntries.lastOrNull()
        val peerLast = peer.logEntries.lastOrNull()
        val candidateTerm = candidateLast?.term ?: 0
        val peerTerm = peerLast?.term ?: 0
        return if (candidateTerm != peerTerm) {
            candidateTerm >= peerTerm
        } else {
            (candidateLast?.index ?: 0) >= (peerLast?.index ?: 0)
        }
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

    private fun ClusterNode.withCommittedEntries(
        commitIndex: Int = this.commitIndex,
    ): ClusterNode = copy(
        commitIndex = commitIndex,
        logEntries = logEntries.map { entry ->
            if (entry.index <= commitIndex) entry.copy(isCommitted = true) else entry
        },
    )

    private fun appendEvent(
        snapshot: ClusterSnapshot,
        title: String,
        detail: String,
        isAlert: Boolean = false,
    ): ClusterSnapshot {
        val event = SimulationEvent(
            id = nextEventId(snapshot),
            tick = snapshot.tick,
            title = title,
            detail = detail,
            isAlert = isAlert,
        )
        return snapshot.copy(events = (listOf(event) + snapshot.events).take(MAX_EVENTS))
    }

    private fun nextEventId(snapshot: ClusterSnapshot): Long = (snapshot.events.maxOfOrNull { it.id } ?: 0L) + 1L

    private companion object {
        const val MIN_NODES = 3
        const val MAX_NODES = 7
        const val HEARTBEAT_INTERVAL_TICKS = 4L
        const val PACKET_SPEED = 0.25f
        const val ELECTION_BASE_PROGRESS = 0.07f
        const val ELECTION_JITTER = 0.05f
        const val CANDIDATE_RETRY_PROGRESS = 0.15f
        const val MAX_EVENTS = 25
    }
}
