package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque

@Composable
fun EditWeightGoalsDialog(
    initialStartKg: Float,
    initialTargetKg: Float,
    onSave: (startKg: Float, targetKg: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val initialStartFormatted = if (initialStartKg % 1f == 0f) "${initialStartKg.toInt()}" else "$initialStartKg"
    val initialTargetFormatted = if (initialTargetKg % 1f == 0f) "${initialTargetKg.toInt()}" else "$initialTargetKg"

    var startInput by remember { mutableStateOf(initialStartFormatted) }
    var targetInput by remember { mutableStateOf(initialTargetFormatted) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun sanitizeInput(raw: String): String {
        val normalized = raw.replace(',', '.')
        var hasDot = false
        return buildString {
            for (c in normalized) {
                if (c.isDigit()) {
                    append(c)
                } else if (c == '.' && !hasDot) {
                    hasDot = true
                    append(c)
                }
            }
        }.take(6)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .testTag("edit_weight_goals_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guildPlaque(20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                // Header with icon and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(UpperBodyAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = UpperBodyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        GuildTitle(
                            text = "TARGET BERAT BADAN",
                            fontSize = 16.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Field 1: Starting Weight
                Text(
                    text = "Starting Weight (kg)",
                    color = SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (startInput.isEmpty()) {
                        Text(
                            text = "Contoh: 38",
                            color = SecondaryText.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = startInput,
                        onValueChange = {
                            startInput = sanitizeInput(it)
                            errorText = null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(UpperBodyAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_weight_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Field 2: Target Weight
                Text(
                    text = "Target Weight (kg)",
                    color = SecondaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (targetInput.isEmpty()) {
                        Text(
                            text = "Contoh: 50",
                            color = SecondaryText.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = targetInput,
                        onValueChange = {
                            targetInput = sanitizeInput(it)
                            errorText = null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(UpperBodyAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_weight_input")
                    )
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorText ?: "",
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Cancel and Save buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, RestDayAccent),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("cancel_goals_button")
                    ) {
                        Text("Batal", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val startVal = startInput.toFloatOrNull()
                            val targetVal = targetInput.toFloatOrNull()

                            if (startVal == null || startVal !in 10f..500f) {
                                errorText = "Masukkan berat mulai yang valid (10 - 500 kg)"
                                return@Button
                            }
                            if (targetVal == null || targetVal !in 10f..500f) {
                                errorText = "Masukkan target berat yang valid (10 - 500 kg)"
                                return@Button
                            }

                            onSave(startVal, targetVal)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UpperBodyAccent,
                            contentColor = OnPrimaryDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_goals_button")
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
