package com.gft.mvi

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import com.gft.data.ConsumableEvent

interface MviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> {
    val viewStates: State<VS>
    val viewEffects: State<ConsumableEvent<VE>?>
    val navigationEffects: State<ConsumableEvent<NE>?>
    fun onEvent(event: EV)

    fun MviViewModel<VS, EV, NE, VE>.dispatchNavigationEffect(effect: NE) {
        if (navigationEffects is MutableState) (navigationEffects as MutableState).value = ConsumableEvent(effect)
        else throw TypeCastException("MviViewModel.dispatchNavigationEffect extension supports MutableState only!")
    }

    fun MviViewModel<VS, EV, NE, VE>.dispatchViewEffect(effect: VE) {
        if (viewEffects is MutableState) (viewEffects as MutableState).value = ConsumableEvent(effect)
        else throw TypeCastException("MviViewModel.dispatchViewEffect extension supports MutableState only!")
    }

    var MviViewModel<VS, EV, NE, VE>.viewState: VS
        get() {
            return viewStates.value
        }
        set(value) {
            if (viewStates is MutableState) (viewStates as MutableState).value = value
            else throw TypeCastException("MviViewModel.viewState extension supports MutableState only!")
        }
}

inline val <VS : ViewState> MviViewModel<VS, *, *, *>.viewState: VS
    get() = viewStates.value
