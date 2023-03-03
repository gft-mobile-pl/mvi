package com.gft.mvi.fragment

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gft.mvi.MviViewModel
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewEvent
import com.gft.mvi.ViewState
import kotlinx.coroutines.launch

fun <VS : ViewState, NE : NavigationEffect, VE : ViewEffect> Fragment.observeViewModel(
    viewModel: MviViewModel<VS, *, NE, VE>,
    onViewState: ((VS) -> Unit)? = null,
    onNavigationEffect: ((NE) -> Unit)? = null,
    onViewEffect: ((VE) -> Unit)? = null
) {
    if (onViewState != null) observeViewState(viewModel, onViewState)
    if (onViewEffect != null) handleViewEffect(viewModel, onViewEffect)
    if (onNavigationEffect != null) handleNavigationEffect(viewModel, onNavigationEffect)
}

fun <NE : NavigationEffect> Fragment.handleNavigationEffect(
    viewModel: MviViewModel<*, *, NE, *>,
    consumer: (NE) -> Unit
) = handleNavigationEffect(viewModel, Lifecycle.State.STARTED, consumer)

fun <NE : NavigationEffect> Fragment.handleNavigationEffect(
    viewModel: MviViewModel<*, *, NE, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    consumer: (NE) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(minActiveState) {
            viewModel.navigationEffects.collect { event ->
                event?.consume { effect ->
                    consumer(effect)
                }
            }
        }
    }
}

fun <VE : ViewEffect> Fragment.handleViewEffect(
    viewModel: MviViewModel<*, *, *, VE>,
    consumer: (VE) -> Unit
) = handleViewEffect(viewModel, Lifecycle.State.STARTED, consumer)

fun <VE : ViewEffect> Fragment.handleViewEffect(
    viewModel: MviViewModel<*, *, *, VE>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    consumer: (VE) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(minActiveState) {
            viewModel.viewEffects.collect { event ->
                event?.consume { effect ->
                    consumer(effect)
                }
            }
        }
    }
}

fun <VS : ViewState> Fragment.observeViewState(
    viewModel: MviViewModel<VS, *, *, *>,
    consumer: (VS) -> Unit
) = observeViewState(viewModel, Lifecycle.State.STARTED, consumer)

fun <VS : ViewState> Fragment.observeViewState(
    viewModel: MviViewModel<VS, *, *, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    consumer: (VS) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(minActiveState) {
            viewModel.viewStates.collect { state ->
                consumer(state)
            }
        }
    }
}
