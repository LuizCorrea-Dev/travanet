package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.LockReason
import com.example.ui.NetworkLockScope
import com.example.ui.TravaNetUiState
import com.example.ui.theme.JetBrainsMono

@Composable
fun DashboardScreen(
    uiState: TravaNetUiState,
    onSelectScope: (NetworkLockScope) -> Unit,
    onQuickBlock: (Int) -> Unit,
    onTotalLock: () -> Unit,
    onRequestImmediateUnlock: () -> Unit,
    onOpenPauseSheet: () -> Unit,
    onResumeSchedules: () -> Unit,
    onRequestVpnPermission: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "TravaNet",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Controle Parental de Conexão",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.systemReadiness.isVpnPrepared) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (uiState.systemReadiness.isVpnPrepared) {
                                    Icons.Filled.Shield
                                } else {
                                    Icons.Filled.Warning
                                },
                                contentDescription = null,
                                tint = if (uiState.systemReadiness.isVpnPrepared) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (uiState.systemReadiness.isVpnPrepared) "Pronto" else "Sem VPN",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.systemReadiness.isVpnPrepared) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = !uiState.systemReadiness.isVpnPrepared) {
                    VpnPermissionCallout(onGrantClick = onRequestVpnPermission)
                }

                AnimatedVisibility(visible = !uiState.systemReadiness.isDeviceAdminActive) {
                    AdminProtectionCallout(onActivateClick = onActivateDeviceAdmin)
                }

                HeroLockStatusCard(
                    uiState = uiState,
                    onRequestImmediateUnlock = onRequestImmediateUnlock,
                    onOpenPauseSheet = onOpenPauseSheet,
                    onResumeSchedules = onResumeSchedules,
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Bloqueio Rápido e Trava Total",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NetworkLockScope.entries.forEach { scope ->
                        FilterChip(
                            selected = uiState.selectedQuickScope == scope,
                            onClick = { onSelectScope(scope) },
                            label = { Text(scope.label) },
                            modifier = Modifier.testTag("scope_chip_${scope.name.lowercase()}"),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickLockActionCard(
                        title = "30 min",
                        subtitle = "Pausa rápida",
                        icon = Icons.Filled.Timer,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = { onQuickBlock(30) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_block_30m"),
                    )
                    QuickLockActionCard(
                        title = "1 hora",
                        subtitle = "Foco ou tarefa",
                        icon = Icons.Filled.HourglassBottom,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = { onQuickBlock(60) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_block_1h"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickLockActionCard(
                        title = "2 horas",
                        subtitle = "Bloqueio longo",
                        icon = Icons.Filled.Schedule,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = { onQuickBlock(120) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_block_2h"),
                    )
                    QuickLockActionCard(
                        title = "Bloqueio Total",
                        subtitle = "Até digitar a senha",
                        icon = Icons.Filled.Lock,
                        accent = MaterialTheme.colorScheme.error,
                        isDestructiveStyle = true,
                        onClick = onTotalLock,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_lock_button"),
                    )
                }
            }
        }

        item {
            ActiveProfilesSummaryCard(
                uiState = uiState,
                onNavigateToProfiles = onNavigateToProfiles,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            )
        }
    }
}

@Composable
private fun VpnPermissionCallout(
    onGrantClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ative o Firewall Local",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Necessário para cortar Wi-Fi e dados móveis sem acesso root.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = onGrantClick,
                modifier = Modifier.testTag("grant_vpn_button"),
            ) {
                Text("Ativar")
            }
        }
    }
}

@Composable
private fun AdminProtectionCallout(
    onActivateClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Proteja contra desinstalação",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Ative o Administrador do Dispositivo para exigir senha ao desinstalar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = onActivateClick,
                modifier = Modifier.testTag("activate_admin_callout_button"),
            ) {
                Text("Proteger")
            }
        }
    }
}

