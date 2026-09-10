package net.sath.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.window.Dialog
import net.sath.ui.theme.AccentBlue
import net.sath.ui.theme.DangerRed
import net.sath.ui.theme.FreezeBlue
import net.sath.ui.theme.GhostPurple
import net.sath.ui.theme.PanelAlt
import net.sath.ui.theme.PanelDark
import net.sath.ui.theme.StrokeDark
import net.sath.ui.theme.TextMuted
import net.sath.ui.theme.TextSoft
import net.sath.ui.theme.TextWhite

@Composable
fun DropRangeDialog(
    title: String,
    subtitle: String,
    initialMin: Int,
    initialMax: Int,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (min: Int, max: Int) -> Unit
) {
    var minVal by remember { mutableFloatStateOf(initialMin.toFloat()) }
    var maxVal by remember { mutableFloatStateOf(initialMax.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = PanelDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Min delay slider
                Text(
                    text = "Min Latency Drop: ${minVal.toInt()} ms",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSoft
                )
                Slider(
                    value = minVal,
                    onValueChange = {
                        minVal = it
                        if (minVal > maxVal) maxVal = minVal
                    },
                    valueRange = 20f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Max delay slider
                Text(
                    text = "Max Latency Drop: ${maxVal.toInt()} ms",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSoft
                )
                Slider(
                    value = maxVal,
                    onValueChange = {
                        maxVal = it
                        if (maxVal < minVal) minVal = maxVal
                    },
                    valueRange = 50f..2000f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onSave(minVal.toInt(), maxVal.toInt()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.testTag("save_drop_range_btn")
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FreezeTimeDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var seconds by remember { mutableFloatStateOf(initialSeconds.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, FreezeBlue.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = PanelDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Freeze Max Duration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Auto-release network freeze protection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Duration: ${seconds.toInt()} seconds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FreezeBlue
                )
                Slider(
                    value = seconds,
                    onValueChange = { seconds = it },
                    valueRange = 1f..30f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = FreezeBlue,
                        activeTrackColor = FreezeBlue,
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onSave(seconds.toInt()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FreezeBlue),
                        modifier = Modifier.testTag("save_freeze_time_btn")
                    ) {
                        Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFirewallRuleDialog(
    isDomain: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = PanelDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isDomain) "Block Domain" else "Block IP Address",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = if (isDomain) "Enter hostname (e.g. telemetry.game.com)" else "Enter IP/CIDR (e.g. 192.168.1.1 or 10.0.0.0/24)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        errorMsg = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firewall_rule_input"),
                    placeholder = {
                        Text(if (isDomain) "ads.tracker.com" else "103.249.28.1")
                    },
                    isError = errorMsg != null,
                    supportingText = {
                        errorMsg?.let { Text(it, color = DangerRed) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = StrokeDark,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val trimmed = textValue.trim()
                            if (trimmed.isEmpty()) {
                                errorMsg = "Value cannot be empty"
                            } else {
                                onAdd(trimmed)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        modifier = Modifier.testTag("add_rule_confirm_btn")
                    ) {
                        Text("Add Rule", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
