package com.janncarlo.kronosim.presentation.screens.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janncarlo.kronosim.data.source.local.dao.SavedTopologyDao
import com.janncarlo.kronosim.data.source.local.entity.SavedTopologyEntity
import com.janncarlo.kronosim.domain.engine.ConsensusEngine
import com.janncarlo.kronosim.util.extensions.nowEpochMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SimulatorViewModel(
    private val engine: ConsensusEngine,
    private val topologyDao: SavedTopologyDao,
) : ViewModel() {

    private val isRunning = MutableStateFlow(true)
    private val selectedNodeId = MutableStateFlow<Int?>(null)
    private val snapshot = MutableStateFlow(engine.createTopology(nodeCount = 5))

    val uiState: StateFlow<SimulatorUiState> = combine(
        isRunning,
        snapshot,
        selectedNodeId,
        topologyDao.observeAll(),
    ) { running, currentSnapshot, selectedId, saved ->
        SimulatorUiState(
            isRunning = running,
            tick = currentSnapshot.tick,
            nodes = currentSnapshot.nodes,
            links = currentSnapshot.links,
            packets = currentSnapshot.packets,
            events = currentSnapshot.events,
            savedTopologies = saved,
            selectedNodeId = selectedId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SimulatorUiState(),
    )

    init {
        viewModelScope.launch {
            while (true) {
                delay(280L)
                if (isRunning.value) {
                    snapshot.update { engine.step(it) }
                }
            }
        }
    }

    fun onUiEvent(event: SimulatorUiEvent) {
        when (event) {
            SimulatorUiEvent.TogglePlayPause -> isRunning.update { !it }

            SimulatorUiEvent.StepOnce -> snapshot.update { engine.step(it) }

            SimulatorUiEvent.ProposeWrite -> snapshot.update { engine.proposeClientWrite(it) }

            is SimulatorUiEvent.ChangeNodeCount -> {
                selectedNodeId.value = null
                snapshot.value = engine.createTopology(event.count)
            }

            is SimulatorUiEvent.NodeTapped -> {
                selectedNodeId.value = event.nodeId
                snapshot.update { engine.toggleNodePower(it, event.nodeId) }
            }

            is SimulatorUiEvent.LinkTapped -> {
                snapshot.update { engine.toggleLinkPartition(it, event.fromId, event.toId) }
            }

            is SimulatorUiEvent.ScenarioSelected -> {
                selectedNodeId.value = null
                snapshot.update { engine.applyScenario(it, event.scenario) }
            }

            is SimulatorUiEvent.SaveCurrentTopology -> saveTopology(event.name)

            is SimulatorUiEvent.LoadSavedTopology -> loadTopology(event.topology)
        }
    }

    private fun saveTopology(name: String) {
        val current = snapshot.value
        val partitioned = current.links
            .filter { it.isPartitioned }
            .joinToString(";") { "${it.fromNodeId}-${it.toNodeId}" }
        val offline = current.nodes
            .filter { !it.isAlive }
            .joinToString(",") { it.id.toString() }

        viewModelScope.launch {
            topologyDao.upsert(
                SavedTopologyEntity(
                    name = name.ifBlank { "Cluster-${current.nodes.size}N" },
                    nodeCount = current.nodes.size,
                    partitionedPairsEncoded = partitioned,
                    offlineNodeIdsEncoded = offline,
                    createdAtEpochMillis = nowEpochMillis(),
                ),
            )
        }
    }

    private fun loadTopology(entity: SavedTopologyEntity) {
        var rebuilt = engine.createTopology(entity.nodeCount)
        entity.offlineNodeIdsEncoded
            .split(",")
            .mapNotNull { it.toIntOrNull() }
            .forEach { id -> rebuilt = engine.toggleNodePower(rebuilt, id) }
        entity.partitionedPairsEncoded
            .split(";")
            .mapNotNull { pair ->
                val parts = pair.split("-")
                if (parts.size == 2) {
                    val a = parts[0].toIntOrNull()
                    val b = parts[1].toIntOrNull()
                    if (a != null && b != null) a to b else null
                } else {
                    null
                }
            }
            .forEach { (a, b) -> rebuilt = engine.toggleLinkPartition(rebuilt, a, b) }
        snapshot.value = rebuilt
    }
}
