package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.TravaNetUiState

@Composable
fun SecurityScreen(
    uiState: TravaNetUiState,
    onChangePin: () -> Unit,
    onToggleAntiTamper: (Boolean) -> Unit,
    onRequestVpnPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSystemVpnSettings: () -> Unit,
    onToggleDeviceAdmin: () -> Unit,
    onUninstallApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Segurança e Blindagem",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Proteja a senha parental e configure as travas do sistema Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Controle de Senha",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(onClick = onChangePin)
                            .padding(vertical = 4.dp)
                            .testTag("change_pin_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(icon = Icons.Filled.Password)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alterar Senha Parental (PIN)",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Atualize o código de 4 dígitos exigido para desbloqueio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(icon = Icons.Filled.Lock)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trava Anti-Desligamento",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Exige senha para desativar ou alterar perfis e horários.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.prefs.antiTamperPinRequired,
                            onCheckedChange = onToggleAntiTamper,
                            modifier = Modifier.testTag("anti_tamper_switch"),
                        )
                    }
                }
            }
        }

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Blindagem do Sistema",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    HardeningItemRow(
                        icon = Icons.Filled.Security,
                        title = "Firewall Local (VPN)",
                        subtitle = if (uiState.systemReadiness.isVpnPrepared) {
                            "Autorizado para bloquear tráfego Wi-Fi e dados móveis."
                        } else {
                            "Autorize o perfil VPN local para aplicar bloqueios."
                        },
                        actionLabel = if (uiState.systemReadiness.isVpnPrepared) "Ativo" else "Ativar",
                        isReady = uiState.systemReadiness.isVpnPrepared,
                        onAction = onRequestVpnPermission,
                        testTag = "security_vpn_button",
                    )

                    HardeningItemRow(
                        icon = Icons.Filled.Notifications,
                        title = "Notificação de Proteção",
                        subtitle = if (uiState.systemReadiness.isNotificationGranted) {
                            "Permissão concedida para manter o bloqueio estável."
                        } else {
                            "Necessária no Android 13+ para exibir estado do bloqueio."
                        },
                        actionLabel = if (uiState.systemReadiness.isNotificationGranted) "Ativo" else "Permitir",
                        isReady = uiState.systemReadiness.isNotificationGranted,
                        onAction = onRequestNotificationPermission,
                        testTag = "security_notification_button",
                    )

                    HardeningItemRow(
                        icon = Icons.Filled.VpnLock,
                        title = "VPN Sempre Ativa no Android",
                        subtitle = "Nas configurações de VPN, ative \"VPN sempre ativa\" e \"Bloquear conexões sem VPN\" para impedir bypass.",
                        actionLabel = "Abrir",
                        isReady = false,
                        isExternal = true,
                        onAction = onOpenSystemVpnSettings,
                        testTag = "security_system_vpn_button",
                    )

                    HardeningItemRow(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "Proteção contra Desinstalação",
                        subtitle = if (uiState.systemReadiness.isDeviceAdminActive) {
                            "Ativa: o Android bloqueia desinstalar sem a senha dos pais."
                        } else {
                            "Impede desinstalar o app sem a senha dos pais."
                        },
                        actionLabel = if (uiState.systemReadiness.isDeviceAdminActive) "Desativar" else "Ativar",
                        isReady = uiState.systemReadiness.isDeviceAdminActive,
                        onAction = onToggleDeviceAdmin,
                        testTag = "security_device_admin_button",
                    )
                }
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(icon = Icons.Filled.DeleteForever)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Desinstalar Aplicativo",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Exige a senha dos pais para desativar as travas e remover o TravaNet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onUninstallApp,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_uninstall_button"),
                    ) {
                        Text("Desinstalar TravaNet (Requer Senha)")
                    }
                }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun HardeningItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    isReady: Boolean,
    isExternal: Boolean = false,
    onAction: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(icon = icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = onAction,
            modifier = Modifier.testTag(testTag),
        ) {
            Text(actionLabel)
            if (isExternal) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp),
                )
            }
        }
    }
}
