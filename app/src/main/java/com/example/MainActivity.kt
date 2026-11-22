package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminder.ReminderManager
import com.example.ui.components.BottomNavBar
import com.example.ui.components.RankMedalBitmapCache
import com.example.ui.components.ReminderDialog
import com.example.ui.components.TopHeader
import com.example.ui.components.WorkoutXpRewardDialog
import com.example.ui.screens.AdventureScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.CardBackground
import com.example.ui.theme.MainBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.UpperBodyAccent
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class MainRoute(val order: Int, val isSubScreen: Boolean = false) {
    data object Today : MainRoute(order = 0)
    data object Schedule : MainRoute(order = 1)
    data object Progress : MainRoute(order = 2)
    data object Stats : MainRoute(order = 3, isSubScreen = true)
    data object Adventure : MainRoute(order = 3, isSubScreen = true)
}

private val SnackbarShape = RoundedCornerShape(10.dp)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        ReminderManager.createNotificationChannel(this)
        ReminderManager.createWeighInNotificationChannel(this)

        // Schedule initial default alarms if not already scheduled
        if (ReminderManager.isReminderEnabled(this)) {
            val (h, m) = ReminderManager.getReminderTime(this)
            ReminderManager.scheduleAllAlarms(this, h, m)
        }
        if (ReminderManager.isWeighInReminderEnabled(this)) {
            val day = ReminderManager.getWeighInReminderDay(this)
            val (wh, wm) = ReminderManager.getWeighInReminderTime(this)
            ReminderManager.scheduleWeighInAlarm(this, day, wh, wm)
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val dayInfo by viewModel.dayInfo.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val workoutXpReward by viewModel.workoutXpReward.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val saveableStateHolder = rememberSaveableStateHolder()
    var showReminderDialog by remember { mutableStateOf(false) }
    var showStatsScreen by remember { mutableStateOf(false) }
    var showAdventureScreen by remember { mutableStateOf(false) }

    // Staged background warm-up state so Frame 1 renders Today immediately,
    // then Schedule, Progress, Stats, Adventure, and WebP medal bitmaps warm off-screen.
    var primaryTabsWarmed by remember { mutableStateOf(false) }
    var subScreensWarmed by remember { mutableStateOf(false) }
    var warmupDrawPass by remember { mutableStateOf(false) }

    val onOpenReminderSettings = remember { { showReminderDialog = true } }
    val onOpenStats = remember { { showStatsScreen = true } }
    val onCloseStats = remember { { showStatsScreen = false } }
    val onOpenAdventure = remember { { showAdventureScreen = true } }
    val onCloseAdventure = remember { { showAdventureScreen = false } }
    val onTabSelected = remember(viewModel) {
        { tab: Int ->
            showStatsScreen = false
            showAdventureScreen = false
            viewModel.selectTab(tab)
        }
    }

    // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Post-first-frame staged warmup: decode 9 medals on Dispatchers.IO, then warm primary tabs and sub-screens
    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(50L)
        launch {
            val rankIndex = viewModel.gamificationState.value.rankInfo.rankIndex
            RankMedalBitmapCache.prewarmAll(
                resources = context.resources,
                density = density,
                currentRankIndex = rankIndex
            )
        }
        warmupDrawPass = true
        primaryTabsWarmed = true
        withFrameNanos { }
        delay(45L)
        subScreensWarmed = true
        withFrameNanos { }
        delay(45L)
        warmupDrawPass = false
    }

    // Collect Snackbar events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Auto-detect midnight rollover and sync timers on app resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-detect midnight rollover while app is left open in foreground
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            viewModel.checkDateRollover()
        }
    }

    val currentRoute: MainRoute = when (selectedTab) {
        0 -> MainRoute.Today
        1 -> MainRoute.Schedule
        else -> when {
            showStatsScreen -> MainRoute.Stats
            showAdventureScreen -> MainRoute.Adventure
            else -> MainRoute.Progress
        }
    }

    var animatingToRoute by remember { mutableStateOf<MainRoute>(currentRoute) }
    var exitingRoute by remember { mutableStateOf<MainRoute?>(null) }
    var transitionForward by remember { mutableStateOf(true) }
    val transitionProgress = remember { Animatable(1f) }
    var viewportWidthPx by remember { mutableIntStateOf(0) }

    // Synchronously bridge Frame 0 between StateFlow update and LaunchedEffect start:
    // without this, Frame 0 reads progress == 1f and flashes the new screen before snapTo(0f) runs.
    val isTransitionPending = currentRoute != animatingToRoute
    val effectiveExitingRoute = if (isTransitionPending) animatingToRoute else exitingRoute
    val effectiveForward = if (isTransitionPending) {
        currentRoute.order >= animatingToRoute.order
    } else {
        transitionForward
    }
    val effectiveProgressProvider = remember(isTransitionPending, transitionProgress) {
        { if (isTransitionPending) 0f else transitionProgress.value }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != animatingToRoute) {
            val previousTarget = animatingToRoute
            transitionProgress.snapTo(0f)
            exitingRoute = previousTarget
            transitionForward = currentRoute.order >= previousTarget.order
            animatingToRoute = currentRoute
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            exitingRoute = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground),
        containerColor = MainBackground,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CardBackground,
                    contentColor = PrimaryText,
                    actionColor = UpperBodyAccent,
                    shape = SnackbarShape
                )
            }
        },
        topBar = {
            TopHeader(
                dayInfo = dayInfo,
                onOpenSettings = onOpenReminderSettings
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                dayType = dayInfo.dayType,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MainBackground)
                .padding(innerPadding)
                .onSizeChanged { viewportWidthPx = it.width }
        ) {
            val widthF = viewportWidthPx.toFloat()
            val isSubTransition = currentRoute.isSubScreen || (effectiveExitingRoute?.isSubScreen == true)

            // 1. Today tab (always mounted)
            KeepAliveRouteLayer(
                route = MainRoute.Today,
                currentRoute = currentRoute,
                exitingRoute = effectiveExitingRoute,
                transitionForward = effectiveForward,
                isSubTransition = isSubTransition,
                progressProvider = effectiveProgressProvider,
                viewportWidthPx = widthF,
                isWarmupDrawPass = warmupDrawPass
            ) {
                saveableStateHolder.SaveableStateProvider(key = "route_today") {
                    TodayRouteSlot(viewModel = viewModel)
                }
            }

            // 2. Schedule tab (mounted right after frame 1 or immediately if selected)
            if (primaryTabsWarmed || currentRoute == MainRoute.Schedule || effectiveExitingRoute == MainRoute.Schedule) {
                KeepAliveRouteLayer(
                    route = MainRoute.Schedule,
                    currentRoute = currentRoute,
                    exitingRoute = effectiveExitingRoute,
                    transitionForward = effectiveForward,
                    isSubTransition = isSubTransition,
                    progressProvider = effectiveProgressProvider,
                    viewportWidthPx = widthF,
                    isWarmupDrawPass = warmupDrawPass
                ) {
                    saveableStateHolder.SaveableStateProvider(key = "route_schedule") {
                        ScheduleScreen()
                    }
                }
            }

            // 3. Progress tab (mounted right after frame 1 or immediately if selected)
            if (primaryTabsWarmed || currentRoute == MainRoute.Progress || effectiveExitingRoute == MainRoute.Progress) {
                KeepAliveRouteLayer(
                    route = MainRoute.Progress,
                    currentRoute = currentRoute,
                    exitingRoute = effectiveExitingRoute,
                    transitionForward = effectiveForward,
                    isSubTransition = isSubTransition,
                    progressProvider = effectiveProgressProvider,
                    viewportWidthPx = widthF,
                    isWarmupDrawPass = warmupDrawPass
                ) {
                    saveableStateHolder.SaveableStateProvider(key = "route_progress") {
                        ProgressRouteSlot(
                            viewModel = viewModel,
                            onOpenReminderSettings = onOpenReminderSettings,
                            onOpenStats = onOpenStats,
                            onOpenAdventure = onOpenAdventure
                        )
                    }
                }
            }

            // 4. Stats sub-screen (pre-warmed cleanly with isActive-guarded BackHandler)
            if (subScreensWarmed || currentRoute == MainRoute.Stats || effectiveExitingRoute == MainRoute.Stats) {
                KeepAliveRouteLayer(
                    route = MainRoute.Stats,
                    currentRoute = currentRoute,
                    exitingRoute = effectiveExitingRoute,
                    transitionForward = effectiveForward,
                    isSubTransition = isSubTransition,
                    progressProvider = effectiveProgressProvider,
                    viewportWidthPx = widthF,
                    isWarmupDrawPass = warmupDrawPass
                ) {
                    StatsRouteSlot(
                        viewModel = viewModel,
                        isActive = currentRoute == MainRoute.Stats,
                        onBack = onCloseStats
                    )
                }
            }

            // 5. Adventure (Kartu Guild) sub-screen (pre-warmed cleanly with isActive-guarded BackHandler)
            if (subScreensWarmed || currentRoute == MainRoute.Adventure || effectiveExitingRoute == MainRoute.Adventure) {
                KeepAliveRouteLayer(
                    route = MainRoute.Adventure,
                    currentRoute = currentRoute,
                    exitingRoute = effectiveExitingRoute,
                    transitionForward = effectiveForward,
                    isSubTransition = isSubTransition,
                    progressProvider = effectiveProgressProvider,
                    viewportWidthPx = widthF,
                    isWarmupDrawPass = warmupDrawPass
                ) {
                    AdventureRouteSlot(
                        viewModel = viewModel,
                        isActive = currentRoute == MainRoute.Adventure,
                        onBack = onCloseAdventure
                    )
                }
            }
        }
    }

    if (showReminderDialog) {
        ReminderDialogSlot(
            viewModel = viewModel,
            onDismiss = { showReminderDialog = false }
        )
    }

    val activeReward = workoutXpReward
    if (activeReward != null) {
        WorkoutXpRewardDialog(
            reward = activeReward,
            onDismiss = { viewModel.dismissWorkoutXpReward() }
        )
    }
}

