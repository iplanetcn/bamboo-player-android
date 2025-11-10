package com.cherrystudios.bamboo.app

import android.app.Application
import com.cherrystudios.bamboo.BuildConfig
import timber.log.Timber

/**
 * App
 *
 * @author john
 * @since 2025-11-10
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }
}