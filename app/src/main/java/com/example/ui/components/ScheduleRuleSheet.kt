package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.ScheduleRuleEntity
import com.example.data.db.ScheduleRuleMode
import com.example.domain.DayMask
import com.example.domain.LockStateEvaluator
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRuleEditorSheet(
    profileId: Long,
    existingRule: ScheduleRuleEntity?,
    onSave: (ScheduleRuleEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var label by remember(existingRule) { mutableStateOf(existingRule?.label.orEmpty()) }
    var ruleMode by remember(existingRule) {
        mutableStateOf(
            ScheduleRuleMode.fromKey(existingRule?.ruleMode ?: ScheduleRuleMode.BLOCK_WINDOW.key),
        )
    }
    var daysMask by remember(existingRule) {
        mutableIntStateOf(existingRule?.daysMask ?: DayMask.WEEKDAYS)
    }
    var startMinutes by remember(existingRule) {
        mutableIntStateOf(existingRule?.startMinutes ?: (22 * 60))
    }
    var endMinutes by remember(existingRule) {
        mutableIntStateOf(existingRule?.endMinutes ?: (7 * 60))
    }

    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = if (existingRule == null) "Novo Agendamento" else "Editar Agendamento",
                style = MaterialTheme.typography.headlineMedium,
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nome da regra (opcional)") },
                placeholder = { Text("Ex.: Noite escolar, Horário livre") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rule_label_input"),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de regra",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RuleModeOptionCard(
                        selected = ruleMode == ScheduleRuleMode.BLOCK_WINDOW,
                        title = "Bloquear neste horário",
                        subtitle = "Corta Wi-Fi/dados na janela",
                        icon = { Icon(Icons.Filled.Block, contentDescription = null) },
                        onClick = { ruleMode = ScheduleRuleMode.BLOCK_WINDOW },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_mode_block"),
                    )
                    RuleModeOptionCard(
                        selected = ruleMode == ScheduleRuleMode.ALLOW_WINDOW,
                        title = "Uso permitido",
                        subtitle = "Libera só nesta janela",
                        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                        onClick = { ruleMode = ScheduleRuleMode.ALLOW_WINDOW },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_mode_allow"),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Dias da semana",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DayMask.orderedBits.forEachIndexed { index, bit ->
                        val selected = (daysMask and bit) != 0
                        Surface(
                            shape = CircleShape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable {
                                    daysMask = if (selected) daysMask and bit.inv() else daysMask or bit
                                }
                                .testTag("rule_day_chip_$index"),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = DayMask.shortLabelsPt[index],
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = daysMask == DayMask.WEEKDAYS,
                        onClick = { daysMask = DayMask.WEEKDAYS },
                        label = { Text("Seg–Sex") },
                    )
                    FilterChip(
                        selected = daysMask == DayMask.WEEKENDS,
                        onClick = { daysMask = DayMask.WEEKENDS },
                        label = { Text("Fim de sem.") },
                    )
                    FilterChip(
                        selected = daysMask == DayMask.ALL_DAYS,
                        onClick = { daysMask = DayMask.ALL_DAYS },
                        label = { Text("Todos") },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Horário",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimeSelectorCard(
                        title = "Início",
                        minutes = startMinutes,
                        onClick = { pickingStart = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_start_time_button"),
                    )
                    TimeSelectorCard(
                        title = "Término",
                        minutes = endMinutes,
                        onClick = { pickingEnd = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rule_end_time_button"),
                    )
                }

                if (endMinutes <= startMinutes && startMinutes != endMinutes) {
                    Text(
                        text = "Janela noturna: atravessa a meia-noite até o dia seguinte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val effectiveMask = if (daysMask == 0) DayMask.ALL_DAYS else daysMask
                        val autoLabel = label.trim().ifBlank {
                            val modePrefix = if (ruleMode == ScheduleRuleMode.ALLOW_WINDOW) {
                                "Uso liberado"
                            } else {
                                "Bloqueio"
                            }
                            "$modePrefix • ${DayMask.formatDaysSummary(effectiveMask)}"
                        }
                        onSave(
                            ScheduleRuleEntity(
                                id = existingRule?.id ?: 0L,
                                profileId = profileId,
                                label = autoLabel,
                                ruleMode = ruleMode.key,
                                daysMask = effectiveMask,
                                startMinutes = startMinutes,
                                endMinutes = endMinutes,
                                isEnabled = existingRule?.isEnabled ?: true,
                            ),
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_rule_button"),
                ) {
                    Text("Salvar Horário")
                }
            }
        }
    }

    if (pickingStart) {
        TimePickerModalDialog(
            title = "Horário de início",
            initialMinutes = startMinutes,
            onConfirm = {
                startMinutes = it
                pickingStart = false
            },
            onDismiss = { pickingStart = false },
        )
    }

    if (pickingEnd) {
        TimePickerModalDialog(
            title = "Horário de término",
            initialMinutes = endMinutes,
            onConfirm = {
                endMinutes = it
                pickingEnd = false
            },
            onDismiss = { pickingEnd = false },
        )
    }
}

@Composable
private fun RuleModeOptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.size(22.dp),
            ) {
                icon()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimeSelectorCard(
    title: String,
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = LockStateEvaluator.formatMinutesOfDay(minutes),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModalDialog(
    title: String,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
