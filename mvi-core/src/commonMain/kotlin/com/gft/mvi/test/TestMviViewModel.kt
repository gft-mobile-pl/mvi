package com.gft.mvi.test

import com.gft.mvi.BaseMviViewModel
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewEvent
import com.gft.mvi.ViewState

class TestMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect>(
    initialState: VS
) : BaseMviViewModel<VS, EV, NE, VE>(initialState) {

    constructor(initialState: VS, initialViewEffect: VE) : this(initialState) {
        dispatchViewEffect(initialViewEffect)
    }

    constructor(initialState: VS, navigationEffect: NE) : this(initialState) {
        dispatchNavigationEffect(navigationEffect)
    }

    override fun onEvent(event: EV) {}
}

fun <VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> VS.toViewModel() =
    TestMviViewModel<VS, EV, NE, VE>(this)

fun <VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> VE.toViewModel(initialState: VS) =
    TestMviViewModel<VS, EV, NE, VE>(initialState, this)

fun <VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> NE.toViewModel(initialState: VS) =
    TestMviViewModel<VS, EV, NE, VE>(initialState, this)
