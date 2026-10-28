package com.bahalabs.sortopia.ui.settings

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahalabs.sortopia.BuildConfig
import com.bahalabs.sortopia.R
import com.bahalabs.sortopia.SortopiaApp
import com.bahalabs.sortopia.ui.common.GlassCard
import com.bahalabs.sortopia.ui.theme.BoardTheme
import kotlinx.coroutines.launch

/** Hosted alongside the store listing; Play requires it for apps with ads. */
const val PRIVACY_POLICY_URL = "https://tubejoy-bahalabs.web.app/privacy.html"

@Composable
fun SettingsScreen(onBack: () -> Unit, onPrivacy: () -> Unit) {
    val app = LocalContext.current.applicationContext as SortopiaApp
    val c = app.container
    val progress by c.store.progress.collectAsState(initial = null)
    val p = progress ?: return
    val scope = rememberCoroutineScope()
    val activity: Activity = LocalActivity.current ?: return
    val privacyRequired by c.ads.privacyOptionsRequired.collectAsState()

    Box(Modifier.fillMaxSize().background(p.theme.brush)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Header(stringResource(R.string.settings), onBack)
            GlassCard(Modifier.fillMaxWidth()) {
                Toggle(stringResource(R.string.music), null, p.music, p.theme) { on -> if (!on) c.music.stop(); scope.launch { c.store.setMusic(on) } }
                Toggle(stringResource(R.string.sound), null, p.sound, p.theme) { on -> c.sounds.enabled = on; scope.launch { c.store.setSound(on) } }
                Toggle(stringResource(R.string.vibration), null, p.vibration, p.theme) { scope.launch { c.store.setVibration(it) } }
                Toggle(stringResource(R.string.color_symbols), stringResource(R.string.color_symbols_desc), p.colorSymbols, p.theme) { scope.launch { c.store.setColorSymbols(it) } }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                if (privacyRequired) Link(stringResource(R.string.privacy_options)) { c.ads.showPrivacyOptions(activity) }
                Link(stringResource(R.string.privacy_policy), onPrivacy)
            }
            Text(
                stringResource(R.string.version, BuildConfig.VERSION_NAME),
                color = Color(0x99FFFFFF),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
fun Header(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
    }
}

@Composable
private fun Toggle(title: String, subtitle: String?, checked: Boolean, theme: BoardTheme, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, color = Color(0xB3FFFFFF), style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = theme.accent, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun Link(title: String, onClick: () -> Unit) {
    Text(
        title,
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}
