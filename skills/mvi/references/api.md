# MVI library: public API reference

Signatures as they exist in the library source. Artifacts: `com.gft.mobile:mvi-core`, `com.gft.mobile:mvi-compose`.
Targets: Android, JVM, iosArm64, iosSimulatorArm64.

When updating this reference, check signatures against the library version used by the project.

## Contents
- [Contract markers](#contract-markers-comgftmvi)
- [MviViewModel](#mviviewmodel-comgftmvi)
- [BaseMviViewModel](#basemviviewmodel-comgftmvi)
- [Flow converters](#flow-converters-comgftmvicoroutines)
- [Compose bindings](#compose-bindings-comgftmvi-artifact-mvi-compose)
- [Test / preview helpers](#test--preview-helpers-comgftmvitest)
- [ConsumableEvent](#consumableevent-comgftdata)

---

## Contract markers (`com.gft.mvi`)

```kotlin
interface ViewState {
    companion object {
        val Empty = object : ViewState {}   // placeholder state for VMs that render nothing state-dependent
    }
}
interface ViewEvent
interface NavigationEffect
interface ViewEffect
```

These are pure marker interfaces. If a screen doesn't need one of the categories, use the bare marker as the generic argument.

## `MviViewModel` (`com.gft.mvi`)

```kotlin
@Stable
interface MviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> {
    val viewStates: StateFlow<VS>
    val viewEffects: StateFlow<ConsumableEvent<VE>?>
    val navigationEffects: StateFlow<ConsumableEvent<NE>?>
    fun onEvent(event: EV)

    // Member extensions, usable only inside an implementing class:
    fun MviViewModel<VS, EV, NE, VE>.dispatchNavigationEffect(effect: NE)
    fun MviViewModel<VS, EV, NE, VE>.clearNavigationEffect()
    fun MviViewModel<VS, EV, NE, VE>.dispatchViewEffect(effect: VE)
    fun MviViewModel<VS, EV, NE, VE>.clearViewEffect(effect: VE)   // argument is ignored; sets the effect to null
    var MviViewModel<VS, EV, NE, VE>.viewState: VS                  // get = viewStates.value; set = update
}

// Top-level read-only extension (usable from anywhere, e.g. tests):
inline val <VS : ViewState> MviViewModel<VS, *, *, *>.viewState: VS
```

The setters (`viewState = …`, `dispatch*`, `clear*`) work only when the backing property's runtime object is a
`MutableStateFlow`. Otherwise they throw `UnsupportedOperationException`. They check the runtime type, so a property
declared as `StateFlow<…>` but assigned a `MutableStateFlow` still works.

`clear*` is rarely needed: consumed effects are never re-delivered anyway. Use it only if a not-yet-consumed effect must be
cancelled before the view sees it.

## `BaseMviViewModel` (`com.gft.mvi`)

```kotlin
abstract class BaseMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect>
    : ViewModel(), MviViewModel<VS, EV, NE, VE> {

    constructor(initialState: VS)
    constructor(initialState: VS, savedStateHandle: SavedStateHandle, viewStateSerializer: KSerializer<VS>)
    constructor()  // subclass MUST override viewStates

    override val viewStates: StateFlow<VS>        // lazy MutableStateFlow (restored from SavedStateHandle if available)
    override val viewEffects: StateFlow<ConsumableEvent<VE>?>        // MutableStateFlow(null)
    override val navigationEffects: StateFlow<ConsumableEvent<NE>?>  // MutableStateFlow(null)

    abstract fun onEvent(event: EV)  // from MviViewModel
}
```

- Saved state: on first access to `viewStates` the VM decodes a previously saved state with the serializer and falls back
  to `initialState`. The latest value is encoded whenever the `SavedStateHandle` is saved. Pass the serializer as
  `MyViewState.serializer()` or `serializer<MyViewState>()` (`kotlinx.serialization.serializer`). Only `ViewState` is
  saved and restored automatically; effects and running coroutine jobs are not persisted.
- The no-arg constructor without overriding `viewStates` throws `IllegalArgumentException("You must either override 'val viewStates: StateFlow<VS>' or provide `initialState: VS` through constructor.")` on first access.
- `initialState` doesn't have to be "real" data if the VM fills it synchronously in `init`
  (e.g. `viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { … }`). Compose still requires an initial value.

## Flow converters (`com.gft.mvi.coroutines`)

```kotlin
fun <T : ViewState> Flow<T>.toViewStates(initialState: T, scope: CoroutineScope): StateFlow<T>
fun <T : ViewEffect> Flow<T?>.toViewEffects(scope: CoroutineScope): StateFlow<ConsumableEvent<T>?>
fun <T : NavigationEffect> Flow<T?>.toNavigationEffects(scope: CoroutineScope): StateFlow<ConsumableEvent<T>?>
```

Each wraps the upstream into a flow in the given scope (normally `viewModelScope`). Effect converters wrap every
non-null item in a `ConsumableEvent`.

- **The returned object is a `MutableStateFlow`** (declared as `StateFlow`), so `viewState = …`, `dispatch*` and
  `clear*` keep working on these properties.
- The upstream is collected only while the flow has subscribers (like `SharingStarted.WhileSubscribed()`), and the last
  value is retained between subscriptions.
- A local write is replaced by the next upstream emission, including the emissions that follow a resubscription.
  Use local writes for transient feedback, e.g. `viewState = viewState.copy(isLoading = true)` before calling the
  domain layer, and let the domain-derived value settle the state.

## Compose bindings (`com.gft.mvi`, artifact `mvi-compose`)

```kotlin
@Composable
fun <VS : ViewState, EV : ViewEvent> ViewState(
    viewModel: MviViewModel<VS, EV, *, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    consumer: @Composable ViewStateProvider<VS>.() -> Unit,   // `viewState` available inside
)

@Composable
fun <VS : ViewState> viewState(
    viewModel: MviViewModel<VS, *, *, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
): ReadOnlyProperty<Any?, VS>            // use: val viewState by viewState(viewModel)

@Composable
fun <NE : NavigationEffect> NavigationEffect(
    viewModel: MviViewModel<*, *, NE, *>,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    consumer: (NE) -> Unit,              // plain lambda, invoked from SideEffect
)

@Composable
fun <VE : ViewEffect> ViewEffect(
    viewModel: MviViewModel<*, *, *, VE>,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    consumer: (VE) -> Unit,              // plain lambda, invoked from SideEffect
)

@Stable
class ViewStateProvider<VS : ViewState>(state: State<VS>) {
    inline val viewState: VS
}
```

- Both state readers return a `State`-backed value, so only composables that actually read `viewState` recompose.
- Effects are collected with `collectAsStateWithLifecycle(minActiveState)` and consumed in a `SideEffect`.
- For lifecycle tuning and `minActiveState` examples, read
  [Subscription lifetime](advanced.md#subscription-lifetime-minactivestate).

## Test / preview helpers (`com.gft.mvi.test`)

```kotlin
class TestMviViewModel<VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect>(
    initialState: VS,
) : BaseMviViewModel<VS, EV, NE, VE>(initialState) {
    constructor(initialState: VS, initialViewEffect: VE)   // dispatches initialViewEffect in the constructor
    constructor(initialState: VS, navigationEffect: NE)    // dispatches navigationEffect in the constructor
    override fun onEvent(event: EV) {}                     // ignores all events
}

fun <VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> VS.toViewModel(): TestMviViewModel<VS, EV, NE, VE>
fun <VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> VE.toViewModel(initialState: VS): TestMviViewModel<VS, EV, NE, VE>
fun <VS : ViewState, EV : ViewEvent, NE : NavigationEffect, VE : ViewEffect> NE.toViewModel(initialState: VS): TestMviViewModel<VS, EV, NE, VE>
```

`TestMviViewModel` is a dependency-free fake for previews:
- It holds the given state (without `SavedStateHandle`) and ignores every event, so the rendered state never changes.
- `VS.toViewModel()` creates it with state only. `VE.toViewModel(initialState)` and `NE.toViewModel(initialState)`
  also dispatch that effect in the constructor, so the view handles it as a pending effect on first composition.
- It is a `BaseMviViewModel`, and therefore a `ViewModel`, so it can also be registered in Koin (see the nested
  component preview pattern in [advanced.md](advanced.md#embedding-the-component-a-private-wrapper-for-previews)).

The generic arguments can be inferred when the expected type or explicit type arguments provide enough context.
Typing a screen's VM parameter as `MviViewModel<…>` lets a preview supply `TestMviViewModel` instead of the production
VM class. Inference itself does not require the parameter to use that interface.

## `ConsumableEvent` (`com.gft.data`)

```kotlin
class ConsumableEvent<T>(payload: T) {
    val isConsumed: Boolean
    fun consume(consumer: (T) -> Unit): Boolean            // invokes at most once per instance; false if already consumed
    fun consumeOptionally(handler: (T) -> Boolean): Boolean // handler decides whether it consumed the event
    fun peekPayload(): T                                    // read without consuming
}
```
Consumption uses an internal lock. `consume` marks the instance consumed before invoking the callback, so the
at-most-once guarantee does not imply successful callback completion or delivery of every effect emitted by a VM.
