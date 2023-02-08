package com.gft.example.mvi.ui.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.gft.mvi.viewState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailsScreen(
    id: String,
    viewModel: DetailsViewModel = koinViewModel { parametersOf(id) }
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = viewModel.viewState.message,
            style = TextStyle.Default.copy(
                fontSize = 144.sp
            )
        )
    }
}

@Preview(showSystemUi = true, heightDp = 800)
@Composable
fun DetailsScreenPreview() {
    DetailsScreen("5")
}