package com.proofapp.android

import android.app.Application
import com.proofapp.android.di.AppContainer
import com.proofapp.android.ui.cooking.TimerNotifications
import kotlinx.coroutines.launch

class ProofApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        TimerNotifications.createChannel(this)

        container.applicationScope.launch {
            container.recipeRepository.seedIfEmpty()
            container.syncScheduler.requestSync()
            container.syncScheduler.schedulePeriodic()
        }
    }
}
