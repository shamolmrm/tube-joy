package com.bahalabs.sortopia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bahalabs.sortopia.engine.GameMode
import com.bahalabs.sortopia.ui.game.GameScreen
import com.bahalabs.sortopia.ui.game.Puzzle
import com.bahalabs.sortopia.ui.home.HomeScreen
import com.bahalabs.sortopia.ui.settings.PrivacyScreen
import com.bahalabs.sortopia.ui.settings.SettingsScreen
import com.bahalabs.sortopia.ui.settings.ThemesScreen
import com.bahalabs.sortopia.ui.theme.SortopiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        (application as SortopiaApp).container.ads.gatherConsent(this)
        // Debug builds only: `adb shell am start ... --es route game/WATER/42` opens a screen
        // directly, for store screenshots and testing late levels.
        val start = intent.getStringExtra("route")?.takeIf { BuildConfig.DEBUG } ?: "home"
        setContent {
            SortopiaTheme { AppNav(start) }
        }
    }

    // Leaving the app (or an ad opening) cuts the short welcome tune off at once.
    override fun onPause() {
        (application as SortopiaApp).container.music.stop()
        super.onPause()
    }
}

@Composable
private fun AppNav(start: String) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = start,enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
        composable("home") {
            HomeScreen(
                onPlay = { mode, level -> nav.navigate("game/${mode.name}/$level") },
                onDaily = { day -> nav.navigate("daily/$day") },
                onSettings = { nav.navigate("settings") },
                onThemes = { nav.navigate("themes") },
            )
        }
        composable(
            "game/{mode}/{level}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType }, navArgument("level") { type = NavType.IntType }),
        ) { entry ->
            val mode = GameMode.valueOf(entry.arguments?.getString("mode") ?: GameMode.WATER.name)
            val level = entry.arguments?.getInt("level") ?: 1
            GameScreen(
                puzzle = Puzzle.Level(mode, level),
                onBack = { nav.popBackStack() },
                onNext = { next ->
                    if (next is Puzzle.Level) {
                        nav.navigate("game/${next.mode.name}/${next.number}") {
                            popUpTo("home")
                        }
                    }
                },
            )
        }
        composable("daily/{day}", arguments = listOf(navArgument("day") { type = NavType.LongType })) { entry ->
            val day = entry.arguments?.getLong("day") ?: 0L
            GameScreen(puzzle = Puzzle.Daily(day), onBack = { nav.popBackStack() }, onNext = { nav.popBackStack() })
        }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }, onPrivacy = { nav.navigate("privacy") }) }
        composable("privacy") { PrivacyScreen(onBack = { nav.popBackStack() }) }
        composable("themes") { ThemesScreen(onBack = { nav.popBackStack() }) }
    }
}
