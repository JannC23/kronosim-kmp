package com.janncarlo.kronosim.presentation.navigation

import androidx.compose.runtime.Composable
import com.janncarlo.kronosim.presentation.screens.simulator.SimulatorScreen
import com.janncarlo.kronosim.presentation.screens.simulator.SimulatorViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigation() {
    val viewModel = koinViewModel<SimulatorViewModel>()
    SimulatorScreen(viewModel = viewModel)
}
