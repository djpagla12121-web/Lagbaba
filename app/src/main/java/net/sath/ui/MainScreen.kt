package net.sath.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.sath.model.LagConfig
import net.sath.ui.components.SnowfallBackground
import net.sath.ui.screens.DiagnosticsScreen
import net.sath.ui.screens.FirewallScreen
import net.sath.ui.screens.HomeScreen
import net.sath.ui.screens.SettingsScreen
import net.sath.ui.theme.AccentBlue
import net.sath.ui.theme.BgBottom
import net.sath.ui.theme.BgMid
import net.sath.ui.theme.BgTop
import net.sath.ui.theme.NeonPurple
import net.sath.ui.theme.PanelAlt
import net.sath.ui.theme.PanelDark
import net.sath.ui.theme.StrokeDark
import net.sath.ui.theme.StrokeSoft
import net.sath.ui.theme.TeleCyan
import net.sath.ui.theme.TextMuted
import net.sath.ui.theme.TextSoft
import net.sath.ui.theme.TextWhite

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestOverlay: () -> Unit,
    onRequestVpn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val hasOverlayPerm by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val hasVpnPerm by viewModel.hasVpnPermission.collectAsStateWithLifecycle()
    val serverTargets by viewModel.serverTargets.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val isEn = config.language == "en"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgMid, BgBottom)
                )
            )
    ) {
        // Decorative glowing orbs from original design
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 30.dp)
                .clip(CircleShape)
                .background(NeonPurple.copy(alpha = 0.10f))
        )

        // Interactive 3D Canvas Snowfall
        SnowfallBackground()

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(280),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        config = config,
                        diagnostics = diagnostics,
                        hasOverlayPerm = hasOverlayPerm,
                        hasVpnPerm = hasVpnPerm,
                        onRequestOverlay = onRequestOverlay,
                        onRequestVpn = onRequestVpn,
                        onStartServices = { viewModel.startAllServices() },
                        onStopServices = { viewModel.stopAllServices() },
                        onToggleLagMode = { viewModel.toggleLagMode(it) },
                        onUpdateConfig = { viewModel.updateConfig(it) }
                    )
                    1 -> SettingsScreen(
                        config = config,
                        onUpdateConfig = { viewModel.updateConfig(it) }
                    )
                    2 -> FirewallScreen(
                        config = config,
                        onAddIp = { viewModel.addBlockedIp(it) },
                        onRemoveIp = { viewModel.removeBlockedIp(it) },
                        onAddDomain = { viewModel.addBlockedDomain(it) },
                        onRemoveDomain = { viewModel.removeBlockedDomain(it) },
                        onToggleFirewall = { enabled ->
                            viewModel.updateConfig { it.copy(isFirewallEnabled = enabled) }
                        }
                    )
                    3 -> DiagnosticsScreen(
                        config = config,
                        diagnostics = diagnostics,
                        serverTargets = serverTargets,
                        onRunPingTest = { viewModel.runPingDiagnostics() }
                    )
                }
            }
        }

        // Custom Cyberpunk Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(PanelDark.copy(alpha = 0.95f))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(StrokeDark, AccentBlue.copy(alpha = 0.4f), StrokeDark)),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    label = if (isEn) "Home" else "Trang Chủ",
                    icon = Icons.Default.Home,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    tag = "nav_home"
                )
                NavBarItem(
                    label = if (isEn) "HUD Setup" else "Cài Đặt",
                    icon = Icons.Default.Settings,
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    tag = "nav_settings"
                )
                NavBarItem(
                    label = if (isEn) "Firewall" else "Tường Lửa",
                    icon = Icons.Default.Lock,
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    tag = "nav_firewall"
                )
                NavBarItem(
                    label = if (isEn) "Diagnostics" else "Chẩn Đoán",
                    icon = Icons.Default.Info,
                    isSelected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    tag = "nav_diagnostics"
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val activeColor = AccentBlue
    val inactiveColor = TextMuted

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) activeColor.copy(alpha = 0.18f) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) TextWhite else TextMuted
        )
    }
}
