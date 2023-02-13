# MVI ViewModel

## Quickstart

### Define contract between view and view model
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

sealed interface ChoiceViewEffect : ViewEffect {
    data class ShowToast(val message: String) : ChoiceViewEffect
}

sealed interface ChoiceNavigationEffect : NavigationEffect {
    data class NavigateToDetails(val id: String) : ChoiceNavigationEffect
}
```

> 👍 Success
>
> Callout test


{% note %}
**Note:** Callout test
{% endnote %}