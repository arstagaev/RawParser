package com.avtelma.rawparser

import android.app.Application
import com.avtelma.backgroundparser.tools.VariablesAndConst

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        VariablesAndConst.RECORD_ACTIVITY_FOR_RAWPARSER = MainActivity::class.java
    }
}