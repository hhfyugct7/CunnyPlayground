package com.thevakhovske.cunnyplayground

import android.app.Application
import com.google.android.material.color.DynamicColors

class CunnyPlaygroundApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
