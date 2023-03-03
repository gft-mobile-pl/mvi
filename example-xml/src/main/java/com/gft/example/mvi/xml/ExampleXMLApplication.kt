package com.gft.example.mvi.xml

import android.app.Application
import com.gft.example.mvi.xml.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ExampleXMLApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@ExampleXMLApplication)
            modules(appModule)
        }
    }
}
