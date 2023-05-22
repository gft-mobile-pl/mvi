package com.gft.mvi

import com.gft.data.ConsumableEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface MviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> {
    val viewStates: StateFlow<VS>
    val viewEffects: StateFlow<ConsumableEvent<VE>?>
    val navigationEffects: StateFlow<ConsumableEvent<NE>?>
    fun onEvent(event: EV)

    fun MviViewModel<VS, EV, NE, VE>.dispatchNavigationEffect(effect: NE) {
        if (navigationEffects is MutableStateFlow) (navigationEffects as MutableStateFlow).value = ConsumableEvent(effect)
        else throw TypeCastException("MviViewModel.dispatchNavigationEffect extension supports MutableState only!")
    }

    fun MviViewModel<VS, EV, NE, VE>.clearNavigationEffect() {
        if (navigationEffects is MutableStateFlow) (navigationEffects as MutableStateFlow).value = null
        else throw TypeCastException("MviViewModel.clearNavigationEffect extension supports MutableState only!")
    }

    fun MviViewModel<VS, EV, NE, VE>.dispatchViewEffect(effect: VE) {
        if (viewEffects is MutableStateFlow) (viewEffects as MutableStateFlow).value = ConsumableEvent(effect)
        else throw TypeCastException("MviViewModel.dispatchViewEffect extension supports MutableState only!")
    }

    fun MviViewModel<VS, EV, NE, VE>.clearViewEffect(effect: VE) {
        if (viewEffects is MutableStateFlow) (viewEffects as MutableStateFlow).value = null
        else throw TypeCastException("MviViewModel.clearViewEffect extension supports MutableState only!")
    }

    var MviViewModel<VS, EV, NE, VE>.viewState: VS
        get() {
            return viewStates.value
        }
        set(value) {
            if (viewStates is MutableStateFlow) (viewStates as MutableStateFlow).value = value
            else throw TypeCastException("MviViewModel.viewState extension supports MutableState only!")
        }
}

inline val <VS : ViewState> MviViewModel<VS, *, *, *>.viewState: VS
    get() = viewStates.value
