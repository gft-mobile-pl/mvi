package com.gft.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gft.compose.toState
import com.gft.data.ConsumableEvent
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Composable
fun <NE : NavigationEffect> NavigationEffect(
    viewModel: MviViewModel<*, *, NE, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    consumer: (NE) -> Unit
) {
    val state = viewModel.navigationEffects.collectAsStateWithLifecycle(
        initialValue = null,
        minActiveState = minActiveState
    )
    ConsumeEvent(state, consumer)
}

@Composable
fun <VE : ViewEffect> ViewEffect(
    viewModel: MviViewModel<*, *, *, VE>,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    consumer: (VE) -> Unit
) {
    val state = viewModel.viewEffects.collectAsStateWithLifecycle(
        initialValue = null,
        minActiveState = minActiveState
    )
    ConsumeEvent(state, consumer)
}

@Composable
private fun <T> ConsumeEvent(state: State<ConsumableEvent<T>?>, consumer: (T) -> Unit) {
    val consumableEvent = state.value
    SideEffect {
        consumableEvent?.consume(consumer)
    }
}

@Composable
fun <VS : ViewState, EV : ViewEvent> ViewState(
    viewModel: MviViewModel<VS, EV, *, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    minActiveStateForViewEventsPropagation: Lifecycle.State = Lifecycle.State.RESUMED,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    consumer: @Composable ViewStateProvider<VS, EV>.() -> Unit
) {
    val state = viewModel.viewStates.toState(minActiveState = minActiveState)
    ViewStateProvider<VS, EV>(state) { event ->
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(minActiveStateForViewEventsPropagation)) {
            viewModel.onEvent(event)
        }
    }.consumer()
}

@Stable
class ViewStateProvider<VS : ViewState, EV : ViewEvent>(
    @PublishedApi internal val state: State<VS>,
    private val onEvent: (EV) -> Unit,
) {
    inline val viewState: VS
        get() {
            return state.value
        }

    fun dispatchViewEvent(event: EV) = onEvent(event)
}

@Composable
fun <VS : ViewState> viewState(
    viewModel: MviViewModel<VS, *, *, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
): ReadOnlyProperty<Any?, VS> {
    val state = viewModel.viewStates.toState(minActiveState = minActiveState)
    return ViewStateDelegate(state)
}

private class ViewStateDelegate<VS : ViewState>(private val state: State<VS>) : ReadOnlyProperty<Any?, VS> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): VS {
        return state.value
    }
}
