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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.BlockingProfileEntity
import com.example.data.db.ScheduleRuleEntity
import com.example.data.db.ScheduleRuleMode
import com.example.domain.DayMask
import com.example.domain.LockStateEvaluator
import com.example.ui.TravaNetViewModel
import com.example.ui.components.ScheduleRuleEditorSheet
import com.example.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    profileId: Long,
    viewModel: TravaNetViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val existingWithSchedules by viewModel.observeProfile(profileId)
        .collectAsStateWithLifecycle(initialValue = null)

    var initialized by remember(profileId) { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("shield") }
    var blockWifi by remember { mutableStateOf(true) }
    var blockMobileData by remember { mutableStateOf(true) }
    var isEnabled by remember { mutableStateOf(true) }
    val pendingNewRules = remember { mutableStateListOf<ScheduleRuleEntity>() }

    var editingRule by remember { mutableStateOf<ScheduleRuleEntity?>(null) }
    var showRuleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(existingWithSchedules) {
        val loaded = existingWithSchedules
        if (!initialized && loaded != null) {
            name = loaded.profile.name
            description = loaded.profile.description
            iconKey = loaded.profile.iconKey
            blockWifi = loaded.profile.blockWifi
            blockMobileData = loaded.profile.blockMobileData
            isEnabled = loaded.profile.isEnabled
            initialized = true
        }
    }

    val currentRules = if (profileId == 0L) {
        pendingNewRules.toList()
    } else {
        existingWithSchedules?.schedules.orEmpty()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (profileId == 0L) "Novo Perfil" else "Editar Perfil")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("editor_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do perfil") },
                        placeholder = { Text("Ex.: Horário de Dormir") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição (opcional)") },
                        placeholder = { Text("Ex.: Cortar internet durante a noite") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_desc_input"),
                    )

                    Text(
                        text = "Ícone do perfil",
                        style = MaterialTheme.typography.titleSmall,
                    )

                    val iconOptions = listOf(
                        "shield" to "Geral",
                        "bedtime" to "Dormir",
                        "school" to "Estudos",
                        "focus" to "Foco",
                        "family" to "Família",
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        iconOptions.forEach { (key, label) ->
                            val selected = iconKey == key
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { iconKey = key },
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = profileIconForKey(key),
                                        contentDescription = label,
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Conexões a bloquear",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Filled.Wifi, contentDescription = null)
                                Text("Bloquear Wi-Fi", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = blockWifi,
                                onCheckedChange = { blockWifi = it },
                                modifier = Modifier.testTag("editor_block_wifi_switch"),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Filled.SignalCellularAlt, contentDescription = null)
                                Text("Bloquear Internet Móvel", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = blockMobileData,
                                onCheckedChange = { blockMobileData = it },
                                modifier = Modifier.testTag("editor_block_mobile_switch"),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Perfil ativo ao salvar", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { isEnabled = it },
                                modifier = Modifier.testTag("editor_profile_enabled_switch"),
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Horários Agendados",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    FilledTonalButton(
                        onClick = {
                            editingRule = null
                            showRuleSheet = true
                        },
                        modifier = Modifier.testTag("add_rule_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adicionar")
                    }
                }
            }

            if (currentRules.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                    ) {
                        Text(
                            text = "Nenhum horário configurado. Toque em \"Adicionar\" para definir dias e horários.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(currentRules) { rule ->
                    val modeLabel = if (ScheduleRuleMode.fromKey(rule.ruleMode) == ScheduleRuleMode.ALLOW_WINDOW) {
                        "Uso permitido"
                    } else {
                        "Bloqueio"
                    }
                    val start = LockStateEvaluator.formatMinutesOfDay(rule.startMinutes)
                    val end = LockStateEvaluator.formatMinutesOfDay(rule.endMinutes)
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                editingRule = rule
                                showRuleSheet = true
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.label,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "$modeLabel • ${DayMask.formatDaysSummary(rule.daysMask)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "$start–$end",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(
                                onClick = {
                                    if (profileId == 0L) {
                                        pendingNewRules.remove(rule)
                                    } else {
                                        viewModel.deleteRule(rule.id)
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Remover horário",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            val cleanName = name.trim().ifBlank { "Perfil Personalizado" }
                            val savedId = viewModel.saveProfileAndReturnId(
                                BlockingProfileEntity(
                                    id = profileId,
                                    name = cleanName,
                                    description = description.trim(),
                                    iconKey = iconKey,
                                    blockWifi = blockWifi,
                                    blockMobileData = blockMobileData,
                                    isEnabled = isEnabled,
                                    createdAt = existingWithSchedules?.profile?.createdAt
                                        ?: System.currentTimeMillis(),
                                ),
                            )
                            if (profileId == 0L) {
                                pendingNewRules.forEach { pending ->
                                    viewModel.saveRule(pending.copy(profileId = savedId))
                                }
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .height(52.dp)
                        .testTag("save_profile_button"),
                ) {
                    Text("Salvar Perfil", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showRuleSheet) {
        ScheduleRuleEditorSheet(
            profileId = profileId,
            existingRule = editingRule,
            onSave = { savedRule ->
                if (profileId == 0L) {
                    val existingIdx = editingRule?.let { pendingNewRules.indexOf(it) } ?: -1
                    if (existingIdx >= 0) {
                        pendingNewRules[existingIdx] = savedRule
                    } else {
                        pendingNewRules.add(savedRule)
                    }
                } else {
                    viewModel.saveRule(savedRule)
                }
                showRuleSheet = false
            },
            onDismiss = { showRuleSheet = false },
        )
    }
}
