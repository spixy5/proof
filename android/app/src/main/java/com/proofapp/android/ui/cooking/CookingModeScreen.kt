package com.proofapp.android.ui.cooking

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proofapp.android.domain.model.Recipe
import com.proofapp.android.ui.components.formatClock
import com.proofapp.android.ui.theme.LocalProofExtras
import kotlinx.coroutines.launch

@Composable
fun CookingModeScreen(
    onClose: () -> Unit,
    viewModel: CookingViewModel = viewModel(factory = CookingViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recipe = state.recipe ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Floury hands shouldn't have to wake the screen.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var askedForPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val startTimer: (Int) -> Unit = { step ->
        if (Build.VERSION.SDK_INT >= 33 && !askedForPermission) {
            askedForPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.start(step)
    }

    val pager = rememberPagerState { recipe.steps.size }
    val scope = rememberCoroutineScope()
    var finished by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Exit cooking mode") }
            Column(Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "Step ${pager.currentPage + 1} of ${recipe.steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(recipe.emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
        }
        LinearProgressIndicator(
            progress = { (pager.currentPage + 1f) / recipe.steps.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(6.dp).clip(CircleShape),
        )

        // Timers running on other steps stay visible, e.g. the oven preheating while you shape.
        val others = state.runningSteps.filter { it != pager.currentPage }
        if (others.isNotEmpty()) {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(others) { step ->
                    val remaining = state.timers.getValue(step).remainingMillis(state.now)
                    AssistChip(
                        onClick = { scope.launch { pager.animateScrollToPage(step) } },
                        label = { Text("Step ${step + 1} · ${formatClock(remaining / 1000)}") },
                        leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                    )
                }
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            StepPage(
                recipe = recipe,
                index = page,
                timer = state.timers[page],
                now = state.now,
                onStart = { startTimer(page) },
                onPause = { viewModel.pause(page) },
                onReset = { viewModel.reset(page) },
            )
        }

        // Footer navigation
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                enabled = pager.currentPage > 0,
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Back")
            }
            val last = pager.currentPage == recipe.steps.lastIndex
            Button(
                onClick = {
                    if (last) finished = true else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text(if (last) "Finish" else "Next")
                Spacer(Modifier.width(8.dp))
                Icon(if (last) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }

    if (finished) {
        AlertDialog(
            onDismissRequest = { finished = false },
            icon = { Text("🎉", fontSize = 40.sp) },
            title = { Text("Out of the oven!") },
            text = { Text("${recipe.title} is done. Let it cool, then enjoy every crumb.", textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
        )
    }
}

@Composable
private fun StepPage(
    recipe: Recipe,
    index: Int,
    timer: StepTimer?,
    now: Long,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
) {
    val step = recipe.steps[index]
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${index + 1}",
            style = MaterialTheme.typography.displayLarge.merge(LocalProofExtras.current.numberStyle),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            step.instruction,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
        )
        step.timerSeconds?.let { seconds ->
            Spacer(Modifier.height(32.dp))
            TimerDial(
                timer = timer ?: StepTimer(seconds.toLong()),
                now = now,
                onStart = onStart,
                onPause = onPause,
                onReset = onReset,
            )
        }
    }
}

@Composable
private fun TimerDial(timer: StepTimer, now: Long, onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit) {
    val done = timer.finished(now)
    val ringColor by animateColorAsState(
        if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "ring",
    )
    Surface(shape = RoundedCornerShape(36.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (done) 1f else timer.progress(now) },
                    modifier = Modifier.size(200.dp),
                    color = ringColor,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (done) "Done!" else formatClock((timer.remainingMillis(now) + 999) / 1000),
                        style = MaterialTheme.typography.displaySmall.merge(LocalProofExtras.current.numberStyle),
                    )
                    Text(
                        when {
                            done -> "Nice timing"
                            timer.running -> "running"
                            else -> "of ${formatClock(timer.totalSeconds)}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(onClick = onReset, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset timer")
                }
                FilledIconButton(onClick = if (timer.running && !done) onPause else onStart, modifier = Modifier.size(72.dp)) {
                    Icon(
                        if (timer.running && !done) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (timer.running && !done) "Pause timer" else "Start timer",
                        modifier = Modifier.size(36.dp),
                    )
                }
                Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
