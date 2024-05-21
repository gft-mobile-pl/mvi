package com.gft.example.mvi.ui.counter

import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEvent
import com.gft.mvi.ViewState

data class CounterViewState(
    val count: Int
) : ViewState

sealed interface CounterViewEvent : ViewEvent {
    data object OnBackClicked : CounterViewEvent
}

sealed interface CounterNavigationEffect : NavigationEffect {
    data object NavigateBack : CounterNavigationEffect
}
