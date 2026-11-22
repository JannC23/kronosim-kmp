package com.janncarlo.kronosim.domain.engine

import com.janncarlo.kronosim.domain.model.ClusterNode
import com.janncarlo.kronosim.domain.model.NetworkLink

fun alivePartitions(
    nodes: List<ClusterNode>,
    links: List<NetworkLink>,
): List<Set<Int>> {
    val aliveIds = nodes.filter { it.isAlive }.map { it.id }.toSet()
    val unseen = aliveIds.toMutableSet()
    val partitions = mutableListOf<Set<Int>>()

    while (unseen.isNotEmpty()) {
        val start = unseen.first()
        val component = mutableSetOf(start)
        val queue = ArrayDeque(listOf(start))
        unseen.remove(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val neighbors = links
                .filter { !it.isPartitioned && (it.fromNodeId == current || it.toNodeId == current) }
                .mapNotNull { link ->
                    when (current) {
                        link.fromNodeId -> link.toNodeId
                        link.toNodeId -> link.fromNodeId
                        else -> null
                    }
                }
                .filter { it in unseen }

            for (neighbor in neighbors) {
                unseen.remove(neighbor)
                component += neighbor
                queue.addLast(neighbor)
            }
        }
        partitions += component
    }

    return partitions
}
