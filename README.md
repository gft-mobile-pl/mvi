# MVI ViewModel

* [TL;DR](#tldr)
* [Usage](#usage)
  * [Define contract between view and view-model](#define-contract-between-view-and-view-model)
  * [Define view-model](#define-view-model)
    * [Using `BaseMviViewModel`](#using-basemviviewmodel)
    * [Using `MviViewModel` interface](#using-mviviewmodel-interface)
    * [Updating view state](#updating-view-state)
    * [Dispatching view effects](#dispatching-view-effects)
    * [Dispatching navigation effects](#dispatching-navigation-effects)
  * [Use view-model (Compose)](#use-view-model-compose)
    * [Injecting view-model](#injecting-view-model)
    * [Injecting view-model with Koin](#injecting-view-model-with-koin)
    * [Handling view state changes.](#handling-view-state-changes)
    * [Dispatching view events](#dispatching-view-events)
    * [View state subscription lifetime](#view-state-subscription-lifetime)
    * [Handling navigation effects](#handling-navigation-effects)
    * [Handling view effects](#handling-view-effects)

## TL;DR
1. Define contract:
```kotlin
@Serializable
data class ChoiceViewState(
    val randomNumber: Int
) : ViewState

sealed interface ChoiceViewEvent : ViewEvent {
    data class OnShowDetailsClicked(val id: String) : ChoiceViewEvent
    object OnDrawNumberClicked : ChoiceViewEvent
    object OnShowToastClicked : ChoiceViewEvent
}

sealed interface ChoiceNavigationEffect : NavigationEffect {
    data class NavigateToDetails(val id: String) : ChoiceNavigationEffect
}

sealed interface ChoiceViewEffect : ViewEffect {
    data class ShowToast(val message: String) : ChoiceViewEffect
}
```
2. Implement view-model:
```kotlin
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
```
3. Bind view to view-model
```kotlin
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = koinViewModel<ChoiceViewModel>(),
    onNavigateToDetails: (String) -> Unit
) {

    NavigationEffect(viewModel) { effect ->
        when (effect) {
            is NavigateToDetails -> {
                onNavigateToDetails(effect.id)
            }
        }
    }

    ViewEffect(viewModel) { effect ->
        when (effect) {
            is ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }

    // Option A
    ViewState(viewModel) {
        Text(
            text = "${viewState.randomNumber}",
            style = TextStyle.Default.copy(
                fontSize = 72.sp
            )
        )
    }
    
    // Option B (less indents)
    val viewState by viewState(viewModel)
    Text(
        text = "${viewState.randomNumber}",
        style = TextStyle.Default.copy(
            fontSize = 72.sp
        )
    )
}
```
4. Provide `@Preview`
```kotlin
@Preview(showSystemUi = true, heightDp = 800)
@Composable
fun ChoiceScreenPreview() {
    ChoiceScreen(
        viewModel = ChoiceViewState(randomNumber = 16).toViewModel(), // uses TestMviViewModel under the hood
        onNavigateToDetails = {}
    )
}
```

## Usage

### Define contract between view and view-model
- Your view state class has to implemented `ViewState` marker interface.
- View events have to implemented `ViewEvent` marker interface.
- Navigation effects have to implemented `NavigationEffect` marker interface.
- View effects have to implemented `ViewEffect` marker interface.
```kotlin
@Serializable
data class ChoiceViewState(
    val randomNumber: Int
) : ViewState, Parcelable

sealed interface ChoiceViewEvent : ViewEvent {
    data class OnShowDetailsClicked(val id: String) : ChoiceViewEvent
    object OnDrawNumberClicked : ChoiceViewEvent
    object OnShowToastClicked : ChoiceViewEvent
}

sealed interface ChoiceNavigationEffect : NavigationEffect {
    data class NavigateToDetails(val id: String) : ChoiceNavigationEffect
}

sealed interface ChoiceViewEffect : ViewEffect {
    data class ShowToast(val message: String) : ChoiceViewEffect
}
```
> ℹ Your view state doesn't have to be annotated with @Serializable if you don't intend to store the view state in `SavedStateHandle`.

> ℹ You don't have to define empty classes, e.g. if you don't need `view effect` simply don't define it. 

> ℹ `ViewState`, `ViewEvent`, `NavigationEffect`, `ViewEffect` are just marker interfaces. 
> Theoretically we would be just fine without them but:
> - They open possibility to create extensions methods which are scoped to the particular parts of MVI contract:
>   - Absolutely life-saver when dealing with `@Preview`
>     ```kotlin
>     @Preview(showSystemUi = true, heightDp = 800)
>     @Composable
>     fun ChoiceScreenPreview() {
>       ChoiceScreen(
>         viewModel = ChoiceViewState(randomNumber = 16).toViewModel(), // uses TestMviViewModel under the hood
>         onNavigateToDetails = {}
>       )
>     }
>     ```
>   - Very useful when creating instrumented tests when view-model is required by Fragment/Composable.
> - They prevent various common mistakes like when the generic parameters are specified in a wrong order while defining view-model.


### Define view-model

#### Using `BaseMviViewModel`

The only requirement is to implement `MviViewModel` interface, but 99% of the time it would be easier to inherit from `BaseMviViewModel`:

```kotlin
class ChoiceViewModel(
    savedStateHandle: SavedStateHandle,
) : BaseMviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>(
    ChoiceViewState(0), savedStateHandle
) {
    override fun onEvent(event: ChoiceViewEvent) {

    }
}

// or

class ChoiceViewModel : BaseMviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>() {

    override val viewStates = streamData()
        .map { data -> ChoiceViewState(data) }
        .toViewStates(ChoiceViewState(0), viewModelScope)
    
    override fun onEvent(event: ChoiceViewEvent) {

    }
}
```
There are two constructors available:
- With two parameters:
  - `initialViewState` - in the MVI (and Compose) the initial state of the view is always required.<br />
ℹ initial data may not be a "correct" if you plan to provide the real data synchronously 
in the `init` block (e.g. using `launch(start = CoroutineStart.UNDISPATCHED) {}`) 
  - `savedStateHandle` (optional) - if you pass `SavedStateHandle` to the `BaseMviViewModel` it will automatically save/restore the view state when activity is killed/recreated.
- With no parameters:
  - If you use this constructor you must override `val viewStates: StateFlow` property.
  If you don't do this an exception will be thrown on runtime when the `viewStates` is accessed for the first time.
  - if you choose this constructor you can't benefit from the automatic save/restore mechanism anymore,
  but if your view state is mapped from an external stream it probably means that you do not need this feature in the first place.

#### Using `MviViewModel` interface

Extending `BaseMviViewModel` is not mandatory - it is enough to implement the `MviViewModel`. This is very useful if you don't want to manage the ViewState "manually" inside the view-model, 
but rather fetch it from domain.

```kotlin
class ChoiceViewModel(streamData: StreamUserChoiceDataUseCase) : MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>, ViewModel() {
    
    override val viewStates: StateFlow<ChoiceViewState> = streamData()
        .map { data -> ChoiceViewState(data) }
        .toViewStates(ChoiceViewState(0), viewModelScope)
    
    override val viewEffects: StateFlow<ConsumableEvent<ChoiceViewEffect>?> = MutableStateFlow<ConsumableEvent<ChoiceViewEffect>?>(null)
    override val navigationEffects: StateFlow<ConsumableEvent<ChoiceNavigationEffect>?> = MutableStateFlow<ConsumableEvent<ChoiceNavigationEffect>?>(null)
    override fun onEvent(event: ChoiceViewEvent) { }
}
```
> 💡 There are 3 extension methods provided that can convert external flows to corresponding `StateFlow-s` of the `MviViewModel` interface: 
> - `toViewStates(initialState: T, scope: CoroutineScope)`, 
> - `toViewEffects(scope: CoroutineScope)`, 
> - `toNavigationEffects(scope: CoroutineScope)`. 

Special caution is required while defining and assigning fields at the same time, e.g.
```kotlin
override val viewEffects: StateFlow<ConsumableEvent<ChoiceViewEffect>?> = MutableStateFlow<ConsumableEvent<ChoiceViewEffect>?>(null)
```
⚠ You should explicitly type all MVI fields to `StateFlow<...>` - otherwise you will expose the assigned object (usually `MutableStateFlow`). 
Alternatively you may use backing fields to avoid this issue:
```kotlin
private val _viewEffects = MutableStateFlow<ConsumableEvent<ChoiceViewEffect>?>(null)
override val viewEffects: StateFlow<ConsumableEvent<ChoiceViewEffect>?> = _viewEffect
```
Note: you will be still able to mutate the view state even if you choose the first option (defining and assigning at the same time) - the `dispatchViewEffect`, `dispatchNavigationEffect` and `val viewState` 
extension methods check the real type of the `flow` and perform necessary casting if required. 

#### Updating view state
If you extend `BaseMviViewModel` OR your custom view-model assigns `MutableStateFlow` to `val viewStates: StateFlow<VS>` you may use `viewState` extension method to update the view state:

```kotlin
class ChoiceViewModel ... {
    ... 
    private fun drawNumber() {
        viewState = viewState.copy(
            randomNumber = Random.nextInt(0, 100)
        )    
    }
    ...
}
```

#### Dispatching view effects
If you extend `BaseMviViewModel` OR your custom view-model assigns `MutableStateFlow` to `val viewEffects: StateFlow<ConsumableEvent<VE>?>` you may use `dispatchViewEffect` extension method:

```kotlin
class ChoiceViewModel ... {
    ... 
    private fun showToast() {
        dispatchViewEffect(ChoiceViewEffect.ShowToast("Toast message!")) 
    }
    ...
}
```
> ⚠ Most of the time you should not use view effects at all - even displaying the `AlertDialogs` should generally be managed with the view state.

#### Dispatching navigation effects
If you extend `BaseMviViewModel` OR your custom view-model assigns `MutableStateFlow` to `val navigationEffects: StateFlow<ConsumableEvent<NE>?>` you may use `dispatchNavigationEffect` extension method:

```kotlin
class ChoiceViewModel ... {
    ... 
    private fun navigateToCardDetails() {
        dispatchNavigationEffect(NavigateToDetails(event.id))
    }
    ...
}
```

### Use view-model (Compose)

#### Injecting view-model

```kotlin
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = viewModel<ChoiceViewModel>(),
)
```
> ⚠ Generally you should type the `viewModel` parameter to the `MviViewModel<...>` interface. 
> Otherwise providing preview may be very hard especially if your view-model has many dependencies (e.g. tons of use cases).
>
> If you really don't intend to use preview you may simplify the view-model injection (still not recommended as makes testing harder):
> ```kotlin
> @Composable
> fun ChoiceScreen(
>     viewModel: ChoiceViewModel = viewModel(),
> )
> ```

You may provide parameters to view-models during injection:
```kotlin
class DetailsViewModelFactory(private val id: String) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailsViewModel(id) as T
}

@Composable
fun DetailsScreen(
    id: String,
    viewModel: MviViewModel<DetailsViewState, ViewEvent, NavigationEffect, ViewEffect> = viewModel(factory = DetailsViewModelFactory(id)),
) {

}
```

#### Injecting view-model with Koin

First you need to declare view-models in Koin `module`.
```kotlin
val appUiModule = module {
    viewModelOf(::ChoiceViewModel)
    viewModel { parameters -> DetailsViewModel(parameters.get()) } // view-model with parameter
}
```

The injection is straightforward.
```kotlin
// view-model without parameters (beside SavedStateHandle)
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = koinViewModel<ChoiceViewModel>(),
    onNavigateToDetails: (String) -> Unit
)

// view-model with parameter
@Composable
fun DetailsScreen(
    id: String,
    viewModel: MviViewModel<DetailsViewState, ViewEvent, NavigationEffect, ViewEffect> = koinViewModel<DetailsViewModel> { parametersOf(id) }
)
```

#### Handling view state changes.
There are two equivalent methods of handling view state changes.

First option is to use `ViewState` composable. Inside the passed content lambda there is a `viewState` property available which holds the latest view state.
```kotlin
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = koinViewModel<ChoiceViewModel>(),
    onNavigateToDetails: (String) -> Unit
) {
    ViewState(viewModel) {
        Text(
            text = "${viewState.randomNumber}",
            style = TextStyle.Default.copy(
                fontSize = 72.sp
            )
        )
    }
    ...
}
```
>ℹ Although the `viewState` field is typed to `<VS : ViewState>` it is a delegate pointing to a `<State<VS : ViewState>>` under the hood.
Thanks to this the whole view is not recomposed when the `viewState` changes - only the parts reading from `viewState` are recomposed.

The second option is to use `by viewState(viewModel)` delegation:
```kotlin
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = koinViewModel<ChoiceViewModel>(),
    onNavigateToDetails: (String) -> Unit
) {
    val viewState by viewState(viewModel)
    
    Text(
        text = "${viewState.randomNumber}",
        style = TextStyle.Default.copy(
            fontSize = 72.sp
        )
    )
}
```
>ℹ Although the `viewState` variable is typed to `<VS : ViewState>` it is a delegate pointing to a `<State<VS : ViewState>>` under the hood.
Thanks to this the whole view is not recomposed when the `viewState` changes - only the parts reading from `viewState` are recomposed.

#### Dispatching view events

Use `viewModel.onEvent(EV)` to provide view event to view model.

>ℹ Consider using `com.gft.compose.interaction.InteractionFilter(minActiveState: Lifecycle.State, content: @Composable () -> Unit)` to filter user input when view is not resumed or started.

```kotlin
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = koinViewModel<ChoiceViewModel>(),
    onNavigateToDetails: (String) -> Unit
) {
    ...
    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = { viewModel.onEvent(OnShowToastClicked) }
    ) {
      Text(text = "Show toast")
    }
    ...
}
```

#### View state subscription lifetime

By default, both the `ViewState` and `by viewState(viewModel)` subscribes to the `viewModel.viewStates` flow when view enters `Lifecycle.State.STARTED`
state and cancels the subscription when view is stopped.

In some rare scenarios you may want to keep the subscription alive even if the view is not visible (not in `Lifecycle.State.STARTED` state),
especially if the `viewModel.viewStates` is backed by a cold flow that should keep working even if app is in background.
You may specify the minimum lifecycle state at which the subscription is active using `minActiveState` param:

```kotlin
ViewState(
    viewModel = viewModel,
    minActiveState = Lifecycle.State.CREATED
) { effect ->

}
```
```kotlin
val viewState by viewState(viewModel = viewModel, minActiveState = Lifecycle.State.CREATED)
```
> ℹ Although using `Lifecycle.State.CREATED` as `minActiveState` keeps the subscription active the Compose suspends recomposition when view is stopped.
> Thanks to this the app is not wasting resources on invisible updates while the view model may keep its work.

#### Handling navigation effects

Use `NavigationEffect` composable method to handle navigation effects. 
```kotlin
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = koinViewModel<ChoiceViewModel>(),
    onNavigateToDetails: (String) -> Unit
) {
    NavigationEffect(viewModel) { effect ->
        when (effect) {
            is NavigateToDetails -> {
                onNavigateToDetails(effect.id)
            }
        }
    }
    ...
}
```
> ℹ Each navigation effect is provided only once. It won't be repeated even if the view is recreated.

By default the `NavigationEffect` handles the navigation effects only if the view is at least in the `Lifecycle.State.RESUMED` state.
Navigation effects are ignored if the view is visible but paused. The last navigation effect dispatched by the view model is handled 
once the view is resumed.


To modify this behaviour, you can specify the minimum required view state. For example, if you pass Lifecycle.State.STARTED, all 
navigation effects will be ignored while the current screen is not visible. When the user returns, only the last navigation effect 
triggered by the view-model during that time will be processed.

```kotlin
NavigationEffect(
    viewModel = viewModel,
    minActiveState = Lifecycle.State.STARTED
) { effect ->

}
```
> ⚠ In Compose it is impossible to handle navigation effects when the view is not started even if you pass `Lifecycle.State.CREATED` as minimum view state.
All the recompositions are suspended when the view is in stopped.

#### Handling view effects

Use `ViewEffect` composable method to handle view effects.
```kotlin
@Composable
fun ChoiceScreen(
    viewModel: MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect> = koinViewModel<ChoiceViewModel>(),
    onNavigateToDetails: (String) -> Unit
) {
    ViewEffect(viewModel) { effect ->
        when (effect) {
            is ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }
    ...
}
```
> ℹ Each view effect is provided only once. It won't be repeated even if the view is recreated.

By default the `ViewEffect` handles the view effects only if the view is in the `Lifecycle.State.RESUMED` state.
View effects are ignored if the view is visible but paused. The last view effect dispatched by the view model is handled
once the view is resumed.

To modify this behaviour, you can specify the minimum required view state. For example, if you pass Lifecycle.State.STARTED,
all view effects will be ignored while the current screen is not visible. When the user returns, only the last view effect 
triggered by the view-model during that time will be processed.
```kotlin
ViewEffect(
    viewModel = viewModel,
    minActiveState = Lifecycle.State.STARTED
) { effect ->

}
```
> ⚠ In Compose it is impossible to handle view effects when the view is not started even if you pass `Lifecycle.State.CREATED` as minimum view state.
All the recompositions are suspended when the view is in stopped.

