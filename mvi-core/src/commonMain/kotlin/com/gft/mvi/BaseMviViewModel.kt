package com.gft.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.gft.data.ConsumableEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlin.jvm.JvmInline

private const val VIEW_STATE_KEY = "MviViewMode.viewState"

/**
 * Base implementation of [MviViewModel] built on top of [ViewModel].
 *
 * The view state is exposed through [viewStates] as a [MutableStateFlow] created lazily from the initial state
 * provided through the constructor (or the subclass must override [viewStates]).
 *
 * The view state can be automatically saved and restored (e.g. across process death) via [SavedStateHandle].
 * Because the view state type is erased at runtime, this requires **both** the [SavedStateHandle] **and** the
 * [KSerializer] of the `@Serializable` view state, passed together through the saved-state constructor:
 *
 * ```
 * import kotlinx.serialization.serializer
 *
 * @Serializable
 * data class ChoiceViewState(val choice: Int) : ViewState
 *
 * class ChoiceViewModel(handle: SavedStateHandle) :
 *     BaseMviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>(
 *         ChoiceViewState(0), handle, serializer<ChoiceViewState>()
 *     )
 * ```
 */
abstract class BaseMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> private constructor(
    private val initialState: InitialStateContainer<VS>,
    private val savedStateHandle: SavedStateHandle?,
    private val viewStateSerializer: KSerializer<VS>?,
) : ViewModel(), MviViewModel<VS, EV, NE, VE> {

    /**
     * Creates a view model whose view state starts with [initialState] and is **not** saved/restored.
     *
     * @param initialState Initial view state.
     */
    constructor(
        initialState: VS,
    ) : this(InitialStateContainer(initialState), null, null)

    /**
     * Creates a view model whose view state is automatically saved to and restored from [savedStateHandle].
     *
     * On first access to [viewStates], a previously saved view state is decoded with [viewStateSerializer] and used
     * instead of [initialState]. The latest view state is encoded with [viewStateSerializer] whenever the
     * [savedStateHandle] is saved.
     *
     * @param initialState          Initial view state used when there is no saved view state.
     * @param savedStateHandle      Handle used to save and restore the view state.
     * @param viewStateSerializer   Serializer of the `@Serializable` view state, e.g. `serializer<MyViewState>()`
     *                              or `MyViewState.serializer()`.
     */
    constructor(
        initialState: VS,
        savedStateHandle: SavedStateHandle,
        viewStateSerializer: KSerializer<VS>,
    ) : this(InitialStateContainer(initialState), savedStateHandle, viewStateSerializer)

    /**
     * Creates a view model without an initial state and without saved state support.
     * The subclass must override [viewStates].
     */
    constructor() : this(InitialStateContainer(null), null, null)

    override val viewStates: StateFlow<VS> by lazy {
        if (savedStateHandle != null && viewStateSerializer != null) {
            val restoredState = savedStateHandle
                .get<SavedState>(VIEW_STATE_KEY)
                ?.let {
                    decodeFromSavedState(viewStateSerializer, it)
                }

            MutableStateFlow(restoredState ?: requireInitialState())
                .also { flow ->
                    savedStateHandle.setSavedStateProvider(VIEW_STATE_KEY) {
                        encodeToSavedState(viewStateSerializer, flow.value)
                    }
                }
        } else {
            MutableStateFlow(requireInitialState())
        }
    }

    override val viewEffects: StateFlow<ConsumableEvent<VE>?> = MutableStateFlow(null)
    override val navigationEffects: StateFlow<ConsumableEvent<NE>?> = MutableStateFlow(null)

    private fun requireInitialState() = initialState.value
        ?: throw IllegalArgumentException("You must either override 'val viewStates: StateFlow<VS>' or provide `initialState: VS` through constructor.")

    @JvmInline
    private value class InitialStateContainer<VS>(val value: VS?)
}
