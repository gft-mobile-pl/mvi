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
- `initialViewState` - in the MVI (and Compose) the initial state of the view is always required.<br />
**Note:** initial data may not be a "correct" if you plan to provide the real data synchronously 
in the `init` block (e.g. using `launch(start = CoroutineStart.UNDISPATCHED) {}`) 
- `savedStateHandle` (optional) - if you pass `SavedStateHandle` to the `BaseMviViewModel` it will automatically save/restore the view state when activity is killed/recreated.

It is also possible to inherit from `BaseMviViewModel` and override particular fields:
```kotlin
class ChoiceViewModel(streamData: StreamUserChoiceDataUseCase) : BaseMviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>(ChoiceViewState(0)) {

    override val viewStates = streamData()
        .map { data -> ChoiceViewState(data) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ChoiceViewState(0))

    override fun onEvent(event: ChoiceViewEvent) {}
}
```
This approach saves a bit of time as the rest of the fields are provided by the `BaseMviViewModel` however:
- you still need to pass `initialState` to the constructor even though it won't be used,
- if you override `viewStates` field the automatic save/restore mechanism won't work anymore even if you pass `SavedStateHandle` to the base class constructor.


#### Using `MviViewModel` interface

Extending `BaseMviViewModel` is not mandatory - it is enough to implement the `MviViewModel`. This is very useful if you don't want to manage the ViewState "manually" inside the view model, 
but rather fetch it from domain.

```kotlin
class ChoiceViewModel(streamData: StreamUserChoiceDataUseCase) : MviViewModel<ChoiceViewState, ChoiceViewEvent, ChoiceNavigationEffect, ChoiceViewEffect>, ViewModel() {
    
    override val viewStates: StateFlow<ChoiceViewState> = fetchData()
        .map { data -> ChoiceViewState(data) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ChoiceViewState(0))
    
    override val viewEffects: StateFlow<ConsumableEvent<ChoiceViewEffect>?> = MutableStateFlow<ConsumableEvent<ChoiceViewEffect>?>(null)
    override val navigationEffects: StateFlow<ConsumableEvent<ChoiceNavigationEffect>?> = MutableStateFlow<ConsumableEvent<ChoiceNavigationEffect>?>(null)
    override fun onEvent(event: ChoiceViewEvent) { }
}
```

Special caution is required while defining and assigning fields at the same time, e.g.
```kotlin
override val viewEffects: StateFlow<ConsumableEvent<ChoiceViewEffect>?> = MutableStateFlow<ConsumableEvent<ChoiceViewEffect>?>(null)
```
You should explicitly type the fields to `StateFlow<...>` - otherwise you will expose the `MutableStateFlow` (generally the type of the backing object). 
Don't worry - you will be still able to mutate the view state with provided extension methods - read on. 

As a rule of thumb you should use backing fields to avoid this inconvenience.

#### Updating view state
If you extend `BaseMviViewModel` or your custom view model assigns `MutableStateFlow` to `val viewStates: StateFlow<VS>` you may use `viewState` extension method to update the view state:

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
If you extend `BaseMviViewModel` or your custom view model assigns `MutableStateFlow` to `val viewEffects: StateFlow<ConsumableEvent<VE>?>` you may use `dispatchViewEffect` extension method:

```kotlin
class ChoiceViewModel ... {
    ... 
    private fun showToast() {
        dispatchViewEffect(ChoiceViewEffect.ShowToast("Toast message!")) 
    }
    ...
}
```
**Note:** Most of the time you should not use view effects at all - even displaying the `AlertDialogs` should generally be managed with the view state.

#### Dispatching navigation effects
If you extend `BaseMviViewModel` or your custom view model assigns `MutableStateFlow` to `val navigationEffects: StateFlow<ConsumableEvent<NE>?>` you may use `dispatchNavigationEffect` extension method:

```kotlin
class ChoiceViewModel ... {
    ... 
    private fun showToast() {
        dispatchNavigationEffect(NavigateToDetails(event.id))
    }
    ...
}
```