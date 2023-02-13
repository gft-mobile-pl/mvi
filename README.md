# MVI ViewModel

## Usage

### Define contract between view and view model

#### ViewState
```kotlin
@Parcelize
data class ChoiceViewState(
    val randomNumber: Int
) : ViewState, Parcelable
```

You don't have to implement `Parcelable` if you do not intent to store the view state in `SavedStateHandle`.
{: .alert .alert-info}

```kotlin
sealed interface ChoiceViewEvent : ViewEvent {
    data class OnShowDetailsClicked(val id: String) : ChoiceViewEvent
    object OnDrawNumberClicked : ChoiceViewEvent
    object OnShowToastClicked : ChoiceViewEvent
}

sealed interface ChoiceViewEffect : ViewEffect {
    data class ShowToast(val message: String) : ChoiceViewEffect
}

sealed interface ChoiceNavigationEffect : NavigationEffect {
    data class NavigateToDetails(val id: String) : ChoiceNavigationEffect
}
```
