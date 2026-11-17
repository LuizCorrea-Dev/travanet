package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.BlockingProfileEntity
import com.example.data.db.ProfileWithSchedules
import com.example.data.db.ScheduleRuleMode
import com.example.domain.DayMask
import com.example.domain.LockStateEvaluator
import com.example.ui.theme.JetBrainsMono

@Composable
fun ProfilesScreen(
    profiles: List<ProfileWithSchedules>,
    onCreateProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onToggleProfile: (BlockingProfileEntity, Boolean) -> Unit,
    onDeleteProfile: (BlockingProfileEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProfile,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_profile_fab"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Novo perfil de bloqueio",
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Perfis de Bloqueio",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = "Configure horários de uso permitido e janelas de bloqueio por dias da semana.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (profiles.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = "Nenhum perfil cadastrado",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Toque no botão + para criar regras personalizadas de Wi-Fi e dados móveis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(profiles, key = { it.profile.id }) { item ->
                    ProfileCard(
                        item = item,
                        onEdit = { onEditProfile(item.profile.id) },
                        onToggle = { enabled -> onToggleProfile(item.profile, enabled) },
                        onDelete = { onDeleteProfile(item.profile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    item: ProfileWithSchedules,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = item.profile
    val activeBorder = if (profile.isEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, activeBorder),
        tonalElevation = if (profile.isEnabled) 2.dp else 0.dp,
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onEdit)
            .testTag("profile_card_${profile.id}"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (profile.isEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = profileIconForKey(profile.iconKey),
                            contentDescription = null,
                            tint = if (profile.isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (profile.description.isNotBlank()) {
                        Text(
                            text = profile.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Switch(
                    checked = profile.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("profile_switch_${profile.id}"),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profile.blockWifi) {
                    TagBadge(text = "Wi-Fi")
                }
                if (profile.blockMobileData) {
                    TagBadge(text = "Internet Móvel")
                }
                TagBadge(
                    text = if (item.schedules.isEmpty()) {
                        "Sem horários"
                    } else {
                        "${item.schedules.size} ${if (item.schedules.size == 1) "horário" else "horários"}"
                    },
                    isHighlight = profile.isEnabled,
                )
            }

            if (item.schedules.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.schedules.take(3).forEach { rule ->
                        val start = LockStateEvaluator.formatMinutesOfDay(rule.startMinutes)
                        val end = LockStateEvaluator.formatMinutesOfDay(rule.endMinutes)
                        val modeLabel = if (ScheduleRuleMode.fromKey(rule.ruleMode) == ScheduleRuleMode.ALLOW_WINDOW) {
                            "Uso permitido"
                        } else {
                            "Bloqueio"
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$modeLabel • ${DayMask.formatDaysSummary(rule.daysMask)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "$start–$end",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_profile_button_${profile.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar perfil ${profile.name}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_profile_button_${profile.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Excluir perfil ${profile.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagBadge(
    text: String,
    isHighlight: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = if (isHighlight) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHighlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

fun profileIconForKey(key: String): ImageVector = when (key) {
    "bedtime" -> Icons.Filled.Bedtime
    "school" -> Icons.Filled.School
    "focus" -> Icons.Filled.Psychology
    "family" -> Icons.Filled.FamilyRestroom
    else -> Icons.Filled.Shield
}
