package com.gft.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gft.data.ConsumableEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline

private const val VIEW_STATE_KEY = "MviViewMode.viewState"

abstract class BaseMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> private constructor(
    private val initialState: InitialStateContainer<VS>,
    private val savedStateHandle: SavedStateHandle?,
) : ViewModel(), MviViewModel<VS, EV, NE, VE> {

    constructor(
        initialState: VS,
        savedStateHandle: SavedStateHandle? = null,
    ) : this(InitialStateContainer(initialState), savedStateHandle)

    constructor() : this(InitialStateContainer(null), null)

    override val viewStates: StateFlow<VS> by lazy {
        if (savedStateHandle != null) {
            MutableStateFlow(savedStateHandle.get<VS>(VIEW_STATE_KEY) ?: requireInitialState())
                .apply {
                    viewModelScope.launch {
                        collectLatest { newValue -> savedStateHandle[VIEW_STATE_KEY] = newValue }
                    }
                }
        } else {
            MutableStateFlow(requireInitialState())
        }
    }

    override val viewEffects: StateFlow<ConsumableEvent<VE>?> = MutableStateFlow<ConsumableEvent<VE>?>(null)
    override val navigationEffects: StateFlow<ConsumableEvent<NE>?> = MutableStateFlow<ConsumableEvent<NE>?>(null)

    private fun requireInitialState() = initialState.value
        ?: throw IllegalArgumentException("You must either override 'val viewStates: StateFlow<VS>' or provide `initialState: VS` through constructor.")

    @JvmInline
    private value class InitialStateContainer<VS>(val value: VS?)
}
