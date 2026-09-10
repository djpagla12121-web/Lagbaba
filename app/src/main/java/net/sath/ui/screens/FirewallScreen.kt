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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import net.sath.ui.components.AddFirewallRuleDialog
import net.sath.ui.components.GlassCard
import net.sath.ui.components.StatusBadge
import net.sath.ui.theme.AccentBlue
import net.sath.ui.theme.DangerRed
import net.sath.ui.theme.GoldYellow
import net.sath.ui.theme.PanelAlt
import net.sath.ui.theme.PanelDark
import net.sath.ui.theme.StrokeDark
import net.sath.ui.theme.StrokeSoft
import net.sath.ui.theme.SuccessGreen
import net.sath.ui.theme.TextMuted
import net.sath.ui.theme.TextSoft
import net.sath.ui.theme.TextWhite

@Composable
fun FirewallScreen(
    config: LagConfig,
    onAddIp: (String) -> Unit,
    onRemoveIp: (String) -> Unit,
    onAddDomain: (String) -> Unit,
    onRemoveDomain: (String) -> Unit,
    onToggleFirewall: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = config.language == "en"

    var showAddIpDialog by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }

    if (showAddIpDialog) {
        AddFirewallRuleDialog(
            isDomain = false,
            onDismiss = { showAddIpDialog = false },
            onAdd = { ip ->
                onAddIp(ip)
                showAddIpDialog = false
            }
        )
    }

    if (showAddDomainDialog) {
        AddFirewallRuleDialog(
            isDomain = true,
            onDismiss = { showAddDomainDialog = false },
            onAdd = { domain ->
                onAddDomain(domain)
                showAddDomainDialog = false
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
        // Header & Master Switch
        item {
            GlassCard(
                borderColor = if (config.isFirewallEnabled) SuccessGreen else StrokeDark,
                backgroundColor = PanelDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (config.isFirewallEnabled) SuccessGreen.copy(alpha = 0.15f) else Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (config.isFirewallEnabled) SuccessGreen else TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEn) "Game Firewall Protection" else "Tường Lửa Chống Do Thám",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = if (config.isFirewallEnabled)
                                    (if (isEn) "Blocking trackers & telemetry" else "Đang chặn máy chủ theo dõi")
                                else
                                    (if (isEn) "Firewall filter disabled" else "Tường lửa đã tắt"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = config.isFirewallEnabled,
                        onCheckedChange = onToggleFirewall,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SuccessGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("firewall_master_switch")
                    )
                }
            }
        }

        // 1. Blocked IP Addresses
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
                            Icon(Icons.Default.List, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Blocked IP Rules (${config.blockedIps.size})" else "Danh Sách IP Chặn (${config.blockedIps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Button(
                            onClick = { showAddIpDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_add_ip")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add IP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (config.blockedIps.isEmpty()) {
                        Text(
                            text = if (isEn) "No IP rules added yet." else "Chưa có địa chỉ IP nào trong danh sách chặn.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            config.blockedIps.forEach { ip ->
                                RuleItemRow(
                                    label = ip,
                                    typeLabel = "IP/CIDR",
                                    onDelete = { onRemoveIp(ip) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Blocked Domains
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
                            Icon(Icons.Default.Share, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Blocked Domains (${config.blockedDomains.size})" else "Tên Miền Chặn (${config.blockedDomains.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Button(
                            onClick = { showAddDomainDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldYellow),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_add_domain")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Host", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (config.blockedDomains.isEmpty()) {
                        Text(
                            text = if (isEn) "No domain rules added yet." else "Chưa có tên miền nào trong danh sách chặn.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            config.blockedDomains.forEach { domain ->
                                RuleItemRow(
                                    label = domain,
                                    typeLabel = "DNS HOST",
                                    onDelete = { onRemoveDomain(domain) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleItemRow(
    label: String,
    typeLabel: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C141F))
            .border(1.dp, StrokeDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = typeLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextWhite,
                maxLines = 1
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = DangerRed.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
