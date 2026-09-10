package net.sath.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sath.model.DiagnosticsState
import net.sath.model.LagConfig
import net.sath.model.LagMode
import net.sath.ui.components.ControlToggleRow
import net.sath.ui.components.GlassCard
import net.sath.ui.components.StatusBadge
import net.sath.ui.theme.AccentBlue
import net.sath.ui.theme.AccentBlueDark
import net.sath.ui.theme.DangerRed
import net.sath.ui.theme.DangerRedDark
import net.sath.ui.theme.FreezeBlue
import net.sath.ui.theme.GhostPurple
import net.sath.ui.theme.GoldYellow
import net.sath.ui.theme.NeonPurple
import net.sath.ui.theme.PanelAlt
import net.sath.ui.theme.StrokeDark
import net.sath.ui.theme.SuccessBorder
import net.sath.ui.theme.SuccessFill
import net.sath.ui.theme.SuccessGreen
import net.sath.ui.theme.TeleCyan
import net.sath.ui.theme.TextMuted
import net.sath.ui.theme.TextSoft
import net.sath.ui.theme.TextWhite

@Composable
fun HomeScreen(
    config: LagConfig,
    diagnostics: DiagnosticsState,
    hasOverlayPerm: Boolean,
    hasVpnPerm: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestVpn: () -> Unit,
    onStartServices: () -> Unit,
    onStopServices: () -> Unit,
    onToggleLagMode: (LagMode) -> Unit,
    onUpdateConfig: ((LagConfig) -> LagConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = config.language == "en"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Master Control Card
        item {
            GlassCard(
                borderColor = if (diagnostics.isVpnActive) SuccessGreen else AccentBlue,
                backgroundColor = Color(0xF00A1322)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FakePingZzz",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextWhite
                            )
                            Text(
                                text = if (isEn) "Game Booster & Lag Engine" else "Tăng Tốc Game & Giả Lập Lag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }

                        StatusBadge(
                            text = if (diagnostics.isVpnActive) (if (isEn) "ACTIVE" else "HOẠT ĐỘNG") else (if (isEn) "STOPPED" else "TẠM DỪNG"),
                            isActive = diagnostics.isVpnActive
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ping & Latency Metrics Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070D16))
                            .border(1.dp, StrokeDark, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricPill(
                            title = "PING",
                            value = "${diagnostics.currentPingMs} ms",
                            color = when {
                                diagnostics.currentPingMs > 500 -> DangerRed
                                diagnostics.currentPingMs > 150 -> GoldYellow
                                else -> SuccessGreen
                            }
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(StrokeDark)
                        )
                        MetricPill(
                            title = "JITTER",
                            value = "±${diagnostics.jitterMs} ms",
                            color = AccentBlue
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(StrokeDark)
                        )
                        MetricPill(
                            title = "MODE",
                            value = if (isEn) diagnostics.currentMode.titleEn else diagnostics.currentMode.titleVi,
                            color = when (diagnostics.currentMode) {
                                LagMode.FREEZE -> FreezeBlue
                                LagMode.GHOST -> GhostPurple
                                LagMode.TELEPORT -> TeleCyan
                                LagMode.NORMAL -> TextSoft
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Start / Stop Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onStartServices,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("start_booster_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!diagnostics.isVpnActive) AccentBlue else Color(0xFF1B3864)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Start Booster" else "Bắt Đầu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onStopServices,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("stop_booster_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRedDark
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Stop Service" else "Dừng Lại",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. Permission Status Card
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column {
                    Text(
                        text = if (isEn) "System Permissions" else "Quyền Hệ Thống",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Overlay Permission Row
                    PermissionRow(
                        title = if (isEn) "Display Over Other Apps (HUD)" else "Hiển Thị Trên Ứng Dụng Khác (HUD)",
                        isGranted = hasOverlayPerm,
                        onGrant = onRequestOverlay,
                        tag = "grant_overlay"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // VPN Permission Row
                    PermissionRow(
                        title = if (isEn) "VPN Connection (Traffic Routing)" else "Kết Nối Mạng VPN (Định Tuyến)",
                        isGranted = hasVpnPerm,
                        onGrant = onRequestVpn,
                        tag = "grant_vpn"
                    )
                }
            }
        }

        // 3. Simulated Lag Quick Triggers
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column {
                    Text(
                        text = if (isEn) "Quick Mode Override" else "Chế Độ Lag Nhanh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeChip(
                            title = if (isEn) "Freeze" else "Ngưng",
                            isSelected = diagnostics.currentMode == LagMode.FREEZE,
                            accentColor = FreezeBlue,
                            onClick = {
                                onToggleLagMode(
                                    if (diagnostics.currentMode == LagMode.FREEZE) LagMode.NORMAL else LagMode.FREEZE
                                )
                            },
                            modifier = Modifier.weight(1f),
                            tag = "mode_freeze"
                        )
                        ModeChip(
                            title = "Ghost",
                            isSelected = diagnostics.currentMode == LagMode.GHOST,
                            accentColor = GhostPurple,
                            onClick = {
                                onToggleLagMode(
                                    if (diagnostics.currentMode == LagMode.GHOST) LagMode.NORMAL else LagMode.GHOST
                                )
                            },
                            modifier = Modifier.weight(1f),
                            tag = "mode_ghost"
                        )
                        ModeChip(
                            title = "Teleport",
                            isSelected = diagnostics.currentMode == LagMode.TELEPORT,
                            accentColor = TeleCyan,
                            onClick = {
                                onToggleLagMode(
                                    if (diagnostics.currentMode == LagMode.TELEPORT) LagMode.NORMAL else LagMode.TELEPORT
                                )
                            },
                            modifier = Modifier.weight(1f),
                            tag = "mode_teleport"
                        )
                    }
                }
            }
        }

        // 4. Feature Switches
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEn) "Booster & Floating Controls" else "Tùy Chỉnh Nút & Tối Ưu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    ControlToggleRow(
                        title = if (isEn) "Floating Trigger Handle" else "Nút Cây Bút (Trigger Floating)",
                        description = if (isEn) "Show top HUD handle widget" else "Hiển thị thanh điều khiển mini",
                        icon = Icons.Default.Star,
                        iconColor = AccentBlue,
                        checked = config.switchPen,
                        onCheckedChange = { onUpdateConfig { c -> c.copy(switchPen = it) } },
                        tag = "toggle_pen"
                    )

                    ControlToggleRow(
                        title = if (isEn) "Freeze Button" else "Nút Ngưng Kết Nối",
                        description = if (isEn) "Floating lag trigger button" else "Nút ngưng mạng thả nổi",
                        icon = Icons.Default.Lock,
                        iconColor = FreezeBlue,
                        checked = config.switchFreeze,
                        onCheckedChange = { onUpdateConfig { c -> c.copy(switchFreeze = it) } },
                        tag = "toggle_freeze"
                    )

                    ControlToggleRow(
                        title = if (isEn) "Ghost Delay Button" else "Nút Ghost (Bóng Ma)",
                        description = if (isEn) "Jitter delayed sync packets" else "Gây trễ gói tin đồng bộ",
                        icon = Icons.Default.Favorite,
                        iconColor = GhostPurple,
                        checked = config.switchGhost,
                        onCheckedChange = { onUpdateConfig { c -> c.copy(switchGhost = it) } },
                        tag = "toggle_ghost"
                    )

                    ControlToggleRow(
                        title = if (isEn) "Teleport Jitter Button" else "Nút Teleport",
                        description = if (isEn) "Burst packet synchronization" else "Đột biến vị trí gói tin",
                        icon = Icons.Default.Send,
                        iconColor = TeleCyan,
                        checked = config.switchTele,
                        onCheckedChange = { onUpdateConfig { c -> c.copy(switchTele = it) } },
                        tag = "toggle_tele"
                    )

                    ControlToggleRow(
                        title = if (isEn) "VPN Download Acceleration" else "Tăng Tốc Tải Mạng VPN",
                        description = if (isEn) "Optimize MTU & TCP buffer pool" else "Tối ưu bộ đệm MTU và socket",
                        icon = Icons.Default.Share,
                        iconColor = SuccessGreen,
                        checked = config.switchDownloadBoost,
                        onCheckedChange = { onUpdateConfig { c -> c.copy(switchDownloadBoost = it) } },
                        tag = "toggle_download_boost"
                    )

                    ControlToggleRow(
                        title = if (isEn) "Stabilize FPS Drop" else "Giảm Drop FPS",
                        description = if (isEn) "Reduce background packet load" else "Giảm tải ngắt mạng gây giật khung hình",
                        icon = Icons.Default.Settings,
                        iconColor = GoldYellow,
                        checked = config.switchReduceFpsDrop,
                        onCheckedChange = { onUpdateConfig { c -> c.copy(switchReduceFpsDrop = it) } },
                        tag = "toggle_fps_drop"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onGrant: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C141F))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) SuccessGreen else DangerRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGranted) TextSoft else TextWhite,
                fontWeight = if (isGranted) FontWeight.Normal else FontWeight.Medium
            )
        }

        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onGrant,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlueDark),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag(tag)
            ) {
                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ModeChip(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF0C141F))
            .border(
                1.dp,
                if (isSelected) accentColor else StrokeDark,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accentColor else TextMuted
        )
    }
}
