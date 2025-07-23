package com.sudhakar.backgroundchangerapp

import android.app.Application

class VBApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext
    }
}