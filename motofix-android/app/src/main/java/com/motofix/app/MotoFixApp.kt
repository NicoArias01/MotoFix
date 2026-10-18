package com.motofix.app

import android.app.Application
import com.motofix.app.data.AppContainer

class MotoFixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
