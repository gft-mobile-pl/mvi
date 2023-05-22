package com.gft.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gft.data.ConsumableEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Optional

private const val VIEW_STATE_KEY = "MviViewMode.viewState"

abstract class BaseMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> private constructor(
    private val initialState: Optional<VS>,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel(), MviViewModel<VS, EV, NE, VE> {

    constructor(
        initialState: VS,
        savedStateHandle: SavedStateHandle? = null
    ) : this(Optional.of(initialState), savedStateHandle)

    constructor() : this(Optional.empty(), null)

    override val viewStates: StateFlow<VS> by lazy {
        when {
            savedStateHandle != null -> savedStateHandle.getLiveData<VS>(VIEW_STATE_KEY).let { liveData ->
                MutableStateFlow(liveData.value ?: initialState.get()).apply {
                    viewModelScope.launch {
                        collectLatest { newValue -> liveData.value = newValue }
                    }
                }
            }
            initialState.isPresent -> MutableStateFlow(initialState.get())
            else -> throw IllegalArgumentException("You must either override 'val viewStates: StateFlow<VS>' or provide `initialState: VS` through constructor.")
        }
    }
    override val viewEffects: StateFlow<ConsumableEvent<VE>?> = MutableStateFlow<ConsumableEvent<VE>?>(null)
    override val navigationEffects: StateFlow<ConsumableEvent<NE>?> = MutableStateFlow<ConsumableEvent<NE>?>(null)
}
