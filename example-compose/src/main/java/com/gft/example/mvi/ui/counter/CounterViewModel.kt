package com.gft.example.mvi.ui.counter

import androidx.lifecycle.viewModelScope
import com.gft.example.mvi.ui.counter.CounterNavigationEffect.NavigateBack
import com.gft.example.mvi.ui.counter.CounterViewEvent.OnBackClicked
import com.gft.mvi.BaseMviViewModel
import com.gft.mvi.ViewEffect
import com.gft.mvi.coroutines.toViewStates
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CounterViewModel : BaseMviViewModel<CounterViewState, CounterViewEvent, CounterNavigationEffect, ViewEffect>() {

    override val viewStates: StateFlow<CounterViewState> = flow {
        var value = 0
        while (true) {
            emit(value)
            value++
            delay(1000)
        }
    }
        .map { count ->
            CounterViewState(
                count = count
            )
        }
        .toViewStates(
            initialState = CounterViewState(-1),
            viewModelScope
        )

    override fun onEvent(event: CounterViewEvent) {
        when (event) {
            OnBackClicked -> {
                dispatchNavigationEffect(NavigateBack)
            }
        }
    }
}
