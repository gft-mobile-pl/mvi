---
name: mvi
description: Build, modify, review and debug screens, ViewModels and contracts using the GFT MVI library (com.gft.mobile:mvi-core + mvi-compose) for Kotlin Multiplatform and Android Compose. Always use this skill when creating a full-screen view or a new ViewModel in a project using this library, even if the request only says "add a screen", "new view" or "create a ViewModel" and never mentions MVI.
---

# MVI (GFT `mvi-core` + `mvi-compose`)

This library implements unidirectional MVI on top of Jetpack `ViewModel` and Compose (Multiplatform: Android, JVM, iOS).

- `mvi-core` provides the building blocks. It has marker interfaces for the contract types (`ViewState`, `ViewEvent`,
  `NavigationEffect`, `ViewEffect`), the `MviViewModel` interface, the `BaseMviViewModel` base class, flow converters
  and a fake view-model for previews. The app writes the actual contracts and view-models.
- `mvi-compose` provides the composables that bind a Compose view to a view-model.

Treat both as one toolkit: `mvi-compose` exposes `mvi-core` transitively.

## When to load references

Do not load reference files by default. For ordinary screen or ViewModel creation and edits, use this file and the
project's existing code. Follow a reference link only when the current task needs information missing from those sources.

- Read [references/api.md](references/api.md) when you need to verify an exact signature or use a less common operation,
  such as `clearNavigationEffect` or `consumeOptionally`.
- Read [references/advanced.md](references/advanced.md) only when the task requires additional guidance for one of these cases:
  - Implementing `MviViewModel` directly instead of using `BaseMviViewModel`.
  - Sourcing effect flows externally or configuring parameterized VM injection beyond the examples here.
  - Changing `minActiveState` or configuring `InteractionFilter`.
  - Creating a preview with a pending effect.
  - Implementing or modifying a nested component with its own VM, including its parent preview wiring.
  - Using several instances of the same VM-backed component, VM keys or a custom `ViewModelStoreOwner`.

A new screen, a new ViewModel, a dialog, a navigation callback or a state-only preview does not by itself require
`advanced.md`. Decide whether it is needed before opening it.

## Mental model

The view forwards user interactions to the view-model as `ViewEvent`s through `viewModel.onEvent(event)`.
The view-model receives these events and reacts to them, performing work synchronously or asynchronously as needed.

The view-model exposes the current `ViewState` through `viewStates`, a `StateFlow` that the view observes and renders.
It also exposes `NavigationEffect`s and `ViewEffect`s for the view to handle.

State updates and both kinds of effects can arise from a `ViewEvent`, immediately or after asynchronous work.
They can also arise independently of user interaction, in response to signals from the domain layer, such as observed
data changes or session expiration.

Effects are one-shot. Each is wrapped in a `ConsumableEvent`; consumption happens at most once per wrapper instance.
Delivery of every emitted effect is not guaranteed (see the conflation rule below).

- **ViewState**: everything the view renders, including loading flags, errors and *dialogs*. Immutable `data class`.
- **ViewEvent**: what the user did (`OnSaveClicked`, `OnItemClicked(id)`).
- **NavigationEffect**: a request to leave or change the screen (`NavigateToDetails(id)`, `NavigateBack`).
- **ViewEffect**: rare, fire-and-forget platform actions that cannot be state (toast, snackbar, haptic).

These markers exist so that the compiler catches generic parameters passed in the wrong order, and so the library can
provide extensions such as `toViewModel()` for previews.

## Imports cheat sheet

| What | Import |
|---|---|
| Marker interfaces **and** the composables with the same names | `com.gft.mvi.ViewState`, `com.gft.mvi.ViewEvent`, `com.gft.mvi.NavigationEffect`, `com.gft.mvi.ViewEffect` |
| View-model types | `com.gft.mvi.MviViewModel`, `com.gft.mvi.BaseMviViewModel` |
| Property-delegate variant of state reading | `com.gft.mvi.viewState` |
| Flow → MVI StateFlow converters | `com.gft.mvi.coroutines.toViewStates` / `toViewEffects` / `toNavigationEffects` |
| Preview fake view-model | `com.gft.mvi.test.toViewModel` (`TestMviViewModel`) |
| One-shot wrapper used by effects | `com.gft.data.ConsumableEvent` |
| Lifecycle-aware back handler (requires `com.gft.mobile:compose`) | `com.gft.compose.interaction.LifecycleAwareBackHandler` |

