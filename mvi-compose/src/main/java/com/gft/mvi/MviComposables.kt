package com.gft.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
inline fun <VS : ViewState> ViewState(
    viewModel: MviViewModel<VS, *, *, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    content: @Composable (State<VS>) -> Unit
) {
    content(viewModel.viewStates.collectAsStateWithLifecycle(minActiveState = minActiveState))
}

@Composable
fun <NE : NavigationEffect> NavigationEffect(
    viewModel: MviViewModel<*, *, NE, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    consumer: (NE) -> Unit
) {
    val state = viewModel.navigationEffects.collectAsStateWithLifecycle(minActiveState = minActiveState)
    val consumableEvent = state.value
    SideEffect {
        consumableEvent?.consume(consumer)
    }
}

@Composable
fun <VE : ViewEffect> ViewEffect(
    viewModel: MviViewModel<*, *, *, VE>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    consumer: (VE) -> Unit
) {
    val state = viewModel.viewEffects.collectAsStateWithLifecycle(minActiveState = minActiveState)
    val consumableEvent = state.value
    SideEffect {
        consumableEvent?.consume(consumer)
    }
}