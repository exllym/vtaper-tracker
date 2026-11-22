package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BackupData
import com.example.data.BackupManager
import com.example.data.DraftWorkoutStore
import com.example.data.WeightGoalManager
import com.example.data.WorkoutOverrideStore
import com.example.data.db.AppDatabase
import com.example.data.model.BodyMeasurement
import com.example.data.model.DayInfo
import com.example.data.model.DayType
import com.example.data.model.ExerciseState
import com.example.data.model.GamificationEngine
import com.example.data.model.GamificationState
import com.example.data.model.OverloadSuggestion
import com.example.data.model.ProgressiveOverloadCalculator
import com.example.data.model.RestTimerStatus
import com.example.data.model.WeightLog
import com.example.data.model.WeightTrendAnalyzer
import com.example.data.model.WeightTrendResult
import com.example.data.model.WorkoutProgram
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutXpReward
import com.example.data.repository.WorkoutRepository
import com.example.reminder.ReminderManager
import com.example.reminder.RestTimerAlarmReceiver
import com.example.reminder.RestTimerStore
import com.example.ui.screens.StatsSummaryState
import com.example.ui.screens.buildStatsSummary30Days
import com.example.util.VibrationSoundHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WorkoutRepository

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate = _currentDate.asStateFlow()

    private val _dayInfo = MutableStateFlow(WorkoutProgram.getDayInfo(LocalDate.now()))
    val dayInfo = _dayInfo.asStateFlow()

    private val _selectedWorkoutOverride = MutableStateFlow<DayType?>(null)
    val selectedWorkoutOverride: StateFlow<DayType?> = _selectedWorkoutOverride.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Today, 1: Schedule, 2: Progress
    val selectedTab = _selectedTab.asStateFlow()

    private val _exerciseStates = MutableStateFlow<List<ExerciseState>>(emptyList())
    val exerciseStates = _exerciseStates.asStateFlow()

    private val timerJobs = mutableMapOf<Int, Job>()
    private var draftSaveJob: Job? = null
    private var overloadCalcJob: Job? = null

    // Rest timer state decoupled from exerciseStates to prevent full list recomposition churn
    private val _restTimerState = MutableStateFlow<Map<Int, RestTimerStatus>>(emptyMap())
    val restTimerState: StateFlow<Map<Int, RestTimerStatus>> = _restTimerState.asStateFlow()

    // Progressive overload suggestions map (exerciseName -> suggestion)
    private val _overloadSuggestions = MutableStateFlow<Map<String, OverloadSuggestion>>(emptyMap())
    val overloadSuggestions: StateFlow<Map<String, OverloadSuggestion>> = _overloadSuggestions.asStateFlow()

    // Reminder state
    private val _reminderHour = MutableStateFlow(16)
    val reminderHour = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute = _reminderMinute.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(true)
    val reminderEnabled = _reminderEnabled.asStateFlow()

    // Weekly weigh-in reminder state (default: Sunday 07:00)
    private val _weighInReminderEnabled = MutableStateFlow(true)
    val weighInReminderEnabled: StateFlow<Boolean> = _weighInReminderEnabled.asStateFlow()

    private val _weighInReminderDay = MutableStateFlow(DayOfWeek.SUNDAY)
    val weighInReminderDay: StateFlow<DayOfWeek> = _weighInReminderDay.asStateFlow()

    private val _weighInReminderHour = MutableStateFlow(7)
    val weighInReminderHour: StateFlow<Int> = _weighInReminderHour.asStateFlow()

    private val _weighInReminderMinute = MutableStateFlow(0)
    val weighInReminderMinute: StateFlow<Int> = _weighInReminderMinute.asStateFlow()

    // Weight goals
    private val _startWeightKg = MutableStateFlow(WeightGoalManager.DEFAULT_START_WEIGHT)
    val startWeightKg: StateFlow<Float> = _startWeightKg.asStateFlow()

    private val _targetWeightKg = MutableStateFlow(WeightGoalManager.DEFAULT_TARGET_WEIGHT)
    val targetWeightKg: StateFlow<Float> = _targetWeightKg.asStateFlow()

    private val _workoutXpReward = MutableStateFlow<WorkoutXpReward?>(null)
    val workoutXpReward: StateFlow<WorkoutXpReward?> = _workoutXpReward.asStateFlow()

    val allSessions: StateFlow<List<WorkoutSession>>
    val sessionsLast7DaysCount: StateFlow<Int>
    val latestWeight: StateFlow<WeightLog?>
    val allWeightsAsc: StateFlow<List<WeightLog>>
    val allMeasurementsAsc: StateFlow<List<BodyMeasurement>>
    val weightTrendResult: StateFlow<WeightTrendResult>
    val gamificationState: StateFlow<GamificationState>
    val sessionCompletedSetsMap: StateFlow<Map<Int, Int>>
    val statsSummary30Days: StateFlow<StatsSummaryState>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WorkoutRepository(db.workoutDao(), db.weightDao(), db.measurementDao())

        allSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

        allMeasurementsAsc = repository.allMeasurementsAsc.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

        val sevenDaysAgoMillis = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        sessionsLast7DaysCount = allSessions.map { sessions ->
            sessions.count { it.timestamp >= sevenDaysAgoMillis }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

        latestWeight = repository.latestWeight.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

        allWeightsAsc = repository.allWeightsAsc.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

        weightTrendResult = combine(allWeightsAsc, targetWeightKg) { weights, target ->
            withContext(Dispatchers.Default) {
                WeightTrendAnalyzer.analyze(weights, target)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = WeightTrendResult()
        )

        gamificationState = combine(
            allSessions,
            allWeightsAsc,
            allMeasurementsAsc,
            currentDate
        ) { sessions, weights, measurements, today ->
            withContext(Dispatchers.Default) {
                GamificationEngine.compute(
                    sessions = sessions,
                    weightLogs = weights,
                    measurements = measurements,
                    today = today
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GamificationEngine.emptyState()
        )

        sessionCompletedSetsMap = allSessions.map { sessions ->
            withContext(Dispatchers.Default) {
                val map = HashMap<Int, Int>(sessions.size)
                for (session in sessions) {
                    map[session.id] = WorkoutProgram.countCompletedSets(session.exercisesJson)
                }
                map
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

        statsSummary30Days = allSessions.map { sessions ->
            withContext(Dispatchers.Default) {
                buildStatsSummary30Days(sessions)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = StatsSummaryState()
        )

        // Initialize reminder time
        val (h, m) = ReminderManager.getReminderTime(application)
        _reminderHour.value = h
        _reminderMinute.value = m
        _reminderEnabled.value = ReminderManager.isReminderEnabled(application)

        // Initialize weekly weigh-in reminder
        _weighInReminderEnabled.value = ReminderManager.isWeighInReminderEnabled(application)
        _weighInReminderDay.value = ReminderManager.getWeighInReminderDay(application)
        val (wh, wm) = ReminderManager.getWeighInReminderTime(application)
        _weighInReminderHour.value = wh
        _weighInReminderMinute.value = wm

        // Initialize weight goals
        _startWeightKg.value = WeightGoalManager.getStartWeight(application)
        _targetWeightKg.value = WeightGoalManager.getTargetWeight(application)

        // Initialize today's exercises & check draft
        refreshDay(LocalDate.now())

        // Sync any active rest timers on launch
        syncActiveRestTimers()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun refreshDay(date: LocalDate = LocalDate.now()) {
        _currentDate.value = date
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val calendarInfo = WorkoutProgram.getDayInfo(date)
        val validOverride = WorkoutOverrideStore.getValidOverride(getApplication(), dateString)
        _selectedWorkoutOverride.value = validOverride

        val effectiveType = validOverride ?: calendarInfo.dayType
        val effectiveInfo = calendarInfo.copy(
            dayType = effectiveType,
            dayLabel = effectiveType.label
        )
        _dayInfo.value = effectiveInfo

        timerJobs.values.forEach { it.cancel() }
        timerJobs.clear()
        _restTimerState.value = emptyMap()

        // Check DraftWorkoutStore keyed by (date, dayType)
        val savedJson = DraftWorkoutStore.getDraft(getApplication(), dateString, effectiveType)
        if (!savedJson.isNullOrBlank()) {
            val restored = WorkoutProgram.deserializeExercises(savedJson, effectiveType)
            _exerciseStates.value = restored
        } else {
            _exerciseStates.value = WorkoutProgram.getInitialExerciseStates(effectiveType)
        }

        // Calculate progressive overload suggestions for effective day type
        calculateOverloadSuggestions(effectiveType)
    }

    fun setTodayWorkoutOverride(selectedType: DayType?) {
        val normalizedOverride = when (selectedType) {
            DayType.UPPER, DayType.LOWER -> selectedType
            else -> null
        }

        val today = _currentDate.value
        val dateString = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val calendarInfo = WorkoutProgram.getDayInfo(today)
        val currentEffectiveType = _dayInfo.value.dayType
        val newEffectiveType = normalizedOverride ?: calendarInfo.dayType

        if (normalizedOverride == _selectedWorkoutOverride.value &&
            newEffectiveType == currentEffectiveType
        ) {
            return
        }

        if (normalizedOverride != null) {
            WorkoutOverrideStore.saveOverride(getApplication(), dateString, normalizedOverride)
        } else {
            WorkoutOverrideStore.clearOverride(getApplication())
        }
        _selectedWorkoutOverride.value = normalizedOverride
        _dayInfo.value = calendarInfo.copy(
            dayType = newEffectiveType,
            dayLabel = newEffectiveType.label
        )

        if (newEffectiveType != currentEffectiveType) {
            // Flush current draft immediately before switching day type
            draftSaveJob?.cancel()
            draftSaveJob = null
            if (currentEffectiveType == DayType.UPPER || currentEffectiveType == DayType.LOWER) {
                val currentJson = WorkoutProgram.serializeExercises(_exerciseStates.value)
                DraftWorkoutStore.saveDraft(
                    getApplication(),
                    dateString,
                    currentEffectiveType,
                    currentJson
                )
            }

            // Clear all running/persisted rest timers, alarms, and notifications (indices differ per day type)
            clearAllRestTimersAndNotifications()

            // Load draft for (today, newEffectiveType) or initial states
            val savedJson = DraftWorkoutStore.getDraft(getApplication(), dateString, newEffectiveType)
            _exerciseStates.value = if (!savedJson.isNullOrBlank()) {
                WorkoutProgram.deserializeExercises(savedJson, newEffectiveType)
            } else {
                WorkoutProgram.getInitialExerciseStates(newEffectiveType)
            }

            calculateOverloadSuggestions(newEffectiveType)
        }
    }

    private fun clearAllRestTimersAndNotifications() {
        val app = getApplication<Application>()
        val indices = buildSet {
            addAll(timerJobs.keys)
            addAll(_restTimerState.value.keys)
            addAll(RestTimerStore.getAllActiveTimers(app).keys)
            for (i in _exerciseStates.value.indices) {
                add(i)
            }
        }
        timerJobs.values.forEach { it.cancel() }
        timerJobs.clear()
        RestTimerStore.clearAllTimers(app)
        for (index in indices) {
            RestTimerAlarmReceiver.cancelAlarmAndNotification(app, index)
        }
        _restTimerState.value = emptyMap()
    }

    private fun calculateOverloadSuggestions(dayType: DayType) {
        if (dayType != DayType.UPPER && dayType != DayType.LOWER) {
            _overloadSuggestions.value = emptyMap()
            return
        }

        overloadCalcJob?.cancel()
        overloadCalcJob = viewModelScope.launch(Dispatchers.IO) {
            val exercises = when (dayType) {
                DayType.UPPER -> WorkoutProgram.upperBodyExercises
                DayType.LOWER -> WorkoutProgram.lowerBodyExercises
                else -> emptyList()
            }

            val suggestionsMap = mutableMapOf<String, OverloadSuggestion>()
            for (ex in exercises) {
                val recentSessions = repository.getRecentSessionsForExercise(ex.name, limit = 10)
                val suggestion = withContext(Dispatchers.Default) {
                    ProgressiveOverloadCalculator.suggestNextWeight(
                        recentSessions = recentSessions,
                        exerciseName = ex.name,
                        currentTargetRepRange = ex.target,
                        inputType = ex.inputType
                    )
                }
                suggestionsMap[ex.name] = suggestion
            }

            _overloadSuggestions.value = suggestionsMap
        }
    }

    fun checkDateRollover() {
        val today = LocalDate.now()
        if (today != _currentDate.value) {
            // Day rolled over: clear previous day's drafts, override, and stale timers
            WorkoutOverrideStore.clearOverride(getApplication())
            DraftWorkoutStore.clearAllDrafts(getApplication())
            clearAllRestTimersAndNotifications()
            refreshDay(today)
        }
    }

    fun onResume() {
        checkDateRollover()
        syncActiveRestTimers()
    }

    private fun syncActiveRestTimers() {
        val now = System.currentTimeMillis()
        val activeTimers = RestTimerStore.getAllActiveTimers(getApplication())
        val currentStates = _exerciseStates.value

        for ((exerciseIndex, endTs) in activeTimers) {
            if (exerciseIndex !in currentStates.indices) {
                RestTimerStore.clearTimer(getApplication(), exerciseIndex)
                continue
            }

            val diffMillis = endTs - now
            val remainingSec = (diffMillis / 1000).toInt()

            if (remainingSec <= 0) {
                // Timer elapsed while away: mark as finished and clean up
                RestTimerStore.clearTimer(getApplication(), exerciseIndex)
                timerJobs[exerciseIndex]?.cancel()
                timerJobs.remove(exerciseIndex)
                _restTimerState.value = _restTimerState.value + (
                    exerciseIndex to RestTimerStatus(
                        isRunning = false,
                        remainingSeconds = 0,
                        isFinished = true
                    )
                )
                // Schedule dismiss
                viewModelScope.launch {
                    delay(2500L)
                    _restTimerState.value = _restTimerState.value - exerciseIndex
                }
            } else {
                // Timer is still running, resume countdown
                startRestTimerCountdown(exerciseIndex, remainingSec, endTs)
            }
        }
    }

    private fun scheduleDraftSave() {
        val targetDate = _currentDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val targetDayType = _dayInfo.value.dayType
        if (targetDayType != DayType.UPPER && targetDayType != DayType.LOWER) return
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(400L) // slight debounce
            val json = WorkoutProgram.serializeExercises(_exerciseStates.value)
            DraftWorkoutStore.saveDraft(getApplication(), targetDate, targetDayType, json)
        }
    }

    fun toggleExerciseExpand(index: Int) {
        val currentList = _exerciseStates.value.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            currentList[index] = item.copy(isExpanded = !item.isExpanded)
            _exerciseStates.value = currentList
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, rawInput: String) {
        val normalized = rawInput.replace(',', '.')
        var hasDot = false
        val clean = buildString {
            for (ch in normalized) {
                if (ch.isDigit()) {
                    append(ch)
                } else if (ch == '.' && !hasDot) {
                    hasDot = true
                    append(ch)
                }
            }
        }.take(6)

        val currentList = _exerciseStates.value.toMutableList()
        if (exerciseIndex in currentList.indices) {
            val ex = currentList[exerciseIndex]
            val sets = ex.sets.toMutableList()
            if (setIndex in sets.indices) {
                sets[setIndex] = sets[setIndex].copy(weightKg = clean)
                currentList[exerciseIndex] = ex.copy(sets = sets)
                _exerciseStates.value = currentList
                scheduleDraftSave()
            }
        }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, rawInput: String) {
        val clean = rawInput.filter { it.isDigit() }.take(4)
        val currentList = _exerciseStates.value.toMutableList()
        if (exerciseIndex in currentList.indices) {
            val ex = currentList[exerciseIndex]
            val sets = ex.sets.toMutableList()
            if (setIndex in sets.indices) {
                sets[setIndex] = sets[setIndex].copy(reps = clean)
                currentList[exerciseIndex] = ex.copy(sets = sets)
                _exerciseStates.value = currentList
                scheduleDraftSave()
            }
        }
    }

    fun toggleSetDone(exerciseIndex: Int, setIndex: Int) {
        val currentList = _exerciseStates.value.toMutableList()
        if (exerciseIndex in currentList.indices) {
            val ex = currentList[exerciseIndex]
            val sets = ex.sets.toMutableList()
            if (setIndex in sets.indices) {
                val currentDone = sets[setIndex].isDone
                sets[setIndex] = sets[setIndex].copy(isDone = !currentDone)
                currentList[exerciseIndex] = ex.copy(sets = sets)
                _exerciseStates.value = currentList
                scheduleDraftSave()
            }
        }
    }

    fun handleRestTimer(exerciseIndex: Int) {
        val currentTimer = _restTimerState.value[exerciseIndex] ?: RestTimerStatus()
        val currentList = _exerciseStates.value
        if (exerciseIndex !in currentList.indices) return

        if (currentTimer.isRunning) {
            // Cancel running timer
            timerJobs[exerciseIndex]?.cancel()
            timerJobs.remove(exerciseIndex)
            RestTimerStore.clearTimer(getApplication(), exerciseIndex)
            RestTimerAlarmReceiver.cancelAlarm(getApplication(), exerciseIndex)
            _restTimerState.value = _restTimerState.value - exerciseIndex
        } else {
            // Start timer
            timerJobs[exerciseIndex]?.cancel()
            val totalSeconds = currentList[exerciseIndex].def.restSeconds
            val endTimestampMillis = System.currentTimeMillis() + (totalSeconds * 1000L)

            // Persist timer state
            RestTimerStore.saveTimer(getApplication(), exerciseIndex, endTimestampMillis, totalSeconds)

            // Schedule exact alarm with AlarmManager
            RestTimerAlarmReceiver.scheduleAlarm(
                getApplication(),
                exerciseIndex,
                currentList[exerciseIndex].def.name,
                endTimestampMillis
            )

            // Start in-memory countdown loop
            startRestTimerCountdown(exerciseIndex, totalSeconds, endTimestampMillis)
        }
    }

    private fun startRestTimerCountdown(exerciseIndex: Int, initialSeconds: Int, endTimestampMillis: Long) {
        timerJobs[exerciseIndex]?.cancel()
        _restTimerState.value = _restTimerState.value + (
            exerciseIndex to RestTimerStatus(
                isRunning = true,
                remainingSeconds = initialSeconds,
                isFinished = false
            )
        )

        val job = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((endTimestampMillis - now) / 1000).toInt()

                if (remaining <= 0) {
                    break
                }

                _restTimerState.value = _restTimerState.value + (
                    exerciseIndex to RestTimerStatus(
                        isRunning = true,
                        remainingSeconds = remaining,
                        isFinished = false
                    )
                )

                delay(1000L)
            }

            // Clean up store and alarm
            RestTimerStore.clearTimer(getApplication(), exerciseIndex)
            RestTimerAlarmReceiver.cancelAlarm(getApplication(), exerciseIndex)

            // Trigger sound & vibration if app is foreground
            VibrationSoundHelper.playTimerFinished(getApplication())

            // Show finish state
            _restTimerState.value = _restTimerState.value + (
                exerciseIndex to RestTimerStatus(
                    isRunning = false,
                    remainingSeconds = 0,
                    isFinished = true
                )
            )

            // Auto-dismiss after 2.5 seconds
            delay(2500L)
            _restTimerState.value = _restTimerState.value - exerciseIndex
            timerJobs.remove(exerciseIndex)
        }
        timerJobs[exerciseIndex] = job
    }

    fun finishWorkoutAndSave() {
        val exercises = _exerciseStates.value
        val hasCompletedOrFilledSet = exercises.any { it.hasAnyCompletedSet }

        if (!hasCompletedOrFilledSet) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("Isi minimal satu set terlebih dahulu!"))
            }
            return
        }

        val todayDate = _currentDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val effectiveDayType = _dayInfo.value.dayType
        val dayType = effectiveDayType.code
        val dayLabel = _dayInfo.value.dayLabel

        draftSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val beforeSessions = repository.allSessions.first()
            val currentWeights = repository.allWeightsAsc.first()
            val currentMeasurements = repository.allMeasurementsAsc.first()
            val todayLocalDate = _currentDate.value

            val json = WorkoutProgram.serializeExercises(exercises)
            val session = WorkoutSession(
                date = todayDate,
                dayType = dayType,
                dayLabel = dayLabel,
                timestamp = System.currentTimeMillis(),
                exercisesJson = json
            )
            repository.saveSession(session)

            val reward = withContext(Dispatchers.Default) {
                GamificationEngine.computePostWorkoutReward(
                    beforeSessions = beforeSessions,
                    newSession = session,
                    weightLogs = currentWeights,
                    measurements = currentMeasurements,
                    today = todayLocalDate
                )
            }

            // Clear only the draft for the saved day type
            DraftWorkoutStore.clearDraft(getApplication(), effectiveDayType)

            // Recompute overload suggestions with new session in history
            calculateOverloadSuggestions(effectiveDayType)

            _workoutXpReward.value = reward
            _eventFlow.emit(UiEvent.ShowSnackbar("Misi tuntas! Catatan guild tersimpan."))
        }
    }

    fun dismissWorkoutXpReward() {
        _workoutXpReward.value = null
    }

    fun saveWeight(kg: Float) {
        val todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val weightLog = WeightLog(
            date = todayDate,
            kg = kg,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveWeight(weightLog)
            _eventFlow.emit(UiEvent.ShowSnackbar("Berat badan $kg kg berhasil dicatat!"))
        }
    }

    fun updateWeightGoals(startKg: Float, targetKg: Float) {
        _startWeightKg.value = startKg
        _targetWeightKg.value = targetKg
        WeightGoalManager.saveWeightGoals(getApplication(), startKg, targetKg)
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar("Target berat badan berhasil diperbarui!"))
        }
    }

    fun updateReminderTime(hour: Int, minute: Int, enabled: Boolean = true) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        _reminderEnabled.value = enabled
        ReminderManager.saveReminderTime(getApplication(), hour, minute, enabled)
        viewModelScope.launch {
            val formatted = String.format("%02d:%02d", hour, minute)
            _eventFlow.emit(UiEvent.ShowSnackbar("Pengingat latihan diatur jam $formatted"))
        }
    }

    fun toggleReminder(enabled: Boolean) {
        _reminderEnabled.value = enabled
        ReminderManager.setReminderEnabled(getApplication(), enabled)
        viewModelScope.launch {
            if (enabled) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Pengingat latihan aktif"))
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar("Pengingat latihan dinonaktifkan"))
            }
        }
    }

    fun toggleWeighInReminder(enabled: Boolean) {
        _weighInReminderEnabled.value = enabled
        ReminderManager.setWeighInReminderEnabled(getApplication(), enabled)
        viewModelScope.launch {
            if (enabled) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Pengingat timbang mingguan aktif"))
            } else {
                _eventFlow.emit(UiEvent.ShowSnackbar("Pengingat timbang mingguan dinonaktifkan"))
            }
        }
    }

    fun updateWeighInReminderDay(dayOfWeek: DayOfWeek) {
        _weighInReminderDay.value = dayOfWeek
        ReminderManager.saveWeighInReminderSettings(
            context = getApplication(),
            enabled = _weighInReminderEnabled.value,
            dayOfWeek = dayOfWeek,
            hour = _weighInReminderHour.value,
            minute = _weighInReminderMinute.value
        )
    }

    fun updateWeighInReminderSettings(
        enabled: Boolean,
        dayOfWeek: DayOfWeek,
        hour: Int,
        minute: Int
    ) {
        _weighInReminderEnabled.value = enabled
        _weighInReminderDay.value = dayOfWeek
        _weighInReminderHour.value = hour
        _weighInReminderMinute.value = minute
        ReminderManager.saveWeighInReminderSettings(
            context = getApplication(),
            enabled = enabled,
            dayOfWeek = dayOfWeek,
            hour = hour,
            minute = minute
        )
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            calculateOverloadSuggestions(_dayInfo.value.dayType)
            _eventFlow.emit(UiEvent.ShowSnackbar("Sesi latihan dihapus"))
        }
    }

    fun updateSessionExercises(sessionId: Int, exercisesJson: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateSessionJson(sessionId, exercisesJson)
            }
            calculateOverloadSuggestions(_dayInfo.value.dayType)
            _eventFlow.emit(UiEvent.ShowSnackbar("Riwayat sesi berhasil diperbarui"))
        }
    }

    fun saveMeasurement(shoulderCm: Float, waistCm: Float) {
        viewModelScope.launch {
            val now = LocalDate.now()
            val measurement = BodyMeasurement(
                date = now.format(DateTimeFormatter.ISO_LOCAL_DATE),
                shoulderCm = shoulderCm,
                waistCm = waistCm,
                timestamp = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) {
                repository.saveMeasurement(measurement)
            }
            _eventFlow.emit(UiEvent.ShowSnackbar("Ukuran tubuh tersimpan (${measurement.shoulderCm}cm / ${measurement.waistCm}cm)"))
        }
    }

    fun deleteMeasurement(measurementId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteMeasurement(measurementId)
            }
            _eventFlow.emit(UiEvent.ShowSnackbar("Data ukuran tubuh dihapus"))
        }
    }

    suspend fun exportBackupJson(): String {
        return withContext(Dispatchers.IO) {
            BackupManager.exportToJson(
                context = getApplication(),
                sessions = allSessions.value,
                weights = allWeightsAsc.value,
                measurements = allMeasurementsAsc.value,
                startWeightKg = _startWeightKg.value,
                targetWeightKg = _targetWeightKg.value,
                reminderHour = _reminderHour.value,
                reminderMinute = _reminderMinute.value,
                reminderEnabled = _reminderEnabled.value,
                weighInReminderEnabled = _weighInReminderEnabled.value,
                weighInReminderDay = _weighInReminderDay.value.value,
                weighInReminderHour = _weighInReminderHour.value,
                weighInReminderMinute = _weighInReminderMinute.value
            )
        }
    }

    fun restoreBackup(backupData: BackupData) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.replaceAllSessions(backupData.sessions)
                repository.replaceAllWeights(backupData.weightLogs)
                repository.replaceAllMeasurements(backupData.bodyMeasurements)
            }
            _startWeightKg.value = backupData.startWeightKg
            _targetWeightKg.value = backupData.targetWeightKg
            WeightGoalManager.saveWeightGoals(getApplication(), backupData.startWeightKg, backupData.targetWeightKg)

            _reminderHour.value = backupData.reminderHour
            _reminderMinute.value = backupData.reminderMinute
            _reminderEnabled.value = backupData.reminderEnabled
            ReminderManager.saveReminderTime(getApplication(), backupData.reminderHour, backupData.reminderMinute, backupData.reminderEnabled)

            val restoredWeighInDay = try {
                DayOfWeek.of(backupData.weighInReminderDay.coerceIn(1, 7))
            } catch (_: Exception) {
                DayOfWeek.SUNDAY
            }
            _weighInReminderEnabled.value = backupData.weighInReminderEnabled
            _weighInReminderDay.value = restoredWeighInDay
            _weighInReminderHour.value = backupData.weighInReminderHour
            _weighInReminderMinute.value = backupData.weighInReminderMinute
            ReminderManager.saveWeighInReminderSettings(
                context = getApplication(),
                enabled = backupData.weighInReminderEnabled,
                dayOfWeek = restoredWeighInDay,
                hour = backupData.weighInReminderHour,
                minute = backupData.weighInReminderMinute
            )

            // Clear drafts/override/timers and refresh
            WorkoutOverrideStore.clearOverride(getApplication())
            DraftWorkoutStore.clearAllDrafts(getApplication())
            clearAllRestTimersAndNotifications()
            refreshDay(_currentDate.value)

            _eventFlow.emit(UiEvent.ShowSnackbar("Data berhasil dipulihkan"))
        }
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackbar(message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJobs.values.forEach { it.cancel() }
        timerJobs.clear()
        draftSaveJob?.cancel()
        overloadCalcJob?.cancel()
        _restTimerState.value = emptyMap()
    }
}
