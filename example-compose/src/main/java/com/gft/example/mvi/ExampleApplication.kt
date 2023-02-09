package com.gft.example.mvi

import android.app.Application
import com.gft.example.mvi.di.appUiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(applicationContext)
            modules(
                appUiModule
            )
        }
    }
}