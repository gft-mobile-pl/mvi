package com.gft.example.mvi.xml.ui.choose

import androidx.lifecycle.SavedStateHandle
import com.gft.mvi.BaseMviViewModel
import kotlin.random.Random

class ChooseViewModel(savedStateHandle: SavedStateHandle) :
    BaseMviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>(
        ChoiceViewState(0), savedStateHandle
    ) {

    override fun onEvent(event: ChoiceViewEvent) {
        when (event) {
            is ChoiceViewEvent.OnShowDetailsClicked -> {
                dispatchNavigationEffect(ChoiceNavigationEffect.NavigateToDetails(event.id))
            }
            ChoiceViewEvent.OnDrawNumberClicked -> {
                viewState = viewState.copy(
                    randomNumber = Random.nextInt(0, 100)
                )
            }
            ChoiceViewEvent.OnShowToastClicked -> {
                dispatchViewEffect(ChoiceViewEffect.ShowToast("Toast message!"))
            }
        }
    }
}
