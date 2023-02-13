# MVI ViewModel

## Usage

### Define contract between view and view model

#### View state
- Your view state class has to implemented `ViewState` marker interface.
- View events have to implemented `ViewState` marker interface.
- Navigation effects have to implemented `ViewState` marker interface.
- View effects have to implemented `ViewState` marker interface.
- ℹ You don't have to implement `Parcelable` if you do not intent to store the view state in `SavedStateHandle`.
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

