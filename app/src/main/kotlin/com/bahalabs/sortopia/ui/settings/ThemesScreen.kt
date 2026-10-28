package com.bahalabs.sortopia.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahalabs.sortopia.R
import com.bahalabs.sortopia.SortopiaApp
import com.bahalabs.sortopia.ui.common.CoinChip
import com.bahalabs.sortopia.ui.common.CoinIcon
import com.bahalabs.sortopia.ui.theme.BoardTheme
import com.bahalabs.sortopia.ui.theme.Gold
import kotlinx.coroutines.launch

@Composable
fun ThemesScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as SortopiaApp
    val store = app.container.store
    val progress by store.progress.collectAsState(initial = null)
    val p = progress ?: return
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val noCoins = stringResource(R.string.not_enough_coins)

    Box(Modifier.fillMaxSize().background(p.theme.brush)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Header(stringResource(R.string.themes), onBack)
                Spacer(Modifier.weight(1f))
                CoinChip(p.coins)
            }
            LazyVerticalGrid(
                GridCells.Fixed(2),
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(BoardTheme.entries) { theme ->
                    val owned = theme in p.ownedThemes
                    val selected = theme == p.theme
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(theme.brush)
                            .border(if (selected) 3.dp else 1.dp, if (selected) Gold else Color(0x40FFFFFF), RoundedCornerShape(22.dp))
                            .clickable {
                                scope.launch {
                                    if (!store.useTheme(theme)) Toast.makeText(context, noCoins, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(14.dp)
                            .aspectRatio(0.85f),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(theme.label), color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = 10.dp).clip(RoundedCornerShape(12.dp)).background(theme.accent.copy(alpha = 0.25f)))
                        when {
                            selected -> Text(stringResource(R.string.selected), color = Gold, fontWeight = FontWeight.Bold)
                            owned -> Text(stringResource(R.string.use), color = Color.White, fontWeight = FontWeight.Bold)
                            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CoinIcon()
                                Text(theme.price.toString(), Modifier.padding(start = 6.dp), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
