package com.gft.example.mvi.ui.counter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gft.example.mvi.ui.counter.CounterNavigationEffect.NavigateBack
import com.gft.example.mvi.ui.counter.CounterViewEvent.OnBackClicked
import com.gft.mvi.MviViewModel
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.ViewState
import com.gft.mvi.test.toViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CounterScreen(
    viewModel: MviViewModel<CounterViewState, CounterViewEvent, CounterNavigationEffect, ViewEffect> = koinViewModel<CounterViewModel>(),
    onNavigateBack: () -> Unit,
) {
    BackHandler {
        viewModel.onEvent(OnBackClicked)
    }

    NavigationEffect(viewModel) { effect ->
        when (effect) {
            NavigateBack -> onNavigateBack()
        }
    }

    ViewState(viewModel) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Card(
                modifier = Modifier.width(180.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Counter")
                    Text(
                        text = "${viewState.count}",
                        style = TextStyle.Default.copy(
                            fontSize = 72.sp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true, heightDp = 800)
@Composable
fun CounterScreenPreview() {
    CounterScreen(
        viewModel = CounterViewState(count = 16).toViewModel(),
        onNavigateBack = {}
    )
}

