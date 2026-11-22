package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.reminder.ReminderManager
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.GuildTitle
import com.example.ui.theme.LowerBodyAccent
import com.example.ui.theme.MainBackground
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.ParchmentLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.RestDayAccent
import com.example.ui.theme.SecondaryCardBackground
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.theme.guildPlaque
import java.time.DayOfWeek

private val WEIGH_IN_DAYS = listOf(
    DayOfWeek.MONDAY to "Sen",
    DayOfWeek.TUESDAY to "Sel",
    DayOfWeek.WEDNESDAY to "Rab",
    DayOfWeek.THURSDAY to "Kam",
    DayOfWeek.FRIDAY to "Jum",
    DayOfWeek.SATURDAY to "Sab",
    DayOfWeek.SUNDAY to "Min"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    initialHour: Int,
    initialMinute: Int,
    isReminderEnabled: Boolean,
    weighInEnabled: Boolean = true,
    weighInDay: DayOfWeek = DayOfWeek.SUNDAY,
    weighInHour: Int = 7,
    weighInMinute: Int = 0,
    onSaveTime: (hour: Int, minute: Int) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onToggleWeighInReminder: (Boolean) -> Unit = {},
    onWeighInDaySelected: (DayOfWeek) -> Unit = {},
    onSaveWeighInSettings: (enabled: Boolean, day: DayOfWeek, hour: Int, minute: Int) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reminderEnabled by remember { mutableStateOf(isReminderEnabled) }
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    var isWeighInEnabled by remember { mutableStateOf(weighInEnabled) }
    var selectedWeighInDay by remember { mutableStateOf(weighInDay) }
    val weighInTimePickerState = rememberTimePickerState(
        initialHour = weighInHour,
        initialMinute = weighInMinute,
        is24Hour = true
    )

    var canExactAlarm by remember {
        mutableStateOf(ReminderManager.canScheduleExactAlarms(context))
    }
    var isBatteryExempt by remember {
        mutableStateOf(ReminderManager.isIgnoringBatteryOptimizations(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canExactAlarm = ReminderManager.canScheduleExactAlarms(context)
                isBatteryExempt = ReminderManager.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxDialogHeight = maxHeight * 0.9f
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight)
                    .testTag("reminder_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .guildPlaque(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = UpperBodyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        GuildTitle(
                            text = "PENGINGAT LATIHAN",
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle reminder row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aktifkan Pengingat",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hanya Senin, Selasa, Kamis, & Jumat",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = {
                            reminderEnabled = it
                            onToggleReminder(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimaryDark,
                            checkedTrackColor = UpperBodyAccent,
                            uncheckedThumbColor = SecondaryText,
                            uncheckedTrackColor = MainBackground
                        ),
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }

                if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFA500))
                            .border(1.dp, Color(0x55FFA500), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .testTag("exact_alarm_warning_banner")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFA500),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Izin Alarm Presisi Diperlukan",
                                    color = PrimaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Agar alarm berbunyi tepat waktu saat HP idle.",
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        // Ignore if activity cannot be resolved on some devices
                                    }
                                },
                                border = BorderStroke(1.5.dp, Color(0xFFFFA500)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFA500)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Izinkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (!isBatteryExempt && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ParchmentLight.copy(alpha = 0.133f))
                            .border(1.dp, ParchmentLight.copy(alpha = 0.333f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryAlert,
                                contentDescription = null,
                                tint = ParchmentLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Abaikan Optimasi Baterai",
                                    color = PrimaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Cegah HP (Xiaomi, Samsung, dll) mematikan alarm saat HP sleep.",
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    ReminderManager.requestIgnoreBatteryOptimizations(context)
                                },
                                border = BorderStroke(1.5.dp, ParchmentLight),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ParchmentLight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Izinkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Workout Material 3 TimePicker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SecondaryCardBackground)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.align(Alignment.Center),
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MainBackground,
                            selectorColor = UpperBodyAccent,
                            containerColor = SecondaryCardBackground,
                            periodSelectorBorderColor = BorderDivider,
                            clockDialSelectedContentColor = OnPrimaryDark,
                            clockDialUnselectedContentColor = SecondaryText,
                            timeSelectorSelectedContainerColor = UpperBodyAccent,
                            timeSelectorUnselectedContainerColor = MainBackground,
                            timeSelectorSelectedContentColor = OnPrimaryDark,
                            timeSelectorUnselectedContentColor = PrimaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                HorizontalDivider(color = BorderDivider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // =========================================================
                // SECOND SECTION: Pengingat Timbang Mingguan
                // =========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LowerBodyAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = LowerBodyAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    GuildTitle(
                        text = "PENGINGAT TIMBANG MINGGUAN",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle weigh-in reminder row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aktifkan Pengingat Timbang",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "1x seminggu sebelum makan pagi",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isWeighInEnabled,
                        onCheckedChange = {
                            isWeighInEnabled = it
                            onToggleWeighInReminder(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnPrimaryDark,
                            checkedTrackColor = LowerBodyAccent,
                            uncheckedThumbColor = SecondaryText,
                            uncheckedTrackColor = MainBackground
                        ),
                        modifier = Modifier.testTag("weigh_in_reminder_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Day-of-week selector (chips Sen, Sel, Rab, Kam, Jum, Sab, Min)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Hari Timbang Badan",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((day, label) in WEIGH_IN_DAYS) {
                            val isSelected = selectedWeighInDay == day
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) LowerBodyAccent else MainBackground
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) LowerBodyAccent else BorderDivider,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedWeighInDay = day
                                        onWeighInDaySelected(day)
                                    }
                                    .testTag("weigh_in_day_chip_${label.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) OnPrimaryDark else SecondaryText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Weigh-In Material 3 TimePicker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SecondaryCardBackground)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = weighInTimePickerState,
                        modifier = Modifier.align(Alignment.Center),
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MainBackground,
                            selectorColor = LowerBodyAccent,
                            containerColor = SecondaryCardBackground,
                            periodSelectorBorderColor = BorderDivider,
                            clockDialSelectedContentColor = OnPrimaryDark,
                            clockDialUnselectedContentColor = SecondaryText,
                            timeSelectorSelectedContainerColor = LowerBodyAccent,
                            timeSelectorUnselectedContainerColor = MainBackground,
                            timeSelectorSelectedContentColor = OnPrimaryDark,
                            timeSelectorUnselectedContentColor = PrimaryText
                        )
                    )
                }

                }

                    HorizontalDivider(color = BorderDivider, thickness = 1.dp)

                    // Pinned action buttons outside the scrolling area with >= 24.dp bottom space
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.5.dp, RestDayAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Batal",
                                color = PrimaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                onSaveTime(timePickerState.hour, timePickerState.minute)
                                onSaveWeighInSettings(
                                    isWeighInEnabled,
                                    selectedWeighInDay,
                                    weighInTimePickerState.hour,
                                    weighInTimePickerState.minute
                                )
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = UpperBodyAccent,
                                contentColor = OnPrimaryDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_reminder_time_button")
                        ) {
                            Text(
                                text = "Simpan",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
