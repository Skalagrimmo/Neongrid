package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.di.DatabaseModule

class MyGameApplication : Application() {
    val container: AppContainer by lazy {
        DatabaseModule.getContainer(this)
    }

    override fun getAttributionTag(): String? {
        return "default"
    }
}
