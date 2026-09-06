package com.poshan.tablettime.ui.checkin

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poshan.tablettime.R
import com.poshan.tablettime.alarm.AlarmReceiver
import com.poshan.tablettime.alarm.AlarmScheduler
import com.poshan.tablettime.alarm.AlarmSoundPlayer
import com.poshan.tablettime.ui.components.TabletCard
import com.poshan.tablettime.ui.theme.SuccessGreen
import com.poshan.tablettime.ui.theme.TabletTimeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CheckInStep {
    ASK_QUESTION,
    CONFIRMED_YES,
    CHOOSE_SNOOZE,
    SNOOZE_CONFIRMED
}

class CheckInActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_LABEL = "extra_reminder_label"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lock screen
        setupLockScreenFlags()

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val reminderLabel = intent.getStringExtra(EXTRA_REMINDER_LABEL) ?: ""
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        setContent {
            TabletTimeTheme {
                CheckInScreen(
                    reminderId = reminderId,
                    reminderLabel = reminderLabel,
                    isSnooze = isSnooze,
                    onStopSound = { AlarmSoundPlayer.stop() },
                    onDismissNotification = { dismissNotification(reminderId) },
                    onFinish = { finishAndRemoveTask() },
                    scheduler = AlarmScheduler(this)
                )
            }
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }
    }

    private fun dismissNotification(reminderId: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmSoundPlayer.stop()
    }
}

@Composable
fun CheckInScreen(
    reminderId: Long,
    reminderLabel: String,
    isSnooze: Boolean,
    onStopSound: () -> Unit,
    onDismissNotification: () -> Unit,
    onFinish: () -> Unit,
    scheduler: AlarmScheduler,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(CheckInStep.ASK_QUESTION) }
    var selectedSnoozeMinutes by remember { mutableStateOf(10) }
    val coroutineScope = rememberCoroutineScope()

    // Gentle pulse animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 })
                    .togetherWith(fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 3 })
            },
            label = "checkInStepTransition"
        ) { step ->
            when (step) {
                CheckInStep.ASK_QUESTION -> {
                    QuestionStep(
                        label = reminderLabel,
                        pulseScale = pulseScale,
                        onYes = {
                            onStopSound()
                            onDismissNotification()
                            currentStep = CheckInStep.CONFIRMED_YES
                        },
                        onNo = {
                            onStopSound()
                            currentStep = CheckInStep.CHOOSE_SNOOZE
                        }
                    )
                }
                CheckInStep.CONFIRMED_YES -> {
                    ConfirmedYesStep(
                        onAutoClose = {
                            onDismissNotification()
                            onFinish()
                        }
                    )
                }
                CheckInStep.CHOOSE_SNOOZE -> {
                    SnoozePickerStep(
                        onSnoozeSelected = { minutes ->
                            selectedSnoozeMinutes = minutes
                            coroutineScope.launch {
                                scheduler.scheduleSnooze(reminderId, reminderLabel, minutes)
                                onDismissNotification()
                                currentStep = CheckInStep.SNOOZE_CONFIRMED
                            }
                        }
                    )
                }
                CheckInStep.SNOOZE_CONFIRMED -> {
                    SnoozeConfirmedStep(
                        snoozeMinutes = selectedSnoozeMinutes,
                        onAutoClose = onFinish
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionStep(
    label: String,
    pulseScale: Float,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pulsing Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MedicalServices,
                        contentDescription = stringResource(R.string.medication_pill_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = stringResource(R.string.checkin_subtitle),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val questionText = if (label.isNotBlank()) {
                label
            } else {
                stringResource(R.string.checkin_question_default)
            }

            Text(
                text = questionText,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 36.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Action Buttons: Yes / No (Big tap targets)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onYes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_yes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onNo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_no),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ConfirmedYesStep(
    onAutoClose: () -> Unit
) {
    LaunchedEffect(Unit) {
        // Auto-close after ~6 seconds per spec (5-8 seconds)
        delay(6000L)
        onAutoClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(110.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(68.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.checkin_yes_confirmation),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.checkin_closing_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SnoozePickerStep(
    onSnoozeSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Snooze,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.snooze_question),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.snooze_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Snooze Options: 5 min, 10 min, 30 min, 1 hour
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val snoozeOptions = listOf(
                R.string.snooze_5_min to 5,
                R.string.snooze_10_min to 10,
                R.string.snooze_30_min to 30,
                R.string.snooze_1_hour to 60
            )

            snoozeOptions.forEach { (resId, minutes) ->
                Button(
                    onClick = { onSnoozeSelected(minutes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(resId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SnoozeConfirmedStep(
    snoozeMinutes: Int,
    onAutoClose: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2500L)
        onAutoClose()
    }

    val snoozeLabel = when (snoozeMinutes) {
        5 -> stringResource(R.string.snooze_5_min)
        10 -> stringResource(R.string.snooze_10_min)
        30 -> stringResource(R.string.snooze_30_min)
        else -> stringResource(R.string.snooze_1_hour)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Snooze,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.snooze_scheduled_feedback, snoozeLabel),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.snooze_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
