package com.gft.example.mvi.xml.ui.details

import com.gft.mvi.BaseMviViewModel
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewEvent

class DetailsViewModel(
    id: String
) : BaseMviViewModel<DetailsViewState, ViewEvent, NavigationEffect, ViewEffect>(
    DetailsViewState("#$id")
) {
    override fun onEvent(event: ViewEvent) = Unit
}
