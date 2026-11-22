package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyMeasurement
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque
import java.util.Locale

@Composable
fun BodyMeasurementCard(
    allMeasurements: List<BodyMeasurement>,
    onSaveMeasurement: (shoulderCm: Float, waistCm: Float) -> Unit,
    onShowSnackbar: (String) -> Unit,
    shoulderInputState: String? = null,
    onShoulderInputChange: ((String) -> Unit)? = null,
    waistInputState: String? = null,
    onWaistInputChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var localShoulderInput by remember { mutableStateOf("") }
    var localWaistInput by remember { mutableStateOf("") }

    val shoulderInput = shoulderInputState ?: localShoulderInput
    val setShoulderInput: (String) -> Unit = onShoulderInputChange ?: { localShoulderInput = it }
    val waistInput = waistInputState ?: localWaistInput
    val setWaistInput: (String) -> Unit = onWaistInputChange ?: { localWaistInput = it }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .guildPlaque(16.dp)
            .padding(16.dp)
            .testTag("body_measurement_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuildTitle(
                text = "UKURAN TUBUH",
                fontSize = 17.sp
            )

            if (allMeasurements.isNotEmpty()) {
                val latestM = allMeasurements.last()
                val ratio = if (latestM.waistCm > 0f) latestM.shoulderCm / latestM.waistCm else 0f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(UpperBodyAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Rasio: ${String.format(Locale.US, "%.2f", ratio)}",
                        color = UpperBodyAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Guidance note
        Text(
            text = "Ukur 1x sebulan, kondisi dan posisi sama. Semakin besar rasio, semakin V-taper.",
            color = SecondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Latest stats & change since first entry
        if (allMeasurements.isNotEmpty()) {
            val latestM = allMeasurements.last()
            val firstM = allMeasurements.first()
            val latestRatio = if (latestM.waistCm > 0f) latestM.shoulderCm / latestM.waistCm else 0f
            val firstRatio = if (firstM.waistCm > 0f) firstM.shoulderCm / firstM.waistCm else 0f
            val ratioDiff = latestRatio - firstRatio

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SecondaryCardBackground)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Bahu Terakhir", color = SecondaryText, fontSize = 11.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", latestM.shoulderCm)} cm",
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(text = "Pinggang Terakhir", color = SecondaryText, fontSize = 11.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", latestM.waistCm)} cm",
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Rasio V-Taper", color = SecondaryText, fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.US, "%.2f", latestRatio),
                            color = UpperBodyAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (allMeasurements.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val sign = if (ratioDiff >= 0f) "+" else ""
                    val changeColor = if (ratioDiff >= 0f) UpperBodyAccent else LowerBodyAccent
                    Text(
                        text = "Perubahan sejak awal (${firstM.date}): $sign${String.format(Locale.US, "%.2f", ratioDiff)} rasio (Bahu: ${if (latestM.shoulderCm - firstM.shoulderCm >= 0) "+" else ""}${String.format(Locale.US, "%.1f", latestM.shoulderCm - firstM.shoulderCm)}cm, Pinggang: ${if (latestM.waistCm - firstM.waistCm >= 0) "+" else ""}${String.format(Locale.US, "%.1f", latestM.waistCm - firstM.waistCm)}cm)",
                        color = changeColor,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Numeric inputs: Bahu cm & Pinggang cm + Save button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shoulder input
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bahu (cm)",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (shoulderInput.isEmpty()) {
                        Text(
                            text = "Contoh: 110",
                            color = SecondaryText.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = shoulderInput,
                        onValueChange = { setShoulderInput(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(UpperBodyAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shoulder_cm_input")
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Waist input
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pinggang (cm)",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (waistInput.isEmpty()) {
                        Text(
                            text = "Contoh: 75",
                            color = SecondaryText.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = waistInput,
                        onValueChange = { setWaistInput(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(UpperBodyAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("waist_cm_input")
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = " ",
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val s = shoulderInput.toFloatOrNull()
                        val w = waistInput.toFloatOrNull()
                        if (s != null && w != null && s in 30f..250f && w in 30f..250f) {
                            onSaveMeasurement(s, w)
                            setShoulderInput("")
                            setWaistInput("")
                        } else {
                            onShowSnackbar("Masukkan nilai bahu dan pinggang yang valid (30 - 250 cm)")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UpperBodyAccent,
                        contentColor = OnPrimaryDark
                    ),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("save_measurement_button")
                ) {
                    Text(
                        text = "Simpan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
