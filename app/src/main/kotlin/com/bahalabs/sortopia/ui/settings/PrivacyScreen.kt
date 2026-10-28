package com.bahalabs.sortopia.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.bahalabs.sortopia.R
import com.bahalabs.sortopia.SortopiaApp
import com.bahalabs.sortopia.ui.common.GlassCard
import com.bahalabs.sortopia.ui.common.PillButton

/** Same text as the hosted page (store/privacy.html); keep both in sync. */
const val PRIVACY_UPDATED = "28 October 2026"
const val SUPPORT_EMAIL = "hamidakhan7654@gmail.com"

private data class Section(val emoji: String, val title: String, val body: String)

private val sections = listOf(
    Section(
        "🎮", "Who we are",
        "Tube Joy: Water Sort Puzzle is made by BahaLabs. This policy explains what information the game uses and why. " +
            "We wrote it to be short and clear.",
    ),
    Section(
        "📱", "What stays on your phone",
        "Your levels, coins, themes and settings are saved only on your device. There is no account and no sign-in, " +
            "and we do not run servers that collect your game data.",
    ),
    Section(
        "📢", "Ads (Google AdMob)",
        "The game is free and shows ads from Google AdMob. To show and measure ads, Google may collect your device's " +
            "advertising ID, approximate location (from your IP address), app interactions and crash diagnostics. " +
            "Google uses this under its own privacy policy. Ads are limited to family-friendly content.",
    ),
    Section(
        "✅", "Your choices",
        "In the EEA, the UK and Switzerland we ask for your consent before any personalised ads; you can change it any " +
            "time in Settings → Privacy options. On any Android phone you can reset or delete your advertising ID in " +
            "Settings → Google → Ads.",
    ),
    Section(
        "🔒", "Sharing and selling",
        "We do not sell your personal information. Apart from Google's ad services described above, the game does not " +
            "share data with anyone.",
    ),
    Section(
        "👶", "Children",
        "Tube Joy is not directed to children under 13, and we do not knowingly collect personal information from them.",
    ),
    Section(
        "🗑️", "Deleting your data",
        "Uninstalling the game removes all game data from your phone. Ad-related data is handled by Google; you can " +
            "delete your advertising ID as described above.",
    ),
    Section(
        "✉️", "Contact",
        "Questions about privacy? Email us at $SUPPORT_EMAIL.",
    ),
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as SortopiaApp
    val progress by app.container.store.progress.collectAsState(initial = null)
    val p = progress ?: return
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(p.theme.brush)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(stringResource(R.string.privacy_policy), onBack)
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(p.theme.accent.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Text("🛡️", fontSize = 36.sp)
                }
                Text(
                    "Your privacy matters",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text("Last updated $PRIVACY_UPDATED", color = Color(0x99FFFFFF), style = MaterialTheme.typography.labelSmall)
            }
            for (s in sections) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.emoji, fontSize = 22.sp)
                        Text(
                            s.title,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    Text(
                        s.body,
                        color = Color(0xD9FFFFFF),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            PillButton(
                "Open web version",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri())) },
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF9C84FF), Color(0xFF5F45D8)),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