@Composable
private fun HeroLockStatusCard(
    uiState: TravaNetUiState,
    onRequestImmediateUnlock: () -> Unit,
    onOpenPauseSheet: () -> Unit,
    onResumeSchedules: () -> Unit,
) {
    val lockState = uiState.lockState
    val isBlocked = lockState.isBlocked

    val containerColor by animateColorAsState(
        targetValue = if (isBlocked) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "hero_bg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isBlocked) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "hero_border",
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isBlocked) 2.dp else 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isBlocked) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                                Color.Transparent,
                            ),
                        )
                    },
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isBlocked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isBlocked) Icons.Filled.Lock else Icons.Filled.CheckCircle,
                            contentDescription = if (isBlocked) "Internet bloqueada" else "Internet liberada",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (val reason = lockState.reason) {
                            is LockReason.TotalLock -> "Bloqueio Total Ativo"
                            is LockReason.QuickBlock -> "Bloqueio Rápido Ativo"
                            is LockReason.ScheduledBlock -> {
                                "Perfil: ${reason.activeProfileNames.joinToString(", ")}"
                            }
                            LockReason.Unlocked -> "Internet Liberada"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    val subtitle = when (val reason = lockState.reason) {
                        is LockReason.TotalLock -> "Trava mantida até o desbloqueio com senha."
                        is LockReason.QuickBlock -> "Tempo restante até liberação automática:"
                        is LockReason.ScheduledBlock -> {
                            reason.activeRuleLabels.joinToString(" • ")
                        }
                        LockReason.Unlocked -> {
                            lockState.nextScheduledLabel ?: "Nenhum bloqueio ativo no momento."
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (lockState.reason is LockReason.QuickBlock) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Restante",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatCountdown(lockState.reason.remainingMs),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("quick_block_countdown"),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NetworkChipStatus(
                    label = "Wi-Fi",
                    isBlocked = lockState.blockWifi,
                    isPhysicalConnected = uiState.networkSnapshot.isWifiConnected,
                    iconAllowed = Icons.Filled.Wifi,
                    iconBlocked = Icons.Filled.WifiOff,
                    modifier = Modifier.weight(1f),
                )
                NetworkChipStatus(
                    label = "Internet Móvel",
                    isBlocked = lockState.blockMobileData,
                    isPhysicalConnected = uiState.networkSnapshot.isMobileConnected,
                    iconAllowed = Icons.Filled.SignalCellularAlt,
                    iconBlocked = Icons.Filled.SignalCellularOff,
                    modifier = Modifier.weight(1f),
                )
            }

            if (isBlocked) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRequestImmediateUnlock,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("immediate_unlock_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Desbloquear Agora pela Senha",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenPauseSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pause_protection_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PauseCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pausar Proteção (Senha dos Pais)")
                    }
                }
            } else if (lockState.isScheduleBypassed) {
                OutlinedButton(
                    onClick = onResumeSchedules,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_schedules_button"),
                ) {
                    Text(
                        text = "Proteção pausada (${formatCountdown(lockState.scheduleBypassRemainingMs)}) • Retomar agora",
                    )
                }
            } else if (uiState.profiles.any { it.profile.isEnabled }) {
                OutlinedButton(
                    onClick = onOpenPauseSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pause_protection_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PauseCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pausar Agendamentos (Senha dos Pais)")
                }
            }
        }
    }
}

@Composable
private fun NetworkChipStatus(
    label: String,
    isBlocked: Boolean,
    isPhysicalConnected: Boolean,
    iconAllowed: ImageVector,
    iconBlocked: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isBlocked) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isBlocked) iconBlocked else iconAllowed,
                contentDescription = null,
                tint = if (isBlocked) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        isBlocked -> "Bloqueado"
                        isPhysicalConnected -> "Conectado"
                        else -> "Liberado"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isBlocked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickLockActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    isDestructiveStyle: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isDestructiveStyle) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isDestructiveStyle) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActiveProfilesSummaryCard(
    uiState: TravaNetUiState,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeCount = uiState.profiles.count { it.profile.isEnabled }
    val totalCount = uiState.profiles.size

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onNavigateToProfiles)
            .testTag("dashboard_profiles_card"),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Perfis e Horários Agendados",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (activeCount == 0) {
                        "$totalCount perfis cadastrados • Nenhum agendamento ativo"
                    } else {
                        "$activeCount de $totalCount perfis ativos monitorando horários"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Gerenciar perfis",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
