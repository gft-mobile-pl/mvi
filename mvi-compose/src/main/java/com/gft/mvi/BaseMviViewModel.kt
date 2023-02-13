package com.gft.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gft.data.ConsumableEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val VIEW_STATE_KEY = "MviViewMode.viewState"

abstract class BaseMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect>(
    initialState: VS,
    savedStateHandle: SavedStateHandle? = null
) : ViewModel(), MviViewModel<VS, EV, NE, VE> {
    override val viewStates: StateFlow<VS> = savedStateHandle?.getLiveData<VS>(VIEW_STATE_KEY)?.let { liveData ->
        MutableStateFlow(liveData.value ?: initialState).apply {
            viewModelScope.launch {
                collectLatest { newValue -> liveData.value = newValue }
            }
        }
    } ?: MutableStateFlow(initialState)
    override val viewEffects: StateFlow<ConsumableEvent<VE>?> = MutableStateFlow<ConsumableEvent<VE>?>(null)
    override val navigationEffects: StateFlow<ConsumableEvent<NE>?> = MutableStateFlow<ConsumableEvent<NE>?>(null)
}