internal data class RouteLayerVisualState(
    val zIndex: Float,
    val alpha: Float,
    val translationX: Float
)

internal fun computeRouteLayerVisualState(
    isActive: Boolean,
    isExiting: Boolean,
    hasExitingRoute: Boolean,
    transitionForward: Boolean,
    isSubTransition: Boolean,
    progress: Float,
    viewportWidthPx: Float,
    isWarmupDrawPass: Boolean
): RouteLayerVisualState {
    val t = progress.coerceIn(0f, 1f)
    val enterShiftDivisor = if (isSubTransition) 5f else 9f
    val exitShiftDivisor = if (isSubTransition) 8f else 12f

    return when {
        isActive -> {
            if (t < 1f && hasExitingRoute) {
                val startOffset = if (viewportWidthPx > 0f) viewportWidthPx / enterShiftDivisor else 0f
                val tx = if (transitionForward) {
                    startOffset * (1f - t)
                } else {
                    -startOffset * (1f - t)
                }
                RouteLayerVisualState(
                    zIndex = 2f,
                    alpha = t,
                    translationX = tx
                )
            } else {
                RouteLayerVisualState(
                    zIndex = 2f,
                    alpha = 1f,
                    translationX = 0f
                )
            }
        }
        isExiting && t < 1f -> {
            val endOffset = if (viewportWidthPx > 0f) viewportWidthPx / exitShiftDivisor else 0f
            val tx = if (transitionForward) {
                -endOffset * t
            } else {
                endOffset * t
            }
            // Keep exiting layer opaque under entering crossfade so background never bleeds through
            RouteLayerVisualState(
                zIndex = 1f,
                alpha = 1f,
                translationX = tx
            )
        }
        isWarmupDrawPass -> {
            RouteLayerVisualState(
                zIndex = -1f,
                alpha = 1f,
                translationX = 0f
            )
        }
        else -> {
            RouteLayerVisualState(
                zIndex = -1f,
                alpha = 0f,
                translationX = 0f
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun KeepAliveRouteLayer(
    route: MainRoute,
    currentRoute: MainRoute,
    exitingRoute: MainRoute?,
    transitionForward: Boolean,
    isSubTransition: Boolean,
    progressProvider: () -> Float,
    viewportWidthPx: Float,
    isWarmupDrawPass: Boolean,
    content: @Composable () -> Unit
) {
    val isActive = route == currentRoute
    val isExiting = !isActive && route == exitingRoute
    val hasExitingRoute = exitingRoute != null
    val z = when {
        isActive -> 2f
        isExiting -> 1f
        else -> -1f
    }

    val gateModifier = if (isActive) {
        Modifier
    } else {
        Modifier
            .focusProperties { canFocus = false }
            .semantics { invisibleToUser() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(z)
            .graphicsLayer {
                val state = computeRouteLayerVisualState(
                    isActive = isActive,
                    isExiting = isExiting,
                    hasExitingRoute = hasExitingRoute,
                    transitionForward = transitionForward,
                    isSubTransition = isSubTransition,
                    progress = progressProvider(),
                    viewportWidthPx = viewportWidthPx,
                    isWarmupDrawPass = isWarmupDrawPass
                )
                translationX = state.translationX
                alpha = state.alpha
            }
            .then(gateModifier)
    ) {
        content()
    }
}

@Composable
private fun TodayRouteSlot(viewModel: MainViewModel) {
    val dayInfo by viewModel.dayInfo.collectAsStateWithLifecycle()
    val selectedWorkoutOverride by viewModel.selectedWorkoutOverride.collectAsStateWithLifecycle()
    val exerciseStates by viewModel.exerciseStates.collectAsStateWithLifecycle()
    val overloadSuggestions by viewModel.overloadSuggestions.collectAsStateWithLifecycle()

    val onToggleExpand = remember(viewModel) { { index: Int -> viewModel.toggleExerciseExpand(index) } }
    val onWeightChanged = remember(viewModel) { { exIdx: Int, setIdx: Int, w: String -> viewModel.updateSetWeight(exIdx, setIdx, w) } }
    val onRepsChanged = remember(viewModel) { { exIdx: Int, setIdx: Int, r: String -> viewModel.updateSetReps(exIdx, setIdx, r) } }
    val onToggleSetDone = remember(viewModel) { { exIdx: Int, setIdx: Int -> viewModel.toggleSetDone(exIdx, setIdx) } }
    val onRestTimerTapped = remember(viewModel) { { index: Int -> viewModel.handleRestTimer(index) } }
    val onFinishWorkout = remember(viewModel) { { viewModel.finishWorkoutAndSave() } }
    val onSelectWorkoutOverride = remember(viewModel) {
        { type: com.example.data.model.DayType? -> viewModel.setTodayWorkoutOverride(type) }
    }

    TodayScreen(
        dayInfo = dayInfo,
        exerciseStates = exerciseStates,
        onToggleExpand = onToggleExpand,
        onWeightChanged = onWeightChanged,
        onRepsChanged = onRepsChanged,
        onToggleSetDone = onToggleSetDone,
        onRestTimerTapped = onRestTimerTapped,
        onFinishWorkout = onFinishWorkout,
        selectedWorkoutOverride = selectedWorkoutOverride,
        onSelectWorkoutOverride = onSelectWorkoutOverride,
        restTimerStateFlow = viewModel.restTimerState,
        overloadSuggestions = overloadSuggestions
    )
}

@Composable
private fun ProgressRouteSlot(
    viewModel: MainViewModel,
    onOpenReminderSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAdventure: () -> Unit
) {
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val sessionsLast7Days by viewModel.sessionsLast7DaysCount.collectAsStateWithLifecycle()
    val latestWeight by viewModel.latestWeight.collectAsStateWithLifecycle()
    val allWeights by viewModel.allWeightsAsc.collectAsStateWithLifecycle()
    val allMeasurements by viewModel.allMeasurementsAsc.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val startWeightKg by viewModel.startWeightKg.collectAsStateWithLifecycle()
    val targetWeightKg by viewModel.targetWeightKg.collectAsStateWithLifecycle()
    val weightTrendResult by viewModel.weightTrendResult.collectAsStateWithLifecycle()
    val gamificationState by viewModel.gamificationState.collectAsStateWithLifecycle()
    val completedSetsBySessionId by viewModel.sessionCompletedSetsMap.collectAsStateWithLifecycle()

    val onSaveWeight = remember(viewModel) { { w: Float -> viewModel.saveWeight(w) } }
    val onSaveMeasurement = remember(viewModel) { { s: Float, w: Float -> viewModel.saveMeasurement(s, w) } }
    val onDeleteSession = remember(viewModel) { { id: Int -> viewModel.deleteSession(id) } }
    val onUpdateSessionExercises = remember(viewModel) { { id: Int, json: String -> viewModel.updateSessionExercises(id, json) } }
    val onUpdateWeightGoals = remember(viewModel) { { startKg: Float, targetKg: Float -> viewModel.updateWeightGoals(startKg, targetKg) } }
    val onExportBackup: suspend () -> String = remember(viewModel) { suspend { viewModel.exportBackupJson() } }
    val onRestoreBackup = remember(viewModel) { { data: com.example.data.BackupData -> viewModel.restoreBackup(data) } }
    val onShowSnackbar = remember(viewModel) { { msg: String -> viewModel.showSnackbar(msg) } }

    ProgressScreen(
        sessionsLast7Days = sessionsLast7Days,
        latestWeight = latestWeight,
        totalSessions = allSessions.size,
        allWeights = allWeights,
        allSessions = allSessions,
        allMeasurements = allMeasurements,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        isReminderEnabled = reminderEnabled,
        onSaveWeight = onSaveWeight,
        onSaveMeasurement = onSaveMeasurement,
        onDeleteSession = onDeleteSession,
        onUpdateSessionExercises = onUpdateSessionExercises,
        onOpenReminderSettings = onOpenReminderSettings,
        startWeightKg = startWeightKg,
        targetWeightKg = targetWeightKg,
        weightTrendResult = weightTrendResult,
        onUpdateWeightGoals = onUpdateWeightGoals,
        onOpenStats = onOpenStats,
        gamificationState = gamificationState,
        onOpenAdventure = onOpenAdventure,
        onExportBackup = onExportBackup,
        onRestoreBackup = onRestoreBackup,
        onShowSnackbar = onShowSnackbar,
        completedSetsBySessionId = completedSetsBySessionId
    )
}

@Composable
private fun StatsRouteSlot(
    viewModel: MainViewModel,
    isActive: Boolean,
    onBack: () -> Unit
) {
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val precomputedSummary by viewModel.statsSummary30Days.collectAsStateWithLifecycle()
    StatsScreen(
        allSessions = allSessions,
        onBack = onBack,
        precomputedSummary = precomputedSummary,
        isActive = isActive
    )
}

@Composable
private fun AdventureRouteSlot(
    viewModel: MainViewModel,
    isActive: Boolean,
    onBack: () -> Unit
) {
    val gamificationState by viewModel.gamificationState.collectAsStateWithLifecycle()
    AdventureScreen(
        gamificationState = gamificationState,
        onBack = onBack,
        isActive = isActive
    )
}

@Composable
private fun ReminderDialogSlot(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val weighInReminderEnabled by viewModel.weighInReminderEnabled.collectAsStateWithLifecycle()
    val weighInReminderDay by viewModel.weighInReminderDay.collectAsStateWithLifecycle()
    val weighInReminderHour by viewModel.weighInReminderHour.collectAsStateWithLifecycle()
    val weighInReminderMinute by viewModel.weighInReminderMinute.collectAsStateWithLifecycle()

    ReminderDialog(
        initialHour = reminderHour,
        initialMinute = reminderMinute,
        isReminderEnabled = reminderEnabled,
        weighInEnabled = weighInReminderEnabled,
        weighInDay = weighInReminderDay,
        weighInHour = weighInReminderHour,
        weighInMinute = weighInReminderMinute,
        onSaveTime = { h, m -> viewModel.updateReminderTime(h, m, reminderEnabled) },
        onToggleReminder = { viewModel.toggleReminder(it) },
        onToggleWeighInReminder = { viewModel.toggleWeighInReminder(it) },
        onWeighInDaySelected = { viewModel.updateWeighInReminderDay(it) },
        onSaveWeighInSettings = { enabled, day, h, m ->
            viewModel.updateWeighInReminderSettings(enabled, day, h, m)
        },
        onDismiss = onDismiss
    )
}
