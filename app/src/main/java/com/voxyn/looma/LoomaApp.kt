package com.voxyn.looma

import android.app.Application

class LoomaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LoomaApp
            private set
    }
}
