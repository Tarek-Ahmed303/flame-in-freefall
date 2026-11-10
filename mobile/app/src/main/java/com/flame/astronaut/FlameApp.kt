package com.flame.astronaut

import android.app.Application
import com.flame.astronaut.di.AppContainer

class FlameApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // English-only app: keep numbers as 0-9 and "." even on an Arabic-locale phone.
        java.util.Locale.setDefault(java.util.Locale.US)
        AppPrefs.init(this)
        container = AppContainer(this)
    }
}
