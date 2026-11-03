package com.proofapp.android.ui.cooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.proofapp.android.data.repository.RecipeRepository
import com.proofapp.android.domain.model.Recipe
import com.proofapp.android.ui.container
import com.proofapp.android.ui.navigation.CookingRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** A timer is either running (has an end time) or paused with some time left. */
data class StepTimer(
    val totalSeconds: Long,
    val endsAtMillis: Long? = null,
    val pausedRemainingMillis: Long = totalSeconds * 1000,
) {
    val running: Boolean get() = endsAtMillis != null

    fun remainingMillis(now: Long): Long =
        (endsAtMillis?.let { it - now } ?: pausedRemainingMillis).coerceAtLeast(0)

    fun finished(now: Long): Boolean = remainingMillis(now) == 0L
    fun progress(now: Long): Float = 1f - remainingMillis(now).toFloat() / (totalSeconds * 1000f)
}

data class CookingState(
    val recipe: Recipe? = null,
    val timers: Map<Int, StepTimer> = emptyMap(),
    val now: Long = System.currentTimeMillis(),
) {
    val runningSteps: List<Int> get() = timers.filter { it.value.running && !it.value.finished(now) }.keys.sorted()
}

class CookingViewModel(
    savedStateHandle: SavedStateHandle,
    recipes: RecipeRepository,
    private val alarms: TimerAlarmScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<CookingRoute>().id
    private val timers = MutableStateFlow<Map<Int, StepTimer>>(emptyMap())
    private val now = MutableStateFlow(clock())
    private var ticker: Job? = null

    val state: StateFlow<CookingState> = combine(recipes.recipe(recipeId), timers, now) { recipe, t, n ->
        CookingState(recipe, t, n)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CookingState())

    fun start(step: Int) {
        val recipe = state.value.recipe ?: return
        val seconds = recipe.steps.getOrNull(step)?.timerSeconds?.toLong() ?: return
        val current = timers.value[step] ?: StepTimer(seconds)
        val remaining = current.remainingMillis(clock()).takeIf { it > 0 } ?: (seconds * 1000)
        val endsAt = clock() + remaining
        timers.update { it + (step to current.copy(endsAtMillis = endsAt)) }
        alarms.schedule(
            alarmId(step), endsAt,
            title = "${recipe.emoji} Step ${step + 1} is done",
            text = recipe.steps[step].instruction,
        )
        ensureTicking()
    }

    fun pause(step: Int) {
        val timer = timers.value[step] ?: return
        timers.update { it + (step to timer.copy(endsAtMillis = null, pausedRemainingMillis = timer.remainingMillis(clock()))) }
        alarms.cancel(alarmId(step))
    }

    fun reset(step: Int) {
        timers.update { it - step }
        alarms.cancel(alarmId(step))
    }

    /** One coroutine drives every on-screen countdown, and only while something is running. */
    private fun ensureTicking() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (isActive) {
                now.value = clock()
                if (timers.value.values.none { it.running && !it.finished(now.value) }) break
                delay(250)
            }
        }
    }

    private fun alarmId(step: Int) = (recipeId.hashCode() * 31) + step

    override fun onCleared() {
        // Leaving cooking mode ends its timers; nobody wants a ghost alarm an hour later.
        timers.value.keys.forEach { alarms.cancel(alarmId(it)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CookingViewModel(createSavedStateHandle(), container.recipeRepository, container.timerAlarms)
            }
        }
    }
}