`import com.gft.mvi.ViewState` brings in both the interface and the `ViewState(...)` composable. Kotlin imports every
top-level declaration with that name, so you don't need a separate import.
`viewState = …`, `dispatchNavigationEffect(…)` and `dispatchViewEffect(…)` are member extensions of `MviViewModel`.
They are available inside the view-model class without imports.

The code examples below show only `com.gft.mvi` imports and leave the rest out for brevity. For everything else (Koin,
`@Preview`, Material, navigation), use the imports the project already uses.

## Dependencies

The library is published as two artifacts (use the version from the project's version catalog):
- `com.gft.mobile:mvi-core`: contract markers, `MviViewModel`, `BaseMviViewModel`, flow converters, preview fakes.
- `com.gft.mobile:mvi-compose`: Compose bindings (`ViewState`, `viewState`, `NavigationEffect`, `ViewEffect` composables).

In practice you add only one of them:
- A module that uses Compose needs only `mvi-compose`, because it exposes `mvi-core` transitively.
- A module without Compose (e.g. a module containing only view-models) needs only `mvi-core`.

```kotlin
implementation("com.gft.mobile:mvi-compose:<version>") // Compose modules (mvi-core comes with it)
// or
implementation("com.gft.mobile:mvi-core:<version>")    // modules without Compose
```

Saving state with `@Serializable` also requires the `org.jetbrains.kotlin.plugin.serialization` Gradle plugin.

`LifecycleAwareBackHandler` and `InteractionFilter` come from a separate library, `com.gft.mobile:compose`
(GFT compose utils). `mvi-compose` uses it internally but does not expose it, so a module that calls these composables
must declare `com.gft.mobile:compose` itself.

---

## Creating a screen: one screen, one view-model

The library requires the contract marker types, the `MviViewModel` interface and compatible flows. File layout,
DI framework and preview wiring are project conventions, not API requirements. Preserve established conventions;
use the following pattern when the project has none.

For a new screen, the default is three files in one feature package (e.g. `ui/profile/`):

| File | Contains |
|---|---|
| `ProfileContract.kt` | `ProfileViewState`, `ProfileViewEvent`, `ProfileNavigationEffect`, `ProfileViewEffect`: the full communication protocol between the screen and its view-model |
| `ProfileViewModel.kt` | `ProfileViewModel`, which reacts to view events and domain signals, and exposes state updates and effects |
| `ProfileScreen.kt` | `ProfileScreen` composable (view-model injected as a default parameter) + `@Preview` |

Then register the view-model in DI and wire the navigation callbacks where the screen is placed in the nav graph.

Before writing code, look at an existing screen in the project, if there is one, and match its conventions:
DI framework, package layout, naming, Preview style. Preserve the project's existing VM creation and lifetime scoping
conventions. Consistency with the codebase is more important than the templates below.

Parts of the screen that only display data are ordinary stateless composables (values and lambdas in, no view-model).
Don't give them their own view-model. For self-contained components with their own data and logic, see
[Nested components](#nested-components-with-their-own-view-models).

### Step 1: Contract (`ProfileContract.kt`)

```kotlin
package com.example.ui.profile

import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewEvent
import com.gft.mvi.ViewState
// other imports omitted for brevity

@Serializable // only needed when the view-model saves state in SavedStateHandle
data class ProfileViewState(
    val name: String = "",
    val isDeleteDialogVisible: Boolean = false, // dialogs are STATE, not effects
) : ViewState

sealed interface ProfileViewEvent : ViewEvent {
    data object OnEditClicked : ProfileViewEvent
    data class OnFriendClicked(val friendId: String) : ProfileViewEvent
    data object OnDeleteClicked : ProfileViewEvent
    data object OnDeleteConfirmed : ProfileViewEvent
    data object OnDeleteDialogDismissed : ProfileViewEvent
    data object OnBackClicked : ProfileViewEvent
}

sealed interface ProfileNavigationEffect : NavigationEffect {
    data object NavigateToEdit : ProfileNavigationEffect
    data class NavigateToFriend(val friendId: String) : ProfileNavigationEffect
    data object NavigateBack : ProfileNavigationEffect
}

sealed interface ProfileViewEffect : ViewEffect {
    data class ShowMessage(val message: String) : ProfileViewEffect
}
```

Contract rules:
- Name events after **what the user did** (`OnXxxClicked`, `OnXxxChanged(value)`), and effects after **what should happen** (`NavigateToXxx`, `ShowXxx`).
- Use `data object` for events and effects without parameters, and `data class` for ones with parameters.
- **Leave out the parts you don't need.** Don't declare empty sealed interfaces. In the generics, use the bare
  marker interface instead (`ViewEvent`, `NavigationEffect`, `ViewEffect`), e.g.
  `BaseMviViewModel<DetailsViewState, ViewEvent, NavigationEffect, ViewEffect>`.
- `@Serializable` is needed only when the state is persisted via `SavedStateHandle`. Every property must then be serializable.
  The automatic mechanism saves and restores only `ViewState`, not effects or running coroutine jobs.

### Step 2: View-model (`ProfileViewModel.kt`)

Inherit from `BaseMviViewModel<VS, EV, NE, VE>` (generic order: **State, Event, Navigation, Effect**). You need to implement `onEvent`.

```kotlin
package com.example.ui.profile

import com.gft.mvi.BaseMviViewModel
// other imports omitted for brevity

class ProfileViewModel(
    savedStateHandle: SavedStateHandle,
) : BaseMviViewModel<ProfileViewState, ProfileViewEvent, ProfileNavigationEffect, ProfileViewEffect>(
    ProfileViewState(), savedStateHandle, ProfileViewState.serializer()
) {
    override fun onEvent(event: ProfileViewEvent) {
        when (event) {
            OnEditClicked -> dispatchNavigationEffect(NavigateToEdit)
            is OnFriendClicked -> dispatchNavigationEffect(NavigateToFriend(event.friendId))
            OnDeleteClicked -> viewState = viewState.copy(isDeleteDialogVisible = true)
            OnDeleteDialogDismissed -> viewState = viewState.copy(isDeleteDialogVisible = false)
            OnDeleteConfirmed -> {
                viewState = viewState.copy(isDeleteDialogVisible = false)
                dispatchNavigationEffect(NavigateBack)
            }
            OnBackClicked -> dispatchNavigationEffect(NavigateBack)
        }
    }
}
```

- `viewState` reads the current state, and `viewState = viewState.copy(...)` updates it.
- `dispatchNavigationEffect(...)` / `dispatchViewEffect(...)` emit one-shot effects.
- Long-running work runs in `viewModelScope.launch { ... }`, and the result goes into state via `viewState = viewState.copy(...)`.
- Use `when` exhaustively over the sealed event type, so adding an event causes a compile error until it is handled.

**Choose the constructor:**

| Situation | Constructor |
|---|---|
| State managed in the VM, no persistence | `BaseMviViewModel<…>(initialState)` |
| State managed in the VM, survives process death | `BaseMviViewModel<…>(initialState, savedStateHandle, MyViewState.serializer())` (state must be `@Serializable`) |
| State controlled by the domain layer, potentially composed from multiple sources | `BaseMviViewModel<…>()` and override `val viewStates: StateFlow<VS>` with the adapted state |

Use the no-arg constructor when the domain layer controls the state and the VM adapts it for the view. The state may
be composed from multiple sources; those sources do not have to be `Flow`s. The VM must expose the resulting state as
`viewStates: StateFlow<VS>`. Send state-changing requests to the domain layer and reflect the resulting domain state.

With this constructor you must override `viewStates`; otherwise the first access throws `IllegalArgumentException`.
Automatic state saving and restoration are not provided in this mode.

**`toViewStates(...)` returns a `MutableStateFlow`** (declared as `StateFlow`), so `viewState = viewState.copy(...)`
still works on top of a domain-driven state. Such a local update is temporary: the next value from the domain source
replaces it. This also happens when the view resubscribes, because the source is collected only while the view observes
`viewStates`. This is useful for immediate UI feedback before the domain layer responds, e.g. setting
`viewState = viewState.copy(isLoading = true)` right before calling a use case. The final value must come from the
domain-derived state, so make sure the domain source emits after the request; otherwise the local value stays.

The following example adapts a single domain `Flow`; it is one possible implementation:

```kotlin
class CounterViewModel(
    observeCounter: ObserveCounterUseCase,
    private val resetCounter: ResetCounterUseCase,
) : BaseMviViewModel<CounterViewState, CounterViewEvent, CounterNavigationEffect, ViewEffect>() {

    override val viewStates: StateFlow<CounterViewState> = observeCounter()
        .map { count -> CounterViewState(count = count, isLoading = false) }
        .toViewStates(initialState = CounterViewState(count = 0), scope = viewModelScope)

    override fun onEvent(event: CounterViewEvent) {
        when (event) {
            CounterViewEvent.OnBackClicked -> dispatchNavigationEffect(CounterNavigationEffect.NavigateBack)
            CounterViewEvent.OnResetClicked -> {
                viewState = viewState.copy(isLoading = true) // immediate feedback, replaced by the next domain emission
                viewModelScope.launch { resetCounter() }
            }
        }
    }
}
```

### Step 3: Screen (`ProfileScreen.kt`)

```kotlin
package com.example.ui.profile

import com.gft.mvi.MviViewModel
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewState
import com.gft.mvi.test.toViewModel
import com.gft.compose.interaction.LifecycleAwareBackHandler // requires com.gft.mobile:compose
// other imports omitted for brevity

@Composable
fun ProfileScreen(
    viewModel: MviViewModel<ProfileViewState, ProfileViewEvent, ProfileNavigationEffect, ProfileViewEffect> = koinViewModel<ProfileViewModel>(),
    onNavigateToEdit: () -> Unit,
    onNavigateToFriend: (friendId: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LifecycleAwareBackHandler {
        viewModel.onEvent(ProfileViewEvent.OnBackClicked)
    }

    NavigationEffect(viewModel) { effect ->
        when (effect) {
            ProfileNavigationEffect.NavigateToEdit -> onNavigateToEdit()
            is ProfileNavigationEffect.NavigateToFriend -> onNavigateToFriend(effect.friendId)
            ProfileNavigationEffect.NavigateBack -> onNavigateBack()
        }
    }

    ViewEffect(viewModel) { effect ->
        when (effect) {
            is ProfileViewEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
        }
    }

    ViewState(viewModel) {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // read `viewState` here; dispatch user actions with viewModel.onEvent(...)
                Text(text = viewState.name)
                Button(onClick = { viewModel.onEvent(ProfileViewEvent.OnEditClicked) }) { Text("Edit") }

                if (viewState.isDeleteDialogVisible) {
                    AlertDialog(
                        onDismissRequest = { viewModel.onEvent(ProfileViewEvent.OnDeleteDialogDismissed) },
                        confirmButton = { TextButton(onClick = { viewModel.onEvent(ProfileViewEvent.OnDeleteConfirmed) }) { Text("Delete") } },
                        text = { Text("Delete profile?") },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    ProfileScreen(
        viewModel = ProfileViewState(name = "Jane Doe").toViewModel(),
        onNavigateToEdit = {},
        onNavigateToFriend = {},
        onNavigateBack = {},
    )
}
```

Screen rules and the reasons behind them:
- **Type the `viewModel` parameter as the `MviViewModel<…>` interface, not the concrete class.** That is what lets
  previews pass `MyViewState(...).toViewModel()`, a dependency-free fake, instead of building a VM with all its use cases.
- **Inject with a default parameter value** (`= koinViewModel<ProfileViewModel>()`, or `= viewModel<ProfileViewModel>()` without Koin).
  Screen arguments such as an `id` come first and are passed to the VM factory (see [references/advanced.md](references/advanced.md#injecting-view-models-with-parameters)).
- **Navigation goes out through lambdas** (`onNavigateToXxx`). The screen and VM never see a `NavController`. The nav graph decides what "edit" means.
- **Read state with `ViewState(viewModel) { … }`** (the default style). Inside the lambda, `viewState` is backed by
  Compose `State`, so only composables that read it recompose. The alternative `val viewState by viewState(viewModel)` is
  equivalent and saves one indentation level. Use it where the surrounding code already does.
- Effect handlers are regular callbacks. Use a coroutine for suspend work, as in the snackbar example above.
- Only call `NavigationEffect(...)` / `ViewEffect(...)` if the contract defines that type.
- For system back handling that needs VM logic, prefer `com.gft.compose.interaction.LifecycleAwareBackHandler`.
  It consumes back events but calls `onBack` only while the screen is at least `RESUMED`, which prevents duplicate
  navigation during transitions. Forward the action to `viewModel.onEvent(OnBackClicked)`; the VM can dispatch
  `NavigateBack`. It requires the `com.gft.mobile:compose` dependency (see [Dependencies](#dependencies)). If the
  module doesn't have it, ask the user before adding it or use the back handler the project already uses.

### Step 4: DI and navigation

```kotlin
val profileModule = module {
    viewModelOf(::ProfileViewModel)                                   // SavedStateHandle is supplied by Koin
    viewModel { (userId: String) -> UserDetailsViewModel(userId) }    // VM with a runtime parameter
}

composable("profile") {
    ProfileScreen(
        onNavigateToEdit = { navController.navigate("profile/edit") },
        onNavigateToFriend = { id -> navController.navigate("user/$id") },
        onNavigateBack = { navController.popBackStack() },
    )
}
```
Add the module where the project already declares its Koin modules, and use the project's navigation style (typed routes, Navigation 3, etc.).

---

## Rules that prevent the common bugs

1. **Prefer state over view effects.** Dialogs, errors, loading, selection and bottom sheets all belong in `ViewState`,
   because state survives recomposition, rotation and (optionally) process death, while effects are one-shot. Reach for a
   `ViewEffect` only for truly transient platform actions (toast, snackbar, haptic, share sheet).
2. **Effects are conflated, so never emit a sequence of them.** Effects are stored in a `StateFlow<ConsumableEvent<T>?>`
   that keeps only the latest value. If the VM dispatches two effects before the view consumes the first, the first is lost.
   The same happens while the view is not `RESUMED` (the default `minActiveState` for effects): only the last effect is
   handled after resuming. Combine them into one effect or model the situation in state.
3. **Consumption is at most once per `ConsumableEvent` instance.** A consumed wrapper isn't handled again after
   recomposition or configuration change. This does not guarantee delivery of every emitted effect.
   Don't add your own "consumed" flags.
4. **Keep the generic order `<VS, EV, NE, VE>`** everywhere: `BaseMviViewModel`, `MviViewModel` parameter types and `TestMviViewModel`.
5. **Don't expose mutable flows.** When implementing `MviViewModel` directly, type every overridden property explicitly as
   `StateFlow<…>` (see [references/advanced.md](references/advanced.md#implementing-mviviewmodel-directly)).
6. **Keep state ownership clear.** When the VM owns the state, update it via `viewState = …`. When the domain layer
   controls the state, adapt its sources into `viewStates` and send state-changing requests to the domain layer.
   Composing multiple sources does not transfer ownership of domain state to the VM. A local `viewState = …` on top of
   `toViewStates` is fine for transient feedback (e.g. a loading flag), but the next domain emission replaces it.
7. **The view only renders and forwards events.** No business logic, no repository calls, no navigation decisions in the composable.

## Modifying existing MVI code

- Read the `*Contract.kt` file first, because it is the screen's full API. A new user interaction means: add a `ViewEvent` →
  handle it in `onEvent` → (new state field / new effect) → render or handle it in the view → update the Preview if
  its state constructor changed.

For reviews and implementation verification, use the final checklist below.

---

## Nested components with their own view-models

Use a separate VM only for a self-contained component with its own data source and logic when the user requests it
or the codebase already uses that pattern. Keep display-only components stateless. Before implementing nested VMs,
read [references/advanced.md](references/advanced.md#nested-components-with-their-own-view-models) for communication and preview wiring.

---

## Final checklist

Apply the items relevant to the requested change. Use this to verify the result; do not reproduce the checklist in every reply.

- [ ] The contract, exhaustive `onEvent` handling and UI cover the added interactions.
- [ ] Persistent UI state, including dialog visibility, is represented in `ViewState`.
- [ ] The code does not rely on lossless delivery of a sequence of effects.
- [ ] State updates respect ownership: VM-owned state is updated locally; domain-controlled state changes through the domain layer.
- [ ] The screen accepts the `MviViewModel` interface and its preview receives a fake VM; nested components avoid
  resolving production dependencies in previews.
- [ ] DI registration and navigation callbacks are connected where the change requires them, following project conventions.
- [ ] Appropriate compilation or behavior verification was performed, and any verification limits are reported.
