package com.janncarlo.kronosim.presentation.screens.simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janncarlo.kronosim.domain.engine.ConsensusEngine
import com.janncarlo.kronosim.domain.model.ChaosScenario
import com.janncarlo.kronosim.domain.model.NodeRole
import com.janncarlo.kronosim.domain.model.PacketType
import kotlin.math.hypot

@Composable
fun SimulatorScreen(
    viewModel: SimulatorViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SimulatorScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    uiState: SimulatorUiState,
    onUiEvent: (SimulatorUiEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KronoSim • Raft Consensus Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Tick #${uiState.tick} • Quorum ${uiState.largestPartitionSize}/${uiState.nodes.size} (min ${uiState.quorumThreshold})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    QuorumStatusBadge(status = uiState.quorumStatus)
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ClusterTopologyCanvas(
                    uiState = uiState,
                    onNodeTap = { onUiEvent(SimulatorUiEvent.NodeTapped(it)) },
                    onLinkTap = { a, b -> onUiEvent(SimulatorUiEvent.LinkTapped(a, b)) },
                )
            }

            item {
                Text(
                    text = "Tap a node to crash/recover • Tap a link midpoint to sever it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 600.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onUiEvent(SimulatorUiEvent.ProposeWrite) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Client Write")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = { onUiEvent(SimulatorUiEvent.TogglePlayPause) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(if (uiState.isRunning) "Pause" else "Resume")
                                }
                                OutlinedButton(
                                    onClick = { onUiEvent(SimulatorUiEvent.StepOnce) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Step")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onUiEvent(SimulatorUiEvent.HealNetwork) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Heal Network")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onUiEvent(SimulatorUiEvent.SaveCurrentTopology("Snapshot #${uiState.tick}"))
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { onUiEvent(SimulatorUiEvent.ProposeWrite) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Client Write")
                            }
                            FilledTonalButton(
                                onClick = { onUiEvent(SimulatorUiEvent.TogglePlayPause) },
                            ) {
                                Text(if (uiState.isRunning) "Pause" else "Resume")
                            }
                            OutlinedButton(
                                onClick = { onUiEvent(SimulatorUiEvent.StepOnce) },
                            ) {
                                Text("Step")
                            }
                            OutlinedButton(
                                onClick = { onUiEvent(SimulatorUiEvent.HealNetwork) },
                            ) {
                                Text("Heal Network")
                            }
                            OutlinedButton(
                                onClick = {
                                    onUiEvent(SimulatorUiEvent.SaveCurrentTopology("Snapshot #${uiState.tick}"))
                                },
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }

            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 600.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(3, 5, 7).forEach { count ->
                                    FilterChip(
                                        selected = uiState.nodes.size == count,
                                        onClick = { onUiEvent(SimulatorUiEvent.ChangeNodeCount(count)) },
                                        label = { Text("${count}N") },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ChaosScenario.entries.forEach { scenario ->
                                    val label = if (scenario == ChaosScenario.HEALTHY_QUORUM) {
                                        "Healthy ${uiState.nodes.size}-Node Cluster"
                                    } else {
                                        scenario.displayName
                                    }
                                    AssistChip(
                                        onClick = { onUiEvent(SimulatorUiEvent.ScenarioSelected(scenario)) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(3, 5, 7).forEach { count ->
                                FilterChip(
                                    selected = uiState.nodes.size == count,
                                    onClick = { onUiEvent(SimulatorUiEvent.ChangeNodeCount(count)) },
                                    label = { Text("${count}N") },
                                )
                            }
                            ChaosScenario.entries.forEach { scenario ->
                                val label = if (scenario == ChaosScenario.HEALTHY_QUORUM) {
                                    "Healthy ${uiState.nodes.size}-Node Cluster"
                                } else {
                                    scenario.displayName
                                }
                                AssistChip(
                                    onClick = { onUiEvent(SimulatorUiEvent.ScenarioSelected(scenario)) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.savedTopologies.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.savedTopologies.forEach { saved ->
                            AssistChip(
                                onClick = { onUiEvent(SimulatorUiEvent.LoadSavedTopology(saved)) },
                                label = { Text("Load: ${saved.name}") },
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Live Consensus Telemetry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            items(
                items = uiState.events,
                key = { it.id },
            ) { event ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (event.isAlert) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "[T+${event.tick}] ${event.title}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.detail,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuorumStatusBadge(status: QuorumStatus) {
    val (label, bgColor) = when (status) {
        QuorumStatus.HEALTHY -> "QUORUM OK" to Color(0xFF1B5E20)
        QuorumStatus.PARTITIONED -> "NETWORK SPLIT" to Color(0xFFE65100)
        QuorumStatus.LOST -> "NO QUORUM" to Color(0xFFB71C1C)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(end = 12.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ClusterTopologyCanvas(
    uiState: SimulatorUiState,
    onNodeTap: (Int) -> Unit,
    onLinkTap: (Int, Int) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .background(Color(0xFF12101A)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(uiState.nodes, uiState.links) {
                        detectTapGestures { tapOffset ->
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            val hitRadius = 46f

                            val tappedNode = uiState.nodes.firstOrNull { node ->
                                val center = Offset(node.normalizedX * width, node.normalizedY * height)
                                hypot(tapOffset.x - center.x, tapOffset.y - center.y) <= hitRadius
                            }
                            if (tappedNode != null) {
                                onNodeTap(tappedNode.id)
                                return@detectTapGestures
                            }

                            val nodeMap = uiState.nodes.associateBy { it.id }
                            val tappedLink = uiState.links.firstOrNull { link ->
                                val a = nodeMap[link.fromNodeId] ?: return@firstOrNull false
                                val b = nodeMap[link.toNodeId] ?: return@firstOrNull false
                                val midX = ((a.normalizedX + b.normalizedX) * 0.5f) * width
                                val midY = ((a.normalizedY + b.normalizedY) * 0.5f) * height
                                hypot(tapOffset.x - midX, tapOffset.y - midY) <= 34f
                            }
                            if (tappedLink != null) {
                                onLinkTap(tappedLink.fromNodeId, tappedLink.toNodeId)
                            }
                        }
                    },
            ) {
                val nodePositions = uiState.nodes.associate { node ->
                    node.id to Offset(node.normalizedX * size.width, node.normalizedY * size.height)
                }

                uiState.links.forEach { link ->
                    val start = nodePositions[link.fromNodeId] ?: return@forEach
                    val end = nodePositions[link.toNodeId] ?: return@forEach
                    drawLine(
                        color = if (link.isPartitioned) Color(0xFFEF5350) else Color(0xFF4A4458),
                        start = start,
                        end = end,
                        strokeWidth = if (link.isPartitioned) 3f else 2f,
                        pathEffect = if (link.isPartitioned) {
                            PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        } else {
                            null
                        },
                    )
                }

                uiState.packets.forEach { packet ->
                    val start = nodePositions[packet.fromNodeId] ?: return@forEach
                    val end = nodePositions[packet.toNodeId] ?: return@forEach
                    val pos = Offset(
                        x = start.x + (end.x - start.x) * packet.progress,
                        y = start.y + (end.y - start.y) * packet.progress,
                    )
                    val packetColor = when (packet.type) {
                        PacketType.HEARTBEAT -> Color(0xFF4DD0E1)
                        PacketType.VOTE_REQUEST -> Color(0xFFFFD54F)
                        PacketType.APPEND_ENTRIES -> Color(0xFF81C784)
                    }
                    drawCircle(color = packetColor, radius = 7f, center = pos)
                }

                uiState.nodes.forEach { node ->
                    val center = nodePositions[node.id] ?: return@forEach
                    val nodeColor = when (node.role) {
                        NodeRole.LEADER -> Color(0xFF7F52FF)
                        NodeRole.CANDIDATE -> Color(0xFFFFB300)
                        NodeRole.FOLLOWER -> Color(0xFF26A69A)
                        NodeRole.OFFLINE -> Color(0xFF424242)
                    }

                    if (node.isAlive && node.role != NodeRole.LEADER) {
                        drawArc(
                            color = Color(0xFFFFD54F).copy(alpha = 0.65f),
                            startAngle = -90f,
                            sweepAngle = 360f * node.electionProgress,
                            useCenter = false,
                            topLeft = Offset(center.x - 36f, center.y - 36f),
                            size = Size(72f, 72f),
                            style = Stroke(width = 4f),
                        )
                    }

                    drawCircle(color = nodeColor, radius = 30f, center = center)

                    val titleLayout = textMeasurer.measure(
                        text = "${node.label}\nT${node.currentTerm}•L${node.logEntries.size}",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    drawText(
                        textLayoutResult = titleLayout,
                        topLeft = Offset(
                            x = center.x - (titleLayout.size.width / 2f),
                            y = center.y - (titleLayout.size.height / 2f),
                        ),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SimulatorScreenPreview() {
    val snapshot = ConsensusEngine().createTopology(5)
    MaterialTheme {
        SimulatorScreen(
            uiState = SimulatorUiState(
                nodes = snapshot.nodes,
                links = snapshot.links,
                events = snapshot.events,
            ),
            onUiEvent = {},
        )
    }
}
