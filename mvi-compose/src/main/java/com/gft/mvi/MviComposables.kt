package com.gft.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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

@Composable
inline fun <reified VS : ViewState> ViewState(
    viewModel: MviViewModel<VS, *, *, *>,
    crossinline consumer: @Composable ViewStateProvider<VS>.() -> Unit
) {
    val state = viewModel.viewStates.collectAsStateWithLifecycle()
    ViewStateProvider(state).consumer()
}

class ViewStateProvider<VS : ViewState>(private val state: State<VS>) {
    val viewState: VS
        get() {
            return state.value
        }
}

@Composable
fun <VS : ViewState> viewState(
    viewModel: MviViewModel<VS, *, *, *>,
): ReadOnlyProperty<Any?, VS> {
    val state = viewModel.viewStates.collectAsStateWithLifecycle()
    return ViewStateDelegate(state)
}

private class ViewStateDelegate<VS : ViewState>(private val state: State<VS>) : ReadOnlyProperty<Any?, VS> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): VS {
        return state.value
    }
}
