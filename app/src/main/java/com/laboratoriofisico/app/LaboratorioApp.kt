package com.laboratoriofisico.app

import android.app.Application
import com.laboratoriofisico.app.core.di.AppContainer
import com.laboratoriofisico.app.core.debug.DebugSessionLog
import kotlinx.coroutines.runBlocking

class LaboratorioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        DebugSessionLog.init(this)
        container = AppContainer(this)
        runBlocking {
            container.initialize()
        }
    }
}
