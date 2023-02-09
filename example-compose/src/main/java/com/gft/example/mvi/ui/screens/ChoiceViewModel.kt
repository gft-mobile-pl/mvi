package com.gft.example.mvi.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.gft.example.mvi.ui.screens.ChoiceNavigationEffect.NavigateToDetails
import com.gft.example.mvi.ui.screens.ChoiceViewEvent.OnDrawNumberClicked
import com.gft.example.mvi.ui.screens.ChoiceViewEvent.OnShowDetailsClicked
import com.gft.example.mvi.ui.screens.ChoiceViewEvent.OnShowToastClicked
import com.gft.mvi.BaseMviViewModel
import kotlin.random.Random

class ChoiceViewModel(
    savedStateHandle: SavedStateHandle,
) : BaseMviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>(
    ChoiceViewState(0), savedStateHandle
) {
    override fun onEvent(event: ChoiceViewEvent) {

        when (event) {
            is OnShowDetailsClicked -> {
                dispatchNavigationEffect(NavigateToDetails(event.id))
            }
            OnDrawNumberClicked -> {
                viewState = viewState.copy(
                    randomNumber = Random.nextInt(0, 100)
                )
            }
            OnShowToastClicked -> {
                dispatchViewEffect(ChoiceViewEffect.ShowToast("Toast message!"))
            }
        }
    }
}