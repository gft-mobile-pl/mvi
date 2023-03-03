package com.gft.example.mvi.xml.di

import com.gft.example.mvi.xml.ui.choose.ChooseViewModel
import com.gft.example.mvi.xml.ui.details.DetailsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

internal val appModule = module {
    viewModelOf(::ChooseViewModel)
    viewModel { (id: String) -> DetailsViewModel(id) }
}
