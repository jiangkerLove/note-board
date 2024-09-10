package com.jiangker.noteboard.app

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings

class App : Application() {


    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()
        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
    }

    companion object {
        var androidId = ""
    }
}