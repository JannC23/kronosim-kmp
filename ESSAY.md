# KronoSim contest essay

I am Jann Carlo Martínez Cardona, a software engineering student at Universidad del Valle in Colombia. I have experience with Kotlin, Java, Python, web technologies, databases, Android, and Compose, and I enjoy turning complex systems into practical learning tools. My interests include distributed systems, developer education, and user interfaces that make invisible state understandable.

KronoSim began from the Kotlin Foundation's Koko KMP Contest Starter Kit, but it is a substantially original contest-specific application. The starter kit provided project scaffolding and build infrastructure; I designed and implemented the Raft consensus engine, its interactive topology, the split-brain rules, the telemetry, and the persistence workflow for this project.

KronoSim is a visual laboratory for the Raft consensus algorithm. It can create clusters with three, five, or seven nodes, simulate crashes and network partitions, send client writes, show heartbeats and packets, and expose election terms and committed logs. Users can sever individual links, recover nodes, save topologies, and replay realistic leader failovers. The simulator's main value is that participants can directly observe which operations can succeed without quorum and how a minority leader converges after the network heals.

The project uses Kotlin and Kotlin Multiplatform with Compose Multiplatform, Room, Coroutines, and Koin. The consensus engine, cluster model, partition analysis, and simulator state are shared Kotlin code rather than platform-specific copies. This shared engine currently supports desktop and web and is structured for Android and iOS. I used JVM tests and partition-focused scenarios to verify stable leaders, quorum commits, minority write rejection, recovery, and event identity. Through KronoSim, I want to demonstrate that Kotlin Multiplatform is not merely a UI-sharing tool: it can power a deterministic, testable simulation across platforms and make distributed systems genuinely approachable.

Together, these choices make KronoSim an educational resource and demonstration of practical Kotlin Multiplatform engineering for the 2026 contest.
