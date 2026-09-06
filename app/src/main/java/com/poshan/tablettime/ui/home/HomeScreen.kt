package com.poshan.tablettime.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poshan.tablettime.R
import com.poshan.tablettime.data.Reminder
import com.poshan.tablettime.ui.components.AnimatedSwitch
import com.poshan.tablettime.ui.components.PermissionRationaleCard
import com.poshan.tablettime.ui.components.TabletCard
import com.poshan.tablettime.viewmodel.HomeUiState
import com.poshan.tablettime.viewmodel.HomeViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddReminderClick: () -> Unit,
    onEditReminderClick: (Long) -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminderClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.action_add_reminder),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Title Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MedicalServices,
                                contentDescription = stringResource(R.string.medication_pill_icon),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Master Reminders Switch Card
            item {
                TabletCard(
                    backgroundColor = if (uiState.isMasterEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.master_switch_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    if (uiState.isMasterEnabled) {
                                        R.string.master_switch_subtitle_on
                                    } else {
                                        R.string.master_switch_subtitle_off
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        AnimatedSwitch(
                            checked = uiState.isMasterEnabled,
                            onCheckedChange = { viewModel.toggleMasterReminders(it) }
                        )
                    }
                }
            }

            // Exact Alarm Permission Warning
            if (uiState.needsExactAlarmPermission) {
                item {
                    PermissionRationaleCard(
                        title = stringResource(R.string.permission_exact_alarm_title),
                        description = stringResource(R.string.permission_exact_alarm_desc),
                        buttonText = stringResource(R.string.permission_grant_btn),
                        onButtonClick = onRequestExactAlarmPermission
                    )
                }
            }

            // Notification Permission Warning
            if (uiState.needsNotificationPermission) {
                item {
                    PermissionRationaleCard(
                        title = stringResource(R.string.permission_notification_title),
                        description = stringResource(R.string.permission_notification_desc),
                        buttonText = stringResource(R.string.permission_request_btn),
                        onButtonClick = onRequestNotificationPermission
                    )
                }
            }

            // Empty State
            if (uiState.reminders.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = stringResource(R.string.alarm_icon),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = stringResource(R.string.empty_reminders_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_reminders_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Reminder Cards List with Animations
                items(
                    items = uiState.reminders,
                    key = { it.id }
                ) { reminder ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                        modifier = Modifier.animateItem()
                    ) {
                        ReminderCard(
                            reminder = reminder,
                            isMasterEnabled = uiState.isMasterEnabled,
                            onToggle = { isEnabled -> viewModel.toggleReminder(reminder, isEnabled) },
                            onClick = { onEditReminderClick(reminder.id) }
                        )
                    }
                }
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    isMasterEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEffectivelyActive = reminder.isEnabled && isMasterEnabled

    TabletCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.formattedTime(),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isEffectivelyActive) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                val labelText = if (reminder.label.isNotBlank()) {
                    reminder.label
                } else {
                    stringResource(R.string.default_reminder_label)
                }
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isEffectivelyActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getDaysSummaryText(reminder.daysOfWeek),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            AnimatedSwitch(
                checked = reminder.isEnabled,
                onCheckedChange = onToggle,
                enabled = isMasterEnabled
            )
        }
    }
}

@Composable
fun getDaysSummaryText(daysMask: Int): String {
    return when (daysMask) {
        Reminder.ALL_DAYS_MASK -> stringResource(R.string.repeat_every_day)
        Reminder.WEEKDAYS_MASK -> stringResource(R.string.repeat_weekdays)
        Reminder.WEEKENDS_MASK -> stringResource(R.string.repeat_weekends)
        0 -> stringResource(R.string.repeat_never)
        else -> {
            val days = mutableListOf<String>()
            if ((daysMask and Reminder.MON) != 0) days.add(stringResource(R.string.day_mon))
            if ((daysMask and Reminder.TUE) != 0) days.add(stringResource(R.string.day_tue))
            if ((daysMask and Reminder.WED) != 0) days.add(stringResource(R.string.day_wed))
            if ((daysMask and Reminder.THU) != 0) days.add(stringResource(R.string.day_thu))
            if ((daysMask and Reminder.FRI) != 0) days.add(stringResource(R.string.day_fri))
            if ((daysMask and Reminder.SAT) != 0) days.add(stringResource(R.string.day_sat))
            if ((daysMask and Reminder.SUN) != 0) days.add(stringResource(R.string.day_sun))
            days.joinToString(", ")
        }
    }
}
