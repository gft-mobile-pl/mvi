package com.gft.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
fun <NE : NavigationEffect> NavigationEffect(viewModel: MviViewModel<*, *, NE, *>, consumer: (NE) -> Unit) {
    val consumableEvent = viewModel.navigationEffects.value
    SideEffect {
        consumableEvent?.consume(consumer)
    }
}

@Composable
fun <VE : ViewEffect> ViewEffect(viewModel: MviViewModel<*, *, *, VE>, consumer: (VE) -> Unit) {
    val consumableEvent = viewModel.viewEffects.value
    SideEffect {
        consumableEvent?.consume(consumer)
    }
}