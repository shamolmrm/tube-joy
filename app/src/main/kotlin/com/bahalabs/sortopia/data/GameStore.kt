package com.bahalabs.sortopia.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bahalabs.sortopia.engine.GameMode
import com.bahalabs.sortopia.ui.theme.BoardTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sortopia")

data class Progress(
    val coins: Int,
    /** The next level to play, per mode. */
    val levels: Map<GameMode, Int>,
    /** Epoch day of the last finished daily challenge. */
    val dailyDoneDay: Long,
    val sound: Boolean,
    val music: Boolean,
    val vibration: Boolean,
    val colorSymbols: Boolean,
    val ownedThemes: Set<BoardTheme>,
    val theme: BoardTheme,
)

/** Everything the player keeps between sessions. Small and local: one preferences file. */
class GameStore(private val context: Context) {

    val progress: Flow<Progress> = context.dataStore.data.map { p ->
        Progress(
            coins = p[COINS] ?: START_COINS,
            levels = GameMode.entries.associateWith { p[levelKey(it)] ?: 1 },
            dailyDoneDay = p[DAILY_DONE] ?: -1L,
            sound = p[SOUND] ?: true,
            music = p[MUSIC] ?: true,
            vibration = p[VIBRATION] ?: true,
            colorSymbols = p[SYMBOLS] ?: false,
            ownedThemes = (p[OWNED] ?: emptySet()).mapNotNull { name -> BoardTheme.entries.find { it.name == name } }.toSet() + BoardTheme.MIDNIGHT,
            theme = p[THEME]?.let { name -> BoardTheme.entries.find { it.name == name } } ?: BoardTheme.MIDNIGHT,
        )
    }

    suspend fun current(): Progress = progress.first()

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit { it[COINS] = (it[COINS] ?: START_COINS) + amount }
    }

    /** Takes [amount] coins if the player has them. */
    suspend fun spend(amount: Int): Boolean {
        var ok = false
        context.dataStore.edit {
            val have = it[COINS] ?: START_COINS
            if (have >= amount) {
                it[COINS] = have - amount
                ok = true
            }
        }
        return ok
    }

    suspend fun completeLevel(mode: GameMode, level: Int) {
        context.dataStore.edit {
            val key = levelKey(mode)
            if ((it[key] ?: 1) <= level) it[key] = level + 1
            it[LEVELS_SINCE_AD] = (it[LEVELS_SINCE_AD] ?: 0) + 1
        }
    }

    suspend fun completeDaily(epochDay: Long) {
        context.dataStore.edit { it[DAILY_DONE] = epochDay }
    }

    /** Levels finished since the last full-screen ad. */
    suspend fun levelsSinceAd(): Int = context.dataStore.data.first()[LEVELS_SINCE_AD] ?: 0

    suspend fun resetLevelsSinceAd() {
        context.dataStore.edit { it[LEVELS_SINCE_AD] = 0 }
    }

    suspend fun setSound(on: Boolean) = context.dataStore.edit { it[SOUND] = on }
    suspend fun setMusic(on: Boolean) = context.dataStore.edit { it[MUSIC] = on }
    suspend fun setVibration(on: Boolean) = context.dataStore.edit { it[VIBRATION] = on }
    suspend fun setColorSymbols(on: Boolean) = context.dataStore.edit { it[SYMBOLS] = on }

    /** Buys [theme] with coins if needed, then selects it. */
    suspend fun useTheme(theme: BoardTheme): Boolean {
        var ok = false
        context.dataStore.edit {
            val owned = it[OWNED] ?: emptySet()
            val have = it[COINS] ?: START_COINS
            when {
                theme == BoardTheme.MIDNIGHT || theme.name in owned -> ok = true
                have >= theme.price -> {
                    it[COINS] = have - theme.price
                    it[OWNED] = owned + theme.name
                    ok = true
                }
            }
            if (ok) it[THEME] = theme.name
        }
        return ok
    }

    private companion object {
        const val START_COINS = 200
        val COINS = intPreferencesKey("coins")
        val DAILY_DONE = longPreferencesKey("daily_done")
        val LEVELS_SINCE_AD = intPreferencesKey("levels_since_ad")
        val SOUND = booleanPreferencesKey("sound")
        val MUSIC = booleanPreferencesKey("music")
        val VIBRATION = booleanPreferencesKey("vibration")
        val SYMBOLS = booleanPreferencesKey("color_symbols")
        val OWNED = stringSetPreferencesKey("owned_themes")
        val THEME = stringPreferencesKey("theme")
        fun levelKey(mode: GameMode) = intPreferencesKey("level_${mode.name.lowercase()}")
    }
}
