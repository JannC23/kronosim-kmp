package com.janncarlo.kronosim.domain.engine

import com.janncarlo.kronosim.domain.model.ChaosScenario
import com.janncarlo.kronosim.domain.model.NodeRole
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConsensusEngineTest {
    @Test
    fun `createTopology initializes N1 as leader and others as followers`() {
        val engine = ConsensusEngine(Random(42))
        val snapshot = engine.createTopology(5)

        assertEquals(5, snapshot.nodes.size)
        assertEquals(NodeRole.LEADER, snapshot.nodes.first().role)
        assertEquals(4, snapshot.nodes.count { it.role == NodeRole.FOLLOWER })
    }

    @Test
    fun `leader crash triggers new leader election via majority quorum`() {
        val engine = ConsensusEngine(Random(7))
        var snapshot = engine.createTopology(5)
        snapshot = engine.applyScenario(snapshot, ChaosScenario.LEADER_CRASH)

        repeat(25) {
            snapshot = engine.step(snapshot)
        }

        val newLeader = snapshot.nodes.firstOrNull { it.role == NodeRole.LEADER }
        assertNotNull(newLeader)
        assertTrue(newLeader.id != 1)
        assertTrue(newLeader.currentTerm >= 2)
    }

    @Test
    fun `client write commits across majority quorum`() {
        val engine = ConsensusEngine(Random(1))
        var snapshot = engine.createTopology(3)
        snapshot = engine.proposeClientWrite(snapshot)

        repeat(8) {
            snapshot = engine.step(snapshot)
        }

        val leader = snapshot.nodes.first { it.role == NodeRole.LEADER }
        assertEquals(1, leader.commitIndex)
        assertTrue(leader.logEntries.first().isCommitted)
    }

    @Test
    fun `split brain elects stable majority leader without term growth`() {
        val engine = ConsensusEngine(Random(1))
        var snapshot = engine.applyScenario(
            engine.createTopology(5),
            ChaosScenario.SPLIT_BRAIN,
        )

        repeat(60) {
            snapshot = engine.step(snapshot)
        }

        val majorityLeader = snapshot.nodes.firstOrNull {
            it.role == NodeRole.LEADER && it.id in setOf(3, 4, 5)
        }
        assertNotNull(majorityLeader)
        assertTrue(majorityLeader.currentTerm <= 3)
        assertEquals(snapshot.events.size, snapshot.events.map { it.id }.distinct().size)
    }

    @Test
    fun `minority write cannot commit while partitioned`() {
        val engine = ConsensusEngine(Random(1))
        var snapshot = engine.applyScenario(
            engine.createTopology(5),
            ChaosScenario.SPLIT_BRAIN,
        )
        snapshot = engine.proposeClientWrite(snapshot)

        repeat(30) {
            snapshot = engine.step(snapshot)
        }

        val leaders = snapshot.nodes.filter { it.role == NodeRole.LEADER }
        val minorityWriteEntry = leaders
            .flatMap { it.logEntries }
            .firstOrNull { it.command.startsWith("SET x=") }
        assertTrue(minorityWriteEntry == null || !minorityWriteEntry.isCommitted)
    }

    @Test
    fun `healed network converges to one leader and same term`() {
        val engine = ConsensusEngine(Random(1))
        var snapshot = engine.applyScenario(
            engine.createTopology(5),
            ChaosScenario.SPLIT_BRAIN,
        )
        repeat(40) {
            snapshot = engine.step(snapshot)
        }

        snapshot = snapshot.copy(
            links = snapshot.links.map { it.copy(isPartitioned = false) },
        )
        repeat(20) {
            snapshot = engine.step(snapshot)
        }

        val leaders = snapshot.nodes.filter { it.role == NodeRole.LEADER }
        assertEquals(1, leaders.size)
        assertTrue(snapshot.nodes.all { it.currentTerm == leaders.first().currentTerm })
    }

    @Test
    fun `alivePartitions identifies split brain components`() {
        val engine = ConsensusEngine(Random(1))
        val snapshot = engine.applyScenario(
            engine.createTopology(5),
            ChaosScenario.SPLIT_BRAIN,
        )

        val partitions = alivePartitions(snapshot.nodes, snapshot.links)

        assertEquals(setOf(setOf(1, 2), setOf(3, 4, 5)), partitions.map { it }.toSet())
    }

    @Test
    fun `healNetwork restores every severed link`() {
        val engine = ConsensusEngine(Random(1))
        val split = engine.applyScenario(
            engine.createTopology(5),
            ChaosScenario.SPLIT_BRAIN,
        )

        val healed = engine.healNetwork(split)

        assertTrue(healed.links.none { it.isPartitioned })
    }

    @Test
    fun `healthy scenario preserves selected node count`() {
        val engine = ConsensusEngine(Random(1))
        val sevenNodes = engine.createTopology(7)

        val healthy = engine.applyScenario(
            sevenNodes,
            ChaosScenario.HEALTHY_QUORUM,
        )

        assertEquals(7, healthy.nodes.size)
    }
}
