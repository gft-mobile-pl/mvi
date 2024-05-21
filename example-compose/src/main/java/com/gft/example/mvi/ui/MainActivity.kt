package com.gft.example.mvi.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gft.example.mvi.ui.counter.CounterScreen
import com.gft.example.mvi.ui.details.DetailsScreen
import com.gft.example.mvi.ui.screens.ChoiceScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            MaterialTheme {
                NavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    navController = navController,
                    startDestination = "choice"
                ) {

                    composable(
                        route = "choice"
                    ) {
                        ChoiceScreen(
                            onNavigateToDetails = { id ->
                                navController.navigate("details/$id")
                            },
                            onNavigateToCounter = {
                                navController.navigate("counter")
                            }
                        )
                    }

                    composable(
                        route = "details/{id}"
                    ) { backStackEntry ->
                        DetailsScreen(id = backStackEntry.arguments?.getString("id") ?: "WTF?")
                    }

                    composable(
                        route = "counter"
                    ) {
                        CounterScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
