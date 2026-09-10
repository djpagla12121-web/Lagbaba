package net.sath.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sath.model.LagConfig
import net.sath.ui.components.CustomSliderRow
import net.sath.ui.components.DropRangeDialog
import net.sath.ui.components.FreezeTimeDialog
import net.sath.ui.components.GlassCard
import net.sath.ui.theme.AccentBlue
import net.sath.ui.theme.FreezeBlue
import net.sath.ui.theme.GhostPurple
import net.sath.ui.theme.GoldYellow
import net.sath.ui.theme.PanelAlt
import net.sath.ui.theme.StrokeDark
import net.sath.ui.theme.SuccessGreen
import net.sath.ui.theme.TeleCyan
import net.sath.ui.theme.TextMuted
import net.sath.ui.theme.TextSoft
import net.sath.ui.theme.TextWhite

@Composable
fun SettingsScreen(
    config: LagConfig,
    onUpdateConfig: ((LagConfig) -> LagConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = config.language == "en"

    var showFreezeDropDialog by remember { mutableStateOf(false) }
    var showGhostDropDialog by remember { mutableStateOf(false) }
    var showFreezeTimeDialog by remember { mutableStateOf(false) }

    if (showFreezeDropDialog) {
        DropRangeDialog(
            title = if (isEn) "Freeze Delay Range" else "Khoảng Delay Ngưng",
            subtitle = if (isEn) "Synthetic latency drop interval" else "Khoảng giật lag khi kích hoạt nút Ngưng",
            initialMin = config.freezeDropMin,
            initialMax = config.freezeDropMax,
            accentColor = FreezeBlue,
            onDismiss = { showFreezeDropDialog = false },
            onSave = { min, max ->
                onUpdateConfig { it.copy(freezeDropMin = min, freezeDropMax = max) }
                showFreezeDropDialog = false
            }
        )
    }

    if (showGhostDropDialog) {
        DropRangeDialog(
            title = if (isEn) "Ghost Delay Range" else "Khoảng Delay Ghost",
            subtitle = if (isEn) "Subtle jitter desynchronization" else "Khoảng trễ bóng ma đồng bộ gói tin",
            initialMin = config.ghostDropMin,
            initialMax = config.ghostDropMax,
            accentColor = GhostPurple,
            onDismiss = { showGhostDropDialog = false },
            onSave = { min, max ->
                onUpdateConfig { it.copy(ghostDropMin = min, ghostDropMax = max) }
                showGhostDropDialog = false
            }
        )
    }

    if (showFreezeTimeDialog) {
        FreezeTimeDialog(
            initialSeconds = config.freezeDurationSeconds,
            onDismiss = { showFreezeTimeDialog = false },
            onSave = { sec ->
                onUpdateConfig { it.copy(freezeDurationSeconds = sec) }
                showFreezeTimeDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title header
        item {
            Column {
                Text(
                    text = if (isEn) "Preferences & HUD" else "Cài Đặt & Giao Diện HUD",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = if (isEn) "Configure overlay sizes, opacity and latency drop limits" else "Tùy chỉnh kích thước, độ mờ và khoảng độ trễ giả lập",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // 1. HUD Sizes
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEn) "HUD Button Sizes" else "Kích Thước Nút Nổi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    CustomSliderRow(
                        title = if (isEn) "Freeze Button Size" else "Kích thước Nút Ngưng",
                        value = config.sizeFreeze.toFloat(),
                        valueRange = 60f..250f,
                        unit = "dp",
                        accentColor = FreezeBlue,
                        onValueChange = { onUpdateConfig { c -> c.copy(sizeFreeze = it.toInt()) } },
                        tag = "size_freeze"
                    )

                    CustomSliderRow(
                        title = if (isEn) "Ghost Button Size" else "Kích thước Nút Ghost",
                        value = config.sizeGhost.toFloat(),
                        valueRange = 60f..250f,
                        unit = "dp",
                        accentColor = GhostPurple,
                        onValueChange = { onUpdateConfig { c -> c.copy(sizeGhost = it.toInt()) } },
                        tag = "size_ghost"
                    )

                    CustomSliderRow(
                        title = if (isEn) "Teleport Button Size" else "Kích thước Nút Tele",
                        value = config.sizeTele.toFloat(),
                        valueRange = 60f..250f,
                        unit = "dp",
                        accentColor = TeleCyan,
                        onValueChange = { onUpdateConfig { c -> c.copy(sizeTele = it.toInt()) } },
                        tag = "size_tele"
                    )
                }
            }
        }

        // 2. HUD Opacity
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEn) "HUD Button Transparency" else "Độ Trong Suốt Nút Nổi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    CustomSliderRow(
                        title = if (isEn) "Freeze Opacity" else "Độ mờ Nút Ngưng",
                        value = config.alphaFreeze.toFloat(),
                        valueRange = 10f..100f,
                        unit = "%",
                        accentColor = FreezeBlue,
                        onValueChange = { onUpdateConfig { c -> c.copy(alphaFreeze = it.toInt()) } },
                        tag = "alpha_freeze"
                    )

                    CustomSliderRow(
                        title = if (isEn) "Ghost Opacity" else "Độ mờ Nút Ghost",
                        value = config.alphaGhost.toFloat(),
                        valueRange = 10f..100f,
                        unit = "%",
                        accentColor = GhostPurple,
                        onValueChange = { onUpdateConfig { c -> c.copy(alphaGhost = it.toInt()) } },
                        tag = "alpha_ghost"
                    )

                    CustomSliderRow(
                        title = if (isEn) "Teleport Opacity" else "Độ mờ Nút Tele",
                        value = config.alphaTele.toFloat(),
                        valueRange = 10f..100f,
                        unit = "%",
                        accentColor = TeleCyan,
                        onValueChange = { onUpdateConfig { c -> c.copy(alphaTele = it.toInt()) } },
                        tag = "alpha_tele"
                    )
                }
            }
        }

        // 3. Network Latency Intervals
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEn) "Latency Parameters" else "Thông Số Trễ Mạng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    ConfigActionRow(
                        title = if (isEn) "Freeze Latency Range" else "Khoảng Delay Nút Ngưng",
                        valueText = "${config.freezeDropMin} - ${config.freezeDropMax} ms",
                        accentColor = FreezeBlue,
                        onClick = { showFreezeDropDialog = true },
                        tag = "btn_config_freeze_drop"
                    )

                    ConfigActionRow(
                        title = if (isEn) "Ghost Latency Range" else "Khoảng Delay Nút Ghost",
                        valueText = "${config.ghostDropMin} - ${config.ghostDropMax} ms",
                        accentColor = GhostPurple,
                        onClick = { showGhostDropDialog = true },
                        tag = "btn_config_ghost_drop"
                    )

                    ConfigActionRow(
                        title = if (isEn) "Freeze Safety Timeout" else "Thời Gian Ngưng Tối Đa",
                        valueText = "${config.freezeDurationSeconds}s",
                        accentColor = GoldYellow,
                        onClick = { showFreezeTimeDialog = true },
                        tag = "btn_config_freeze_time"
                    )
                }
            }
        }

        // 4. Language & App Info
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isEn) "Language & Region" else "Ngôn Ngữ & Ứng Dụng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageOption(
                            title = "🇻🇳 Tiếng Việt",
                            isSelected = config.language == "vi",
                            onClick = { onUpdateConfig { it.copy(language = "vi") } },
                            modifier = Modifier.weight(1f)
                        )
                        LanguageOption(
                            title = "🇬🇧 English",
                            isSelected = config.language == "en",
                            onClick = { onUpdateConfig { it.copy(language = "en") } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0C141F))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "License: UNLOCKED (VIP Pro Permanent)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Anti-detect v2.4 + Kernel IP-ID & Carrier TTL active",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigActionRow(
    title: String,
    valueText: String,
    accentColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C141F))
            .border(1.dp, StrokeDark.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextWhite,
            fontWeight = FontWeight.Medium
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentBlue.copy(alpha = 0.2f) else Color(0xFF0C141F))
            .border(
                1.dp,
                if (isSelected) AccentBlue else StrokeDark,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AccentBlue else TextSoft
        )
    }
}
