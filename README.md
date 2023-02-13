# MVI ViewModel

## Usage

### Define contract between view and view model

#### View state
```kotlin
@Parcelize
data class ChoiceViewState(
    val randomNumber: Int
) : ViewState, Parcelable
```
> ⚠ You must implement empty marker interface `ViewState`.
> 
> ℹ You don't have to implement `Parcelable` if you do not intent to store the view state in `SavedStateHandle`.

#### View events
```kotlin
sealed interface ChoiceViewEvent : ViewEvent {
    data class OnShowDetailsClicked(val id: String) : ChoiceViewEvent
    object OnDrawNumberClicked : ChoiceViewEvent
    object OnShowToastClicked : ChoiceViewEvent
}
```
> ⚠ You must implement empty marker interface `ViewEvent`.

#### Navigation effects
```kotlin
sealed interface ChoiceNavigationEffect : NavigationEffect {
    data class NavigateToDetails(val id: String) : ChoiceNavigationEffect
}
```
> ⚠ You must implement empty marker interface `NavigationEffect`.

#### View effects
```kotlin
sealed interface ChoiceViewEffect : ViewEffect {
    data class ShowToast(val message: String) : ChoiceViewEffect
}
```
> ⚠ You must implement empty marker interface `ViewEffect`.
