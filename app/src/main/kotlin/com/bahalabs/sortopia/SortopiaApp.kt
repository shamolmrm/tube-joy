package com.bahalabs.sortopia

import android.app.Application
import com.bahalabs.sortopia.ads.Ads
import com.bahalabs.sortopia.audio.Music
import com.bahalabs.sortopia.audio.Sounds
import com.bahalabs.sortopia.data.GameStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** App-wide objects. Small enough that a hand-written container beats a DI framework. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val store = GameStore(app)
    val sounds = Sounds(app)
    val music = Music(app)
    val ads = Ads(app, scope)
}

class SortopiaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            container.store.progress.collect {
                container.sounds.enabled = it.sound
            }
        }
    }
}
