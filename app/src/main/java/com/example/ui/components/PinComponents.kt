package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.PinModalMode

private const val PIN_LENGTH = 4

@Composable
fun PinSetupScreen(
    onPinConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var mismatchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentInput) {
        if (currentInput.length == PIN_LENGTH) {
            if (firstPin == null) {
                firstPin = currentInput
                currentInput = ""
                mismatchError = null
            } else if (firstPin == currentInput) {
                onPinConfirmed(currentInput)
            } else {
                mismatchError = "As senhas não conferem. Tente novamente."
                firstPin = null
                currentInput = ""
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Escudo parental TravaNet",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (firstPin == null) "Criar Senha Parental" else "Confirmar Senha Parental",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (firstPin == null) {
                        "Defina um PIN de 4 dígitos para proteger bloqueios de Wi-Fi, internet móvel e agendamentos."
                    } else {
                        "Digite o mesmo PIN novamente para confirmar."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            PinDotsIndicator(
                filledCount = currentInput.length,
                isError = mismatchError != null,
            )

            AnimatedVisibility(visible = mismatchError != null) {
                Text(
                    text = mismatchError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            PinNumericKeypad(
                onDigit = { digit ->
                    if (currentInput.length < PIN_LENGTH) {
                        mismatchError = null
                        currentInput += digit
                    }
                },
                onBackspace = {
                    if (currentInput.isNotEmpty()) {
                        currentInput = currentInput.dropLast(1)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinVerificationBottomSheet(
    mode: PinModalMode,
    errorMessage: String?,
    onSubmitPin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember(mode) { mutableStateOf("") }
    var firstNewPin by remember(mode) { mutableStateOf<String?>(null) }
    var localError by remember(mode) { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            input = ""
        }
    }

    LaunchedEffect(input) {
        if (input.length == PIN_LENGTH) {
            when (mode) {
                is PinModalMode.VerifyForAction -> onSubmitPin(input)
                is PinModalMode.SetNewPin -> {
                    if (firstNewPin == null) {
                        firstNewPin = input
                        input = ""
                        localError = null
                    } else if (firstNewPin == input) {
                        onSubmitPin(input)
                    } else {
                        localError = "As senhas não conferem. Digite novamente."
                        firstNewPin = null
                        input = ""
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Text(
                        text = when (mode) {
                            is PinModalMode.VerifyForAction -> mode.title
                            is PinModalMode.SetNewPin -> {
                                if (firstNewPin == null) "Nova Senha Parental" else "Confirmar Nova Senha"
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("pin_modal_close_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fechar teclado de senha",
                    )
                }
            }

            Text(
                text = when (mode) {
                    is PinModalMode.VerifyForAction -> mode.subtitle
                    is PinModalMode.SetNewPin -> {
                        if (firstNewPin == null) {
                            "Digite o novo PIN de 4 dígitos."
                        } else {
                            "Confirme o novo PIN digitando novamente."
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            val activeError = localError ?: errorMessage
            PinDotsIndicator(
                filledCount = input.length,
                isError = activeError != null,
            )

            AnimatedVisibility(visible = activeError != null) {
                Text(
                    text = activeError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            PinNumericKeypad(
                onDigit = { digit ->
                    if (input.length < PIN_LENGTH) {
                        localError = null
                        input += digit
                    }
                },
                onBackspace = {
                    if (input.isNotEmpty()) {
                        input = input.dropLast(1)
                    }
                },
            )
        }
    }
}

@Composable
private fun PinDotsIndicator(
    filledCount: Int,
    isError: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        repeat(PIN_LENGTH) { index ->
            val filled = index < filledCount
            val dotColor = when {
                isError -> MaterialTheme.colorScheme.error
                filled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val borderColor = when {
                isError -> MaterialTheme.colorScheme.error
                filled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.5.dp, borderColor, CircleShape),
            )
        }
    }
}

@Composable
private fun PinNumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "BACK"),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(68.dp))
                        "BACK" -> {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(68.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .clickable(onClick = onBackspace)
                                    .testTag("pin_key_backspace"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Apagar dígito",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        else -> {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .size(68.dp)
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .clickable { onDigit(key) }
                                    .testTag("pin_key_$key"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingShieldScreen(
    isVpnPrepared: Boolean,
    isDeviceAdminActive: Boolean,
    onActivateVpn: () -> Unit,
    onActivateDeviceAdmin: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Blindagem Inicial",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Ative as proteções do sistema para impedir que o bloqueio seja pausado ou o app desinstalado sem sua senha.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ShieldSetupCard(
                title = "1. Firewall Local (VPN)",
                description = "Permite cortar Wi-Fi e dados móveis instantaneamente.",
                isReady = isVpnPrepared,
                actionText = if (isVpnPrepared) "Ativado" else "Ativar VPN",
                onAction = onActivateVpn,
                testTag = "onboarding_vpn_button",
            )

            ShieldSetupCard(
                title = "2. Trava contra Desinstalação",
                description = "Impede desinstalar o TravaNet sem a senha dos pais.",
                isReady = isDeviceAdminActive,
                actionText = if (isDeviceAdminActive) "Ativado" else "Ativar Trava",
                onAction = onActivateDeviceAdmin,
                testTag = "onboarding_admin_button",
            )

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.material3.Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_continue_button"),
            ) {
                Text(
                    text = if (isVpnPrepared && isDeviceAdminActive) {
                        "Ir para o Painel"
                    } else {
                        "Continuar para o Painel"
                    },
                )
            }
        }
    }
}

@Composable
private fun ShieldSetupCard(
    title: String,
    description: String,
    isReady: Boolean,
    actionText: String,
    onAction: () -> Unit,
    testTag: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isReady) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.FilledTonalButton(
                onClick = onAction,
                enabled = !isReady,
                modifier = Modifier.testTag(testTag),
            ) {
                Text(actionText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseDurationBottomSheet(
    onSelectDuration: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Pausar Proteção do App",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Escolha por quanto tempo deseja pausar os bloqueios e agendamentos. Será solicitada a senha dos pais.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val options = listOf(
                15 to "Pausar por 15 minutos",
                30 to "Pausar por 30 minutos",
                60 to "Pausar por 1 hora",
                120 to "Pausar por 2 horas",
            )
            options.forEach { (minutes, label) ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelectDuration(minutes) }
                        .testTag("pause_option_${minutes}m"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = label, style = MaterialTheme.typography.titleSmall)
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Requer senha dos pais",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
