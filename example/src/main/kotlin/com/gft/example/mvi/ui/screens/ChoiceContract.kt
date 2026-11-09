package com.gft.example.mvi.ui.screens

import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewEvent
import com.gft.mvi.ViewState
import kotlinx.serialization.Serializable

@Serializable
data class ChoiceViewState(
    val randomNumber: Int
) : ViewState

sealed interface ChoiceViewEvent : ViewEvent {
    data class OnShowDetailsClicked(val id: String) : ChoiceViewEvent
    data object OnDrawNumberClicked : ChoiceViewEvent
    data object OnShowToastClicked : ChoiceViewEvent
    data object OnNavigateToCounterClicked : ChoiceViewEvent
}

sealed interface ChoiceViewEffect : ViewEffect {
    data class ShowToast(val message: String) : ChoiceViewEffect
}

sealed interface ChoiceNavigationEffect : NavigationEffect {
    data class NavigateToDetails(val id: String) : ChoiceNavigationEffect
    data object NavigateToCounter : ChoiceNavigationEffect
}
