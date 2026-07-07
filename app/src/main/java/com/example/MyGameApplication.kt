package com.example

import android.app.Application

class MyGameApplication : Application() {
    override fun getAttributionTag(): String? {
        return "audio"
    }
}
