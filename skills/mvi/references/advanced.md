# MVI: advanced patterns

## Contents
- [Implementing MviViewModel directly](#implementing-mviviewmodel-directly)
- [Injecting view-models with parameters](#injecting-view-models-with-parameters)
- [Subscription lifetime (minActiveState)](#subscription-lifetime-minactivestate)
- [Filtering input while not resumed](#filtering-input-while-not-resumed)
- [Previewing effects](#previewing-effects)
- [Nested components with their own view-models](#nested-components-with-their-own-view-models)

---

## Implementing `MviViewModel` directly

`BaseMviViewModel` covers about 99% of cases. Implement the interface yourself only when you need a different base class
or want every flow to come from the domain layer.

```kotlin
class ChoiceViewModel(
    streamData: StreamUserChoiceDataUseCase,
) : ViewModel(), MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> {

    override val viewStates: StateFlow<ChoiceViewState> = streamData()
        .map { data -> ChoiceViewState(data) }
        .toViewStates(ChoiceViewState(0), viewModelScope)

    override val viewEffects: StateFlow<ConsumableEvent<ChoiceViewEffect>?> =
        MutableStateFlow<ConsumableEvent<ChoiceViewEffect>?>(null)

    override val navigationEffects: StateFlow<ConsumableEvent<ChoiceNavigationEffect>?> =
        MutableStateFlow<ConsumableEvent<ChoiceNavigationEffect>?>(null)

    override fun onEvent(event: ChoiceViewEvent) { /* … dispatchNavigationEffect(...) still works … */ }
}
```

**Always declare the type explicitly as `StateFlow<…>`.** Without it Kotlin infers `MutableStateFlow` and the VM leaks a
mutable flow to the view. A backing field is an alternative:

```kotlin
private val _viewEffects = MutableStateFlow<ConsumableEvent<ChoiceViewEffect>?>(null)
override val viewEffects: StateFlow<ConsumableEvent<ChoiceViewEffect>?> = _viewEffects
```

`dispatchViewEffect`, `dispatchNavigationEffect` and `viewState = …` keep working as long as the runtime object is a
`MutableStateFlow`, even when it is declared as `StateFlow`.

Effects coming from an external stream:

```kotlin
override val navigationEffects = sessionExpiredFlow            // Flow<MyNavigationEffect?>
    .map { MyNavigationEffect.NavigateToLogin }
    .toNavigationEffects(viewModelScope)                       // StateFlow<ConsumableEvent<MyNavigationEffect>?>
```
`toNavigationEffects` also returns a `MutableStateFlow`, so `dispatchNavigationEffect(...)` still works. A local
dispatch and an upstream emission overwrite each other: the latest one wins (the conflation rule applies).

## Injecting view-models with parameters

**Koin**
```kotlin
val appUiModule = module {
    viewModelOf(::ChoiceViewModel)                           // constructor deps + SavedStateHandle resolved by Koin
    viewModel { (id: String) -> DetailsViewModel(id) }       // runtime parameter
}

@Composable
fun DetailsScreen(
    id: String,
    viewModel: MviViewModel<DetailsViewState, ViewEvent, NavigationEffect, ViewEffect> =
        koinViewModel<DetailsViewModel> { parametersOf(id) },
)
```

**Without DI framework**: `androidx.lifecycle.viewmodel.compose.viewModel`
```kotlin
// Factory class (works everywhere on Android)
class DetailsViewModelFactory(private val id: String) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailsViewModel(id) as T
}

@Composable
fun DetailsScreen(
    id: String,
    viewModel: MviViewModel<DetailsViewState, ViewEvent, NavigationEffect, ViewEffect> =
        viewModel(factory = DetailsViewModelFactory(id)),
)

// Or the initializer overload (also multiplatform), with createSavedStateHandle() when needed:
//   viewModel { DetailsViewModel(id) }
//   viewModel { ChoiceViewModel(createSavedStateHandle()) }
```

Use the same mechanism the project already uses for other screens.

## Subscription lifetime (`minActiveState`)

| Binding | Default | Meaning |
|---|---|---|
| `ViewState(viewModel)` / `viewState(viewModel)` | `STARTED` | subscribes to `viewStates` while the view is visible |
| `NavigationEffect(viewModel)` | `RESUMED` | effects are handled only while resumed; the last pending one is handled on resume |
| `ViewEffect(viewModel)` | `RESUMED` | as above |

Change it only for a concrete reason:
- `ViewState(viewModel, minActiveState = Lifecycle.State.CREATED) { … }` keeps a cold upstream flow running while the app is
  in the background. Compose still pauses recomposition while stopped, so no resources are wasted on invisible frames.
- `NavigationEffect(viewModel, minActiveState = Lifecycle.State.STARTED) { … }` handles navigation while visible but paused
  (e.g. behind a dialog activity). Below `STARTED` nothing can be handled in Compose, whatever value you pass.

## Filtering input while not resumed

If the project depends on `com.gft.mobile:compose` (GFT compose utils), wrap screen content in
`com.gft.compose.interaction.InteractionFilter(minActiveState = Lifecycle.State.RESUMED) { … }`. This ignores taps while the
screen is not resumed (e.g. during a transition), which prevents double navigation:

```kotlin
ViewState(viewModel) {
    InteractionFilter(minActiveState = Lifecycle.State.RESUMED) {
        // screen content
    }
}
```
Don't add the dependency just for this without asking the user.

## Previewing effects

```kotlin
// state only
viewModel = ProfileViewState(name = "Jane").toViewModel()

// with a pending view effect (e.g. to preview a snackbar)
viewModel = ProfileViewEffect.ShowMessage("Saved").toViewModel(initialState = ProfileViewState(name = "Jane"))
```

---

## Nested components with their own view-models

> **Most screens have exactly one view-model. Don't use this pattern by default.**
> Apply it only when a part of a screen has to be a self-contained component with its own data source and logic, for
> example a component reused on several screens or shipped from another feature module, and the user wants or the
> codebase already uses such components. If a part only displays data, keep it a stateless composable of the screen
> (see the standard case in [SKILL.md](../SKILL.md#creating-a-screen-one-screen-one-view-model)).

### Files and communication

The default convention gives the component its own contract and view-model, following the screen's 3-file pattern,
but uses **no `Screen` suffix** for the composable file. Follow existing project naming if it differs. For a "UserHeader" component:

| File | Contains |
|---|---|
| `UserHeader.kt` | `UserHeader` composable + its own `@Preview` |
| `UserHeaderContract.kt` | `UserHeaderViewState`, `UserHeaderViewEvent`, `UserHeaderNavigationEffect`, (`UserHeaderViewEffect`) |
| `UserHeaderViewModel.kt` | `UserHeaderViewModel` |

**Callbacks go up, and the view-models stay independent:**
- The component injects its own VM through a default parameter, exactly like a screen.
- The component handles its own `NavigationEffect`s and turns them into lambda callbacks (`onNavigateToAvatar`).
  The parent screen forwards these lambdas to its own navigation callbacks.
- The parent VM and the component VM never reference each other. If they need the same data, both observe it from the
  domain/data layer (shared use case or repository flow).

```kotlin
// ---- feature module "userheader": UserHeaderViewModel.kt ----
// The default VM may be `internal` when the component lives in another Gradle module.
// Other modules never reference it directly.
internal class UserHeaderViewModel(
    observeUser: ObserveCurrentUserUseCase,
) : BaseMviViewModel<UserHeaderViewState, UserHeaderViewEvent, UserHeaderNavigationEffect, ViewEffect>(
    UserHeaderViewState(avatarUrl = null, displayName = "")
) {
    init {
        viewModelScope.launch {
            observeUser().collect { user ->
                viewState = viewState.copy(avatarUrl = user.avatarUrl, displayName = user.name)
            }
        }
    }

    override fun onEvent(event: UserHeaderViewEvent) {
        when (event) {
            is UserHeaderViewEvent.OnAvatarClicked -> dispatchNavigationEffect(UserHeaderNavigationEffect.NavigateToAvatar(event.userId))
        }
    }
}

// ---- feature module "userheader": UserHeader.kt ----
@Composable
fun UserHeader(
    modifier: Modifier = Modifier,
    viewModel: MviViewModel<UserHeaderViewState, UserHeaderViewEvent, UserHeaderNavigationEffect, ViewEffect> = koinViewModel<UserHeaderViewModel>(),
    onNavigateToAvatar: (userId: String) -> Unit,
) {
    NavigationEffect(viewModel) { effect ->
        when (effect) {
            is UserHeaderNavigationEffect.NavigateToAvatar -> onNavigateToAvatar(effect.userId)
        }
    }
    ViewState(viewModel) {
        Row(modifier) { /* uses viewState, calls viewModel.onEvent(...) */ }
    }
}

@Preview
@Composable
private fun UserHeaderPreview() {
    // the component's own preview passes a fake directly, just like a screen preview
    UserHeader(
        viewModel = UserHeaderViewState(avatarUrl = null, displayName = "Jane").toViewModel(),
        onNavigateToAvatar = {},
    )
}
```

### Embedding the component: a private wrapper for previews

If a nested component resolves its production VM during a parent preview, missing Koin definitions or production
dependencies can break the preview. The component's interface-typed VM parameter still accepts a fake even when its
concrete VM is internal to another module.

When the project has no established way to supply nested preview fakes, use a private wrapper with the component's
name, as shown below. This is a preview convention, not a library requirement; preserve an existing working approach.
In this pattern:
- In a preview (`LocalInspectionMode.current == true`), the wrapper fetches the VM from Koin as
  `BaseMviViewModel<…>` with a **qualifier**. `koinViewModel<T>()` needs a `ViewModel` subtype, so `BaseMviViewModel` is used
  instead of `MviViewModel`. The qualifier tells this definition apart from every other `BaseMviViewModel` definition.
- Otherwise it calls the original component with its default view-model.

The parent preview registers the fake under exactly that type and qualifier in `KoinApplicationPreview`.

```kotlin
// ---- feature module "profile": ProfileScreen.kt ----
package com.example.ui.profile

import com.gft.mvi.BaseMviViewModel
import com.gft.mvi.MviViewModel
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewState
import com.gft.mvi.test.toViewModel
// other imports omitted for brevity. Do NOT import com.example.userheader.UserHeader:
// an explicit import would take precedence over the private wrapper below.

private val UserHeaderPreviewQualifier = named("UserHeader")

@Composable
fun ProfileScreen(
    viewModel: MviViewModel<ProfileViewState, ProfileViewEvent, ProfileNavigationEffect, ViewEffect> = koinViewModel<ProfileViewModel>(),
    onNavigateToEdit: () -> Unit,
    onNavigateToAvatar: (userId: String) -> Unit,
) {
    NavigationEffect(viewModel) { effect ->
        when (effect) {
            ProfileNavigationEffect.NavigateToEdit -> onNavigateToEdit()
        }
    }
    ViewState(viewModel) {
        Column {
            UserHeader(onNavigateToAvatar = onNavigateToAvatar) // resolves to the private wrapper below
            Text(text = viewState.name)
            Button(onClick = { viewModel.onEvent(ProfileViewEvent.OnEditClicked) }) { Text("Edit") }
        }
    }
}

@Composable
private fun UserHeader(
    modifier: Modifier = Modifier,
    onNavigateToAvatar: (userId: String) -> Unit,
) {
    if (LocalInspectionMode.current) {
        com.example.userheader.UserHeader(
            modifier = modifier,
            viewModel = koinViewModel<BaseMviViewModel<UserHeaderViewState, UserHeaderViewEvent, UserHeaderNavigationEffect, ViewEffect>>(
                qualifier = UserHeaderPreviewQualifier
            ),
            onNavigateToAvatar = onNavigateToAvatar,
        )
    } else {
        com.example.userheader.UserHeader(modifier = modifier, onNavigateToAvatar = onNavigateToAvatar)
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    KoinApplicationPreview(
        application = {
            modules(module {
                viewModel<BaseMviViewModel<UserHeaderViewState, UserHeaderViewEvent, UserHeaderNavigationEffect, ViewEffect>>(UserHeaderPreviewQualifier) {
                    UserHeaderViewState(avatarUrl = null, displayName = "Jane").toViewModel()
                }
            })
        }
    ) {
        ProfileScreen(
            viewModel = ProfileViewState(name = "Jane").toViewModel(),   // the screen's own VM: passed directly
            onNavigateToEdit = {},
            onNavigateToAvatar = {},
        )
    }
}
```

Details that matter:
- **The type argument of `viewModel<…>(qualifier)` in the preview module must be written out** as the same
  `BaseMviViewModel<…>` that the wrapper requests. Koin finds definitions by declared class + qualifier. Leaving the type
  for inference would register a `TestMviViewModel` and the lookup would miss it. The explicit type also lets `toViewModel()` infer its generics.
- Keep the qualifier in one `private val` per parent file, so the wrapper and the preview can't drift apart.
- Each nested component using this wrapper pattern gets its own wrapper and qualifier. The screen's own VM is still passed
  directly to the screen in the preview.

### Scoping

A nested component's default `koinViewModel()` / `viewModel()` resolves in the nearest `LocalViewModelStoreOwner`,
usually the screen's navigation entry. The component VM therefore lives as long as the screen, not the composable.

- Several instances of the same component on one screen share **one** VM unless each passes a distinct `key`
  (`koinViewModel<UserHeaderViewModel>(key = "header-$userId") { parametersOf(userId) }`).
- Koin applies `parametersOf(...)` only when it creates the instance. If a parameter can change, include it in the
  `key`, or the component keeps the VM created for the old value.
- Every new key leaves another instance in the store until the owner is cleared. Avoid per-item VMs in lazy lists;
  hoist the data to the screen VM instead.
- A new key does not create a new lifetime scope. For a shorter lifetime, provide a different `ViewModelStoreOwner`
  (see the [official ViewModel scoping docs](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis)).

Preserve the project's existing owner and key conventions.
