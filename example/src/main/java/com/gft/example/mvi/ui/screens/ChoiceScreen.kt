package com.gft.example.mvi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gft.example.mvi.ui.screens.ChoiceNavigationEffect.NavigateToDetails
import com.gft.example.mvi.ui.screens.ChoiceViewEffect.ShowToast
import com.gft.example.mvi.ui.screens.ChoiceViewEvent.OnDrawNumberClicked
import com.gft.example.mvi.ui.screens.ChoiceViewEvent.OnShowDetailsClicked
import com.gft.example.mvi.ui.screens.ChoiceViewEvent.OnShowToastClicked
import com.gft.mvi.NavigationEffect
import com.gft.mvi.ViewEffect
import com.gft.mvi.viewState
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChoiceScreen(
    viewModel: ChoiceViewModel = koinViewModel(),
    onNavigateToDetails: (String) -> Unit
) {
    NavigationEffect(viewModel) { effect ->
        when (effect) {
            is NavigateToDetails -> {
                onNavigateToDetails(effect.id)
            }
        }
    }

    val context = LocalContext.current
    ViewEffect(viewModel) { effect ->
        when (effect) {
            is ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Card(
            modifier = Modifier.width(180.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onEvent(OnShowToastClicked) }
                ) {
                    Text(text = "Show toast")
                }
            }

        }

        Card(
            modifier = Modifier.width(180.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onEvent(OnShowDetailsClicked("1")) }
                ) {
                    Text(text = "Show details #1")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onEvent(OnShowDetailsClicked("2")) }
                ) {
                    Text(text = "Show details #2")
                }
            }

        }

        Card(
            modifier = Modifier.width(180.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.onEvent(OnDrawNumberClicked) }
                ) {
                    Text(text = "Draw a number")
                }
                Text(text = "Random number")
                Text(
                    text = "${viewModel.viewState.randomNumber}",
                    style = TextStyle.Default.copy(
                        fontSize = 72.sp
                    )
                )
            }
        }
    }
}

@Preview(showSystemUi = true, heightDp = 800)
@Composable
fun ChoiceScreenPreview() {
    ChoiceScreen(
        onNavigateToDetails = {}
    )
}