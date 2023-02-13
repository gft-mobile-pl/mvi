# MVI ViewModel

## Usage

### Define contract between view and view model
- Your view state class has to implemented `ViewState` marker interface.
- View events have to implemented `ViewEvent` marker interface.
- Navigation effects have to implemented `NavigationEffect` marker interface.
- View effects have to implemented `ViewEffect` marker interface.
```kotlin
@Parcelize
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
ℹ Your view state doesn't have to implement `Parcelable` interface if you don't intend to store the view state in `SavedStateHandle`.

ℹ You don't have to define empty classes, e.g. if you don't need `view effect` simply don't define it. 

ℹ `ViewState`, `ViewEvent`, `NavigationEffect`, `ViewEffect` are just marker interfaces. 
Theoretically we would be just fine without them but:
- they open possibility to create extensions methods which are scoped to the particular MVI contract parts,
- they prevent various common mistakes like when the generic parameters are specified in a wrong order while defining view model.

### Define view model
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
```
`BaseMviViewModel` accepts two parameters:
- `initialViewState` - in the `Compose` world the initial state of the view is always required. 
**Note:** initial data may not be a "correct data" if you plan to provide the real data synchronously 
in the `init` block (e.g. using `launch(start = CoroutineStart.UNDISPATCHED) {}`) 
- `savedStateHandle` (optional) - if you pass `SavedStateHandle` to the `BaseMviViewModel` it will automatically save/restore the view state when activity is killed/recreated.

#### Using `MviViewModel` interface
Extending `BaseMviViewModel` is not mandatory. 
```kotlin
class ChoiceViewModel(fetchData: FetchChoiceDataUseCase) : MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>, ViewModel() {
    override val viewStates: State<ChoiceViewState> = fetchData().collectAsStateWithLifecycle()mutableStateOf(ChoiceViewState(0))
    override val viewEffects: State<ConsumableEvent<ChoiceViewEffect>?> = mutableStateOf<ConsumableEvent<ChoiceViewEffect>?>(null)
    override val navigationEffects: State<ConsumableEvent<ChoiceNavigationEffect>?> = mutableStateOf<ConsumableEvent<ChoiceNavigationEffect>?>(null)

    override fun onEvent(event: ChoiceViewEvent) {
        
    }
}
```
