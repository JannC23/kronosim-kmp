package com.janncarlo.kronosim

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.janncarlo.kronosim.root.App
import com.janncarlo.kronosim.root.AppInitializer

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppInitializer.initialize {}
    ComposeViewport {
        App()
    }
}
