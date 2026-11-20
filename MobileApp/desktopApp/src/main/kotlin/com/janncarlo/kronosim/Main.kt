package com.janncarlo.kronosim

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.janncarlo.kronosim.root.App
import com.janncarlo.kronosim.root.AppInitializer

fun main() {
    System.setProperty("java.util.logging.SimpleFormatter.format", $$"%5$s%6$s%n")

    application {
        AppInitializer.initialize {}
        Window(
            onCloseRequest = ::exitApplication,
            title = "KronoSim",
        ) {
            App()
        }
    }
}
