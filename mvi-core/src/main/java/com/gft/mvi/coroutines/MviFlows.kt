package com.gft.mvi.coroutines

import com.gft.data.ConsumableEvent
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

fun <T : ViewState> Flow<T>.toViewStates(initialState: T, scope: CoroutineScope): StateFlow<T> {
    return MutableStateFlowWithSource(
        initialValue = initialState,
        source = this,
        scope = scope
    )
}

fun <T : ViewEffect?> Flow<T>.toViewEffects(scope: CoroutineScope): StateFlow<ConsumableEvent<T>?> = MutableStateFlowWithSource(
    initialValue = null,
    source = map { item ->
        item?.let { ConsumableEvent(item) }
    },
    scope = scope
)

fun <T : NavigationEffect?> Flow<T>.toNavigationEffects(scope: CoroutineScope): StateFlow<ConsumableEvent<T>?> = MutableStateFlowWithSource(
    initialValue = null,
    source = map { item ->
        item?.let { ConsumableEvent(item) }
    },
    scope = scope
)

private class MutableStateFlowWithSource<T>(
    initialValue: T,
    private val source: Flow<T>,
    private val scope: CoroutineScope,
    private val dispatcher: MutableStateFlow<T> = MutableStateFlow(initialValue)
) : MutableStateFlow<T> by dispatcher {
    private var subscription: Job? = null

    private val sharedSource by lazy {
        dispatcher
            .onSubscription {
                subscription = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    source.collect { item -> value = item }
                }
            }
            .onCompletion {
                val subscriptionToCancel = subscription
                subscription = null
                subscriptionToCancel?.cancel()

            }
            .mapNotNull { item -> item }
            .shareIn(scope, SharingStarted.WhileSubscribed())
    }

    override var value: T
        get() {
            // We try to get the latest item from the source - some flows (e.g. StateFlows, buffered flows) are able to provide last item immediately.
            // This is very important during State creation (that is, when `collectAsStateWithLifecycle` is called).
            val latestValue = dispatcher.value
            var newValue = latestValue
            scope
                .launch(start = CoroutineStart.UNDISPATCHED, context = Dispatchers.Unconfined) {
                    runCatching {
                        newValue = source.first()
                    }
                }
                .cancel()
            dispatcher.compareAndSet(latestValue, newValue)

            return dispatcher.value
        }
        set(value) {
            dispatcher.value = value
        }

    override suspend fun collect(collector: FlowCollector<T>): Nothing = sharedSource.collect(collector)

    override val subscriptionCount: StateFlow<Int>
        get() = throw NotImplementedError("subscriptionCount is not supported!")
}
