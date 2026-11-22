package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BackupData
import com.example.data.BackupManager
import com.example.data.model.BodyMeasurement
import com.example.data.model.GamificationEngine
import com.example.data.model.GamificationState
import com.example.data.model.WeightLog
import com.example.data.model.WeightTrendResult
import com.example.data.model.WorkoutSession
import com.example.ui.components.BodyMeasurementCard
import com.example.ui.components.EditWeightGoalsDialog
import com.example.ui.components.EditWorkoutSessionDialog
import com.example.ui.components.RankMedal
import com.example.ui.components.TierStarsRow
import com.example.ui.components.WeeklyTrendAnalysisBlock
import com.example.ui.components.WeightTrendChart
import com.example.ui.components.WorkoutHistoryCard
import com.example.ui.theme.BorderDivider
import com.example.ui.theme.CardBackground
import com.example.ui.theme.GoldDark
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ProgressXpBrush = Brush.horizontalGradient(listOf(GoldDark, UpperBodyAccent))
private val ProgressXpOrigin = TransformOrigin(0f, 0.5f)
private val ProgressXpBarShape = RoundedCornerShape(4.dp)
private val YmdFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@Composable
fun ProgressScreen(
    sessionsLast7Days: Int,
    latestWeight: WeightLog?,
    totalSessions: Int,
    allWeights: List<WeightLog>,
    allSessions: List<WorkoutSession>,
    allMeasurements: List<BodyMeasurement> = emptyList(),
    reminderHour: Int,
    reminderMinute: Int,
    isReminderEnabled: Boolean,
    onSaveWeight: (Float) -> Unit,
    onSaveMeasurement: (shoulderCm: Float, waistCm: Float) -> Unit = { _, _ -> },
    onDeleteSession: (Int) -> Unit,
    onUpdateSessionExercises: (sessionId: Int, exercisesJson: String) -> Unit = { _, _ -> },
    onOpenReminderSettings: () -> Unit,
    startWeightKg: Float = 38f,
    targetWeightKg: Float = 50f,
    weightTrendResult: WeightTrendResult = WeightTrendResult(),
    onUpdateWeightGoals: (startKg: Float, targetKg: Float) -> Unit = { _, _ -> },
    onOpenStats: () -> Unit = {},
    gamificationState: GamificationState = GamificationEngine.emptyState(),
    onOpenAdventure: () -> Unit = {},
    onExportBackup: suspend () -> String = { "" },
    onRestoreBackup: (BackupData) -> Unit = {},
    onShowSnackbar: (String) -> Unit = {},
    completedSetsBySessionId: Map<Int, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    var weightInput by remember { mutableStateOf("") }
    var shoulderInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var historyVisibleCount by remember { mutableStateOf(8) }
    var expandedSessionIds by remember { mutableStateOf(setOf<Int>()) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreData by remember { mutableStateOf<BackupData?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var sessionToDeleteId by remember { mutableStateOf<Int?>(null) }
    var sessionToEdit by remember { mutableStateOf<WorkoutSession?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = pendingExportJson
            if (json != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(json.toByteArray(Charsets.UTF_8))
                        }
                        withContext(Dispatchers.Main) {
                            onShowSnackbar("Backup berhasil disimpan")
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            onShowSnackbar("Gagal menyimpan backup")
                        }
                    } finally {
                        pendingExportJson = null
                    }
                }
            }
        } else {
            pendingExportJson = null
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                    if (content != null) {
                        val parsed = BackupManager.importFromJson(content)
                        withContext(Dispatchers.Main) {
                            if (parsed != null) {
                                pendingRestoreData = parsed
                                showRestoreConfirmDialog = true
                            } else {
                                onShowSnackbar("File backup tidak valid")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onShowSnackbar("File backup tidak valid")
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        onShowSnackbar("File backup tidak valid")
                    }
                }
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kartu Guild compact card at the top of Progress tab
        item(key = "petualangan_card", contentType = "guild_summary_card") {
            val rankInfo = gamificationState.rankInfo
            val rankLabelText = if (rankInfo.isEndless) {
                "${rankInfo.rankLetter} · ${rankInfo.displayLabel}"
            } else {
                "${rankInfo.displayLabel} · ${rankInfo.rankTitle}"
            }
            val xpFraction = if (rankInfo.xpNeededForNextTier <= 0) {
                1f
            } else {
                (rankInfo.xpIntoTier.toFloat() / rankInfo.xpNeededForNextTier.toFloat())
                    .coerceIn(0f, 1f)
            }
            val animatedXpFraction by animateFloatAsState(
                targetValue = xpFraction,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "progress_tab_xp_bar"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .guildPlaque(16.dp)
                    .clickable { onOpenAdventure() }
                    .padding(14.dp)
                    .testTag("petualangan_open_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RankMedal(
                            rankIndex = rankInfo.rankIndex,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            GuildTitle(
                                text = "KARTU GUILD",
                                color = SecondaryText,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rankLabelText,
                                color = ParchmentLight,
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TierStarsRow(
                                filledCount = rankInfo.tierIndex + 1,
                                starSize = 12.dp,
                                spacing = 2.dp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Detail",
                            color = UpperBodyAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Buka Kartu Guild",
                            tint = UpperBodyAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(ProgressXpBarShape)
                        .background(MainBackground)
                        .border(1.dp, BorderDivider, ProgressXpBarShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = animatedXpFraction
                                transformOrigin = ProgressXpOrigin
                            }
                            .background(ProgressXpBrush)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${rankInfo.xpIntoTier} / ${rankInfo.xpNeededForNextTier} XP ke ${rankInfo.nextLabel}",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3 stat cards in a horizontal row at the top
        item(key = "top_stat_cards", contentType = "stat_cards_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            // 1. SESSIONS (7D)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .guildPlaque(16.dp)
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SESSIONS (7D)",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$sessionsLast7Days",
                    color = UpperBodyAccent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // 2. LATEST WEIGHT (format "XXkg", show "-" if no data yet)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .guildPlaque(16.dp)
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LATEST WEIGHT",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val weightDisplay = if (latestWeight != null) {
                    val kgStr = if (latestWeight.kg % 1f == 0f) {
                        "${latestWeight.kg.toInt()}kg"
                    } else {
                        "${latestWeight.kg}kg"
                    }
                    kgStr
                } else {
                    "-"
                }
                Text(
                    text = weightDisplay,
                    color = PrimaryText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // 3. TOTAL SESSIONS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .guildPlaque(16.dp)
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL SESSIONS",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$totalSessions",
                    color = LowerBodyAccent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
            }
        }

        // Workout Reminder Quick Card
        item(key = "workout_reminder_card", contentType = "quick_action_card") {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .clickable { onOpenReminderSettings() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pengingat Latihan",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val timeFormatted = String.format("%02d:%02d", reminderHour, reminderMinute)
                    Text(
                        text = if (isReminderEnabled) "Aktif jam $timeFormatted (Sen, Sel, Kam, Jum)" else "Nonaktif",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SecondaryCardBackground)
                    .border(1.dp, BorderDivider, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Ubah",
                    color = UpperBodyAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            }
        }

        // Volume Lifted Trend (30 Days) Quick Banner
        item(key = "volume_stats_banner", contentType = "quick_action_card") {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .clickable { onOpenStats() }
                .padding(14.dp)
                .testTag("stats_open_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LowerBodyAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = LowerBodyAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Volume Angkatan (30 Hari)",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lihat grafik tren volume per sesi",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Grafik",
                    color = LowerBodyAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = LowerBodyAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            }
        }

        // "Body Weight" card
        item(key = "body_weight_card", contentType = "body_weight_card") {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .padding(16.dp)
                .testTag("body_weight_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    GuildTitle(
                        text = "BODY WEIGHT",
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target ${formatWeight(targetWeightKg)}kg • started at ${formatWeight(startWeightKg)}kg",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    // Weigh-in status line based on latest WeightLog timestamp
                    val (weighInStatusText, weighInStatusColor) = remember(latestWeight) {
                        if (latestWeight == null) {
                            "Belum ada data berat badan" to SecondaryText
                        } else {
                            val daysAgo = try {
                                val logDate = LocalDate.parse(latestWeight.date, YmdFormatter)
                                java.time.temporal.ChronoUnit.DAYS.between(logDate, LocalDate.now()).toInt().coerceAtLeast(0)
                            } catch (_: Exception) {
                                val diffMillis = System.currentTimeMillis() - latestWeight.timestamp
                                (diffMillis / (1000L * 60 * 60 * 24)).coerceAtLeast(0).toInt()
                            }
                            when {
                                daysAgo == 0 -> "Terakhir ditimbang hari ini" to SecondaryText
                                daysAgo <= 6 -> "Terakhir ditimbang $daysAgo hari lalu" to SecondaryText
                                else -> "Sudah $daysAgo hari belum ditimbang — saatnya timbang" to UpperBodyAccent
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weighInStatusText,
                        color = weighInStatusColor,
                        fontSize = 12.sp,
                        fontWeight = if (weighInStatusColor == UpperBodyAccent) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                IconButton(
                    onClick = { showEditGoalsDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("edit_weight_goals_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Target",
                        tint = UpperBodyAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact muted-text note explaining recommended weigh-in routine
            Text(
                text = "Timbang 1x seminggu, hari yang sama, pagi setelah bangun & buang air, sebelum makan/minum, pakaian sama.",
                color = SecondaryText.copy(alpha = 0.8f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Decimal number input + "Save" button to add a new weight entry (auto-uses today's date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SecondaryCardBackground)
                        .border(1.dp, BorderDivider, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (weightInput.isEmpty()) {
                        Text(
                            text = "Berat badan hari ini (kg)...",
                            color = SecondaryText.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = weightInput,
                        onValueChange = { raw ->
                            val normalized = raw.replace(',', '.')
                            var hasDot = false
                            val clean = buildString {
                                for (c in normalized) {
                                    if (c.isDigit()) {
                                        append(c)
                                    } else if (c == '.' && !hasDot) {
                                        hasDot = true
                                        append(c)
                                    }
                                }
                            }.take(6)
                            weightInput = clean
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
                            .testTag("weight_input_field")
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        val parsed = weightInput.toFloatOrNull()
                        if (parsed != null && parsed in 10f..500f) {
                            onSaveWeight(parsed)
                            weightInput = ""
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UpperBodyAccent,
                        contentColor = OnPrimaryDark
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("save_weight_button")
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // A simple line chart showing body weight trend
            WeightTrendChart(
                weights = allWeights,
                targetKg = targetWeightKg,
                startKg = startWeightKg
            )

            Spacer(modifier = Modifier.height(14.dp))

            // "Analisis Mingguan" block under the weight chart
            WeeklyTrendAnalysisBlock(weightTrendResult = weightTrendResult)
        }
        }

        // "Ukuran Tubuh" card for V-taper tracking
        item(key = "body_measurement_card", contentType = "body_measurement_card") {
            BodyMeasurementCard(
                allMeasurements = allMeasurements,
                onSaveMeasurement = onSaveMeasurement,
                onShowSnackbar = onShowSnackbar,
                shoulderInputState = shoulderInput,
                onShoulderInputChange = { shoulderInput = it },
                waistInputState = waistInput,
                onWaistInputChange = { waistInput = it }
            )
        }

        // "Workout History" card
        item(key = "workout_history_card", contentType = "workout_history_card") {
            WorkoutHistoryCard(
                allSessions = allSessions,
                onEditSession = { sessionToEdit = it },
                onDeleteSession = { sessionToDeleteId = it },
                visibleCountState = historyVisibleCount,
                onVisibleCountChange = { historyVisibleCount = it },
                expandedSessionIdsState = expandedSessionIds,
                onExpandedSessionIdsChange = { expandedSessionIds = it },
                completedSetsBySessionId = completedSetsBySessionId
            )
        }

        // Backup & Restore Section
        item(key = "backup_restore_card", contentType = "backup_restore_card") {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .guildPlaque(16.dp)
                .padding(16.dp)
                .testTag("backup_restore_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(UpperBodyAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Cadangkan Data",
                        tint = UpperBodyAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    GuildTitle(
                        text = "CADANGKAN & PULIHKAN DATA",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simpan atau pulihkan riwayat latihan & berat badan",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val json = onExportBackup()
                            pendingExportJson = json
                            val todayStr = LocalDate.now().format(YmdFormatter)
                            createDocumentLauncher.launch("vtaper_grind_backup_$todayStr.json")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("export_backup_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryCardBackground
                    ),
                    border = BorderStroke(1.5.dp, UpperBodyAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Export Data",
                        tint = UpperBodyAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Export Data",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("import_backup_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryCardBackground
                    ),
                    border = BorderStroke(1.5.dp, LowerBodyAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Import Data",
                        tint = LowerBodyAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import Data",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            }
        }
    }

    if (showRestoreConfirmDialog && pendingRestoreData != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreData = null
            },
            title = {
                GuildTitle(
                    text = "PULIHKAN DATA",
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Ini akan mengganti semua data latihan dan berat badan yang ada saat ini dengan data dari file backup. Lanjutkan?",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val data = pendingRestoreData
                        showRestoreConfirmDialog = false
                        pendingRestoreData = null
                        if (data != null) {
                            onRestoreBackup(data)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UpperBodyAccent,
                        contentColor = OnPrimaryDark
                    ),
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text(
                        text = "Lanjutkan",
                        color = OnPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreData = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryCardBackground
                    ),
                    border = BorderStroke(1.5.dp, RestDayAccent),
                    modifier = Modifier.testTag("cancel_restore_button")
                ) {
                    Text(
                        text = "Batal",
                        color = PrimaryText
                    )
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (sessionToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { sessionToDeleteId = null },
            title = {
                GuildTitle(
                    text = "HAPUS SESI LATIHAN",
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Hapus sesi ini? Riwayat dan saran progresif overload akan ikut berubah.",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = sessionToDeleteId
                        sessionToDeleteId = null
                        if (id != null) {
                            onDeleteSession(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF453A)
                    ),
                    modifier = Modifier.testTag("confirm_delete_session_button")
                ) {
                    Text(
                        text = "Hapus",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { sessionToDeleteId = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryCardBackground
                    ),
                    border = BorderStroke(1.5.dp, RestDayAccent),
                    modifier = Modifier.testTag("cancel_delete_session_button")
                ) {
                    Text(
                        text = "Batal",
                        color = PrimaryText
                    )
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showEditGoalsDialog) {
        EditWeightGoalsDialog(
            initialStartKg = startWeightKg,
            initialTargetKg = targetWeightKg,
            onSave = onUpdateWeightGoals,
            onDismiss = { showEditGoalsDialog = false }
        )
    }

    val editingSession = sessionToEdit
    if (editingSession != null) {
        EditWorkoutSessionDialog(
            session = editingSession,
            onSave = { updatedJson ->
                onUpdateSessionExercises(editingSession.id, updatedJson)
                sessionToEdit = null
            },
            onDismiss = { sessionToEdit = null }
        )
    }
}

private fun formatWeight(kg: Float): String {
    return if (kg % 1f == 0f) kg.toInt().toString() else kg.toString()
}
