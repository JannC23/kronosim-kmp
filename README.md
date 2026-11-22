# KronoSim — Raft Consensus Lab

KronoSim is an interactive, cross-platform laboratory for learning and debugging
distributed consensus. It uses a deterministic Raft-style simulation to show
leaders, election terms, heartbeats, log replication, commits, node crashes,
network partitions, and quorum behavior in real time.

The project was created individually for the **Kotlin Multiplatform Contest 2026**
by Jann Carlo Martínez Cardona. It began from the official **Koko KMP Contest
Starter Kit** and was transformed into a purpose-built application: the shared
consensus engine and Compose interface are original KronoSim work.

## Features

- 3-, 5-, or 7-node clusters
- Leader election and live election timers
- Heartbeats and append-entry packet visualization
- Client writes with partition-aware replication
- Crash and recovery of individual nodes
- Interactive sever/heal controls for network links
- Healthy 5-node, leader-crash, and split-brain scenarios
- Live three-state quorum status:
  **QUORUM OK**, **NETWORK SPLIT**, and **NO QUORUM**
- Event trace with unique, bounded history entries
- Room-based saved topology persistence
- Shared simulation logic that runs on Android, desktop, web, and iOS

## Quick start

Requirements:

- JDK 17 or newer
- Android SDK for Android builds
- A recent Gradle installation is not required; use the checked-in wrapper
- macOS and Xcode are required for iOS

Run from `MobileApp/`:

```bash
cd MobileApp

# JVM desktop
./gradlew :desktopApp:run

# Web/Wasm
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

The web app is served by the Gradle development server, normally at
`http://localhost:8080`.

For Android:

```bash
./gradlew :androidApp:installDebug
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme.

## How to test the key behavior

1. Start the app with the default 5-node cluster.
2. Use **Pause** to inspect a state or **Step** to advance exactly one tick.
3. Select **Split-Brain Partition**.
4. Observe that the {N1, N2} component is isolated from the {N3, N4, N5}
   majority and that **NETWORK SPLIT** replaces the previous alive-node indicator.
5. Send a **Client Write** before the majority elects a leader to see the
   minority write remain uncommitted.
6. Restore all links, wait for packet delivery and election convergence, and
   observe one leader and synchronized terms.
7. Choose **Leader Failover** to crash the leader and trigger a majority election.
8. Save the current topology, restart the app, and load the saved snapshot.

You can also crash or recover any node by tapping it. Tap the midpoint of a
network link to sever or heal that link.

## Kotlin Multiplatform architecture

The consensus engine, cluster model, partition analysis, and UI state live in
`shared/src/commonMain`. Compose Multiplatform renders the same app across the
supported targets.

```text
MobileApp/shared/src/
├── commonMain/
│   └── com/janncarlo/kronosim/
│       ├── domain/engine/     Raft simulation and partition analysis
│       ├── domain/model/     Nodes, links, packets, logs, and events
│       ├── data/             Room entities and topology DAO
│       └── presentation/     Shared Compose simulator UI and state
├── commonTest/               Consensus and partition tests
├── androidMain/
├── iosMain/
├── jvmMain/
└── wasmJsMain/
```

The `ConsensusEngine` deliberately exposes the simulation to every target
without platform-specific control flow. This makes the engine both a learning
tool and a testable implementation of split-brain, failover, commit, and
reconciliation behavior.

## Build and quality checks

```bash
cd MobileApp
./gradlew :shared:jvmTest
./gradlew spotlessCheck
```

To auto-format Kotlin and Gradle Kotlin sources:

```bash
./gradlew spotlessApply
```

JVM tests run on Linux. iOS builds require macOS/Xcode and Android builds require
the Android SDK. The project is configured for JDK 17 compilation and JDK 21 test
execution where supported.

## Known verification scope

- Desktop JVM: verified
- Desktop launch smoke test: verified
- Web/Wasm: compiled successfully
- JVM tests: verified
- Android debug APK: compiled successfully; runtime testing needs a connected device or emulator
- iOS: not yet verified on macOS in this environment

Do not rely on those platform statuses until you have reproduced them locally.

## Origin and license

KronoSim preserves the official Koko template history and license notices.
The starter kit is by Mirzamehdi Karimov and Measify Kft. KronoSim's simulation,
engineering, UI direction, and contest-specific implementation are by Jann Carlo
Martínez Cardona.

Released under the MIT License. See [LICENSE](LICENSE).
