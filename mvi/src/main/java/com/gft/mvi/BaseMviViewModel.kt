package com.gft.mvi

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.gft.data.ConsumableEvent

private const val VIEW_STATE_KEY = "MviViewMode.viewState"

@OptIn(SavedStateHandleSaveableApi::class)
abstract class BaseMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect>(
    initialState: VS,
    savedStateHandle: SavedStateHandle? = null
) : ViewModel(), MviViewModel<VS, EV, NE, VE> {
    override val viewStates: State<VS> = savedStateHandle?.saveable(VIEW_STATE_KEY) { mutableStateOf(initialState) } ?: mutableStateOf(initialState)
    override val viewEffects: State<ConsumableEvent<VE>?> = mutableStateOf<ConsumableEvent<VE>?>(null)
    override val navigationEffects: State<ConsumableEvent<NE>?> = mutableStateOf<ConsumableEvent<NE>?>(null)
}
