package net.sath.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sath.model.DiagnosticsState
import net.sath.model.LagConfig
import net.sath.model.ServerPingTarget
import net.sath.ui.components.GlassCard
import net.sath.ui.theme.AccentBlue
import net.sath.ui.theme.DangerRed
import net.sath.ui.theme.GoldYellow
import net.sath.ui.theme.PanelAlt
import net.sath.ui.theme.StrokeDark
import net.sath.ui.theme.SuccessGreen
import net.sath.ui.theme.TeleCyan
import net.sath.ui.theme.TextMuted
import net.sath.ui.theme.TextSoft
import net.sath.ui.theme.TextWhite

@Composable
fun DiagnosticsScreen(
    config: LagConfig,
    diagnostics: DiagnosticsState,
    serverTargets: List<ServerPingTarget>,
    onRunPingTest: () -> Unit,
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
        // Header
        item {
            Column {
                Text(
                    text = if (isEn) "Game Server Diagnostics" else "Chẩn Đoán Mạng Máy Chủ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = if (isEn) "Real-time latency, jitter analysis & packet metrics" else "Đo độ trễ thực tế, độ biến thiên và thống kê gói tin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Live Traffic Metrics Card
        item {
            GlassCard(
                borderColor = AccentBlue,
                backgroundColor = PanelAlt
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Live Traffic Analysis" else "Phân Tích Lưu Lượng Trực Tiếp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBox(
                            title = if (isEn) "PACKETS PROCESSED" else "GÓI TIN XỬ LÝ",
                            value = "${diagnostics.packetsSent}",
                            color = AccentBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = if (isEn) "DELAY INJECTED" else "GÓI TIN GÂY TRỄ",
                            value = "${diagnostics.packetsDelayed}",
                            color = TeleCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBox(
                            title = if (isEn) "AVG LATENCY" else "ĐỘ TRỄ TRUNG BÌNH",
                            value = "${diagnostics.currentPingMs} ms",
                            color = if (diagnostics.currentPingMs < 100) SuccessGreen else DangerRed,
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = if (isEn) "PACKET JITTER" else "ĐỘ DAO ĐỘNG (JITTER)",
                            value = "±${diagnostics.jitterMs} ms",
                            color = GoldYellow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Multi-Region Gaming Cluster Ping
        item {
            GlassCard(
                borderColor = StrokeDark,
                backgroundColor = PanelAlt
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = TeleCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Global Gaming Datacenters" else "Máy Chủ Trò Chơi Quốc Tế",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Button(
                            onClick = onRunPingTest,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_run_ping_test")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ping All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        serverTargets.forEach { server ->
                            ServerPingRow(server = server)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C141F))
            .border(1.dp, StrokeDark.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun ServerPingRow(
    server: ServerPingTarget
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C141F))
            .border(1.dp, StrokeDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = server.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = "${server.region} • ${server.host}",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        if (server.isTesting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = AccentBlue,
                strokeWidth = 2.dp
            )
        } else if (server.pingMs > 0) {
            val pingColor = when {
                server.pingMs < 60 -> SuccessGreen
                server.pingMs < 150 -> GoldYellow
                else -> DangerRed
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(pingColor.copy(alpha = 0.15f))
                    .border(1.dp, pingColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${server.pingMs} ms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pingColor
                )
            }
        } else {
            Text(
                text = "-- ms",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }
    }
}
