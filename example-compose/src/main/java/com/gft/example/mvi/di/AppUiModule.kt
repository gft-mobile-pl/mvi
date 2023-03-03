package com.gft.example.mvi.di

import com.gft.example.mvi.ui.details.DetailsViewModel
import com.gft.example.mvi.ui.screens.ChoiceViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appUiModule = module {
    viewModelOf(::ChoiceViewModel)
    viewModel { (id: String) -> DetailsViewModel(id) }
}
