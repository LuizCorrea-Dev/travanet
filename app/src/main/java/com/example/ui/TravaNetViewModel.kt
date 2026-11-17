package com.example.ui

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TravaNetApplication
import com.example.data.db.BlockingProfileEntity
import com.example.data.db.ProfileWithSchedules
import com.example.data.db.ScheduleRuleEntity
import com.example.data.prefs.ParentalControlPrefs
import com.example.domain.EffectiveLockState
import com.example.domain.LockReason
import com.example.service.DeviceNetworkSnapshot
import com.example.service.LockCoordinator
import com.example.service.ParentalDeviceAdminReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NetworkLockScope(val blockWifi: Boolean, val blockMobile: Boolean, val label: String) {
    BOTH(true, true, "Wi-Fi + Móvel"),
    WIFI_ONLY(true, false, "Só Wi-Fi"),
    MOBILE_ONLY(false, true, "Só Móvel"),
}

sealed interface ProtectedAction {
    data object ImmediateUnlock : ProtectedAction
    data class PauseProtection(val durationMinutes: Int) : ProtectedAction
    data class DisableProfile(val profileId: Long) : ProtectedAction
    data class OpenProfileForEdit(val profileId: Long) : ProtectedAction
    data class DeleteProfile(val profileId: Long) : ProtectedAction
    data class DisableRule(val ruleId: Long) : ProtectedAction
    data object DisableAntiTamper : ProtectedAction
    data object ChangePin : ProtectedAction
    data object DeactivateDeviceAdmin : ProtectedAction
    data object UninstallApp : ProtectedAction
}

sealed interface PinModalMode {
    data class VerifyForAction(
        val action: ProtectedAction,
        val title: String,
        val subtitle: String,
    ) : PinModalMode

    data object SetNewPin : PinModalMode
}

data class SystemReadiness(
    val isVpnPrepared: Boolean = false,
    val isNotificationGranted: Boolean = true,
    val isDeviceAdminActive: Boolean = false,
)

data class TravaNetUiState(
    val prefs: ParentalControlPrefs = ParentalControlPrefs(),
    val profiles: List<ProfileWithSchedules> = emptyList(),
    val lockState: EffectiveLockState = EffectiveLockState(
        isBlocked = false,
        blockWifi = false,
        blockMobileData = false,
        reason = LockReason.Unlocked,
        isScheduleBypassed = false,
        scheduleBypassRemainingMs = 0L,
        nextTransitionEpochMs = null,
        nextScheduledLabel = null,
    ),
    val networkSnapshot: DeviceNetworkSnapshot = DeviceNetworkSnapshot(),
    val selectedQuickScope: NetworkLockScope = NetworkLockScope.BOTH,
    val systemReadiness: SystemReadiness = SystemReadiness(),
    val isLoaded: Boolean = false,
)

sealed interface UiEvent {
    data object RequestVpnPermission : UiEvent
    data class ShowMessage(val message: String) : UiEvent
    data class NavigateToProfileEditor(val profileId: Long) : UiEvent
    data object RequestDeactivateAdmin : UiEvent
    data object LaunchUninstall : UiEvent
}

class TravaNetViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TravaNetApplication
    private val repository = app.repository
    private val networkMonitor = app.networkMonitor

    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    private val _selectedQuickScope = MutableStateFlow(NetworkLockScope.BOTH)
    private val _systemReadiness = MutableStateFlow(readSystemReadiness())

    private val _pinModalMode = MutableStateFlow<PinModalMode?>(null)
    val pinModalMode: StateFlow<PinModalMode?> = _pinModalMode.asStateFlow()

    private val _pinErrorMessage = MutableStateFlow<String?>(null)
    val pinErrorMessage: StateFlow<String?> = _pinErrorMessage.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<TravaNetUiState> = combine(
        repository.prefsFlow,
        repository.profilesFlow,
        repository.observeEffectiveLockState(tickerFlow),
        networkMonitor.snapshot,
        combine(_selectedQuickScope, _systemReadiness) { scope, readiness -> scope to readiness },
    ) { prefs, profiles, lockState, netSnapshot, (scope, readiness) ->
        TravaNetUiState(
            prefs = prefs,
            profiles = profiles,
            lockState = lockState,
            networkSnapshot = netSnapshot,
            selectedQuickScope = scope,
            systemReadiness = readiness,
            isLoaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TravaNetUiState(),
    )

    fun refreshSystemReadiness() {
        _systemReadiness.value = readSystemReadiness()
        networkMonitor.refresh()
    }

    fun selectQuickScope(scope: NetworkLockScope) {
        _selectedQuickScope.value = scope
    }

    fun createInitialPin(pin: String) {
        viewModelScope.launch {
            repository.setPin(pin)
            _events.emit(UiEvent.ShowMessage("Senha parental configurada com sucesso."))
        }
    }

    fun completeOnboardingShield() {
        viewModelScope.launch {
            repository.markOnboardingShieldSeen()
            LockCoordinator.syncProtection(app)
        }
    }

    fun startQuickBlock(durationMinutes: Int) {
        val scope = _selectedQuickScope.value
        viewModelScope.launch {
            repository.startQuickBlock(
                durationMinutes = durationMinutes,
                blockWifi = scope.blockWifi,
                blockMobile = scope.blockMobile,
            )
            syncAndPromptVpnIfNeeded()
            _events.emit(
                UiEvent.ShowMessage(
                    "Bloqueio rápido de ${formatDurationLabel(durationMinutes)} ativado.",
                ),
            )
        }
    }

    fun activateTotalLock() {
        val scope = _selectedQuickScope.value
        viewModelScope.launch {
            repository.setTotalLock(
                active = true,
                blockWifi = scope.blockWifi,
                blockMobile = scope.blockMobile,
            )
            syncAndPromptVpnIfNeeded()
            _events.emit(UiEvent.ShowMessage("Bloqueio total ativado."))
        }
    }

    fun requestImmediateUnlock() {
        _pinErrorMessage.value = null
        _pinModalMode.value = PinModalMode.VerifyForAction(
            action = ProtectedAction.ImmediateUnlock,
            title = "Desbloqueio Imediato",
            subtitle = "Digite a senha parental para liberar Wi-Fi e internet móvel agora.",
        )
    }

    fun requestPauseProtection(durationMinutes: Int) {
        _pinErrorMessage.value = null
        _pinModalMode.value = PinModalMode.VerifyForAction(
            action = ProtectedAction.PauseProtection(durationMinutes),
            title = "Pausar Proteção (${formatDurationLabel(durationMinutes)})",
            subtitle = "A ação de pausa exige a senha dos pais para confirmar.",
        )
    }

    fun resumeSchedulesAfterBypass() {
        viewModelScope.launch {
            repository.clearScheduleBypass()
            syncAndPromptVpnIfNeeded()
            _events.emit(UiEvent.ShowMessage("Agendamentos reativados."))
        }
    }

    fun toggleProfileEnabled(profile: BlockingProfileEntity, targetEnabled: Boolean) {
        if (targetEnabled) {
            viewModelScope.launch {
                repository.setProfileEnabled(profile.id, true)
                syncAndPromptVpnIfNeeded()
            }
        } else {
            runWithPinIfRequired(
                action = ProtectedAction.DisableProfile(profile.id),
                title = "Desativar Perfil",
                subtitle = "Confirme a senha parental para desativar \"${profile.name}\".",
            )
        }
    }

    fun requestEditProfile(profileId: Long) {
        val currentState = uiState.value
        if (profileId == 0L || !currentState.prefs.antiTamperPinRequired) {
            viewModelScope.launch {
                _events.emit(UiEvent.NavigateToProfileEditor(profileId))
            }
            return
        }
        val targetProfile = currentState.profiles.firstOrNull { it.profile.id == profileId }
        val isActive = targetProfile?.profile?.isEnabled == true
        if (isActive) {
            runWithPinIfRequired(
                action = ProtectedAction.OpenProfileForEdit(profileId),
                title = "Editar Perfil Ativo",
                subtitle = "Digite a senha parental para editar regras de um perfil ativo.",
            )
        } else {
            viewModelScope.launch {
                _events.emit(UiEvent.NavigateToProfileEditor(profileId))
            }
        }
    }

    fun requestDeleteProfile(profile: BlockingProfileEntity) {
        runWithPinIfRequired(
            action = ProtectedAction.DeleteProfile(profile.id),
            title = "Excluir Perfil",
            subtitle = "Digite a senha parental para excluir \"${profile.name}\".",
        )
    }

    fun toggleRuleEnabled(rule: ScheduleRuleEntity, targetEnabled: Boolean) {
        if (targetEnabled) {
            viewModelScope.launch {
                repository.setRuleEnabled(rule.id, true)
                syncAndPromptVpnIfNeeded()
            }
        } else {
            runWithPinIfRequired(
                action = ProtectedAction.DisableRule(rule.id),
                title = "Desativar Horário",
                subtitle = "Digite a senha parental para desativar este agendamento.",
            )
        }
    }

    fun toggleAntiTamper(targetEnabled: Boolean) {
        if (targetEnabled) {
            viewModelScope.launch {
                repository.setAntiTamperPinRequired(true)
                _events.emit(UiEvent.ShowMessage("Trava anti-desligamento ativada."))
            }
        } else {
            _pinErrorMessage.value = null
            _pinModalMode.value = PinModalMode.VerifyForAction(
                action = ProtectedAction.DisableAntiTamper,
                title = "Desativar Trava de Senha",
                subtitle = "Confirme a senha parental para permitir alterações sem senha.",
            )
        }
    }

    fun requestChangePin() {
        _pinErrorMessage.value = null
        _pinModalMode.value = PinModalMode.VerifyForAction(
            action = ProtectedAction.ChangePin,
            title = "Alterar Senha Parental",
            subtitle = "Primeiro, digite sua senha atual.",
        )
    }

    fun requestDeactivateDeviceAdmin() {
        _pinErrorMessage.value = null
        _pinModalMode.value = PinModalMode.VerifyForAction(
            action = ProtectedAction.DeactivateDeviceAdmin,
            title = "Desativar Proteção",
            subtitle = "Confirme a senha dos pais para remover a proteção contra desinstalação.",
        )
    }

    fun requestUninstallApp() {
        _pinErrorMessage.value = null
        _pinModalMode.value = PinModalMode.VerifyForAction(
            action = ProtectedAction.UninstallApp,
            title = "Desinstalar TravaNet",
            subtitle = "Digite a senha dos pais para liberar e desinstalar o aplicativo.",
        )
    }

    fun submitPinForModal(pin: String) {
        val mode = _pinModalMode.value ?: return
        viewModelScope.launch {
            when (mode) {
                is PinModalMode.SetNewPin -> {
                    repository.setPin(pin)
                    _pinModalMode.value = null
                    _pinErrorMessage.value = null
                    _events.emit(UiEvent.ShowMessage("Nova senha parental salva."))
                }

                is PinModalMode.VerifyForAction -> {
                    val valid = repository.verifyPin(pin)
                    if (!valid) {
                        _pinErrorMessage.value = "Senha incorreta. Tente novamente."
                        return@launch
                    }
                    _pinErrorMessage.value = null
                    if (mode.action is ProtectedAction.ChangePin) {
                        _pinModalMode.value = PinModalMode.SetNewPin
                        return@launch
                    }
                    _pinModalMode.value = null
                    executeProtectedAction(mode.action)
                }
            }
        }
    }

    fun dismissPinModal() {
        _pinErrorMessage.value = null
        _pinModalMode.value = null
    }

    fun observeProfile(profileId: Long) = repository.observeProfileById(profileId)

    suspend fun saveProfileAndReturnId(profile: BlockingProfileEntity): Long {
        val id = repository.createOrUpdateProfile(profile)
        syncAndPromptVpnIfNeeded()
        return id
    }

    fun saveRule(rule: ScheduleRuleEntity) {
        viewModelScope.launch {
            repository.saveRule(rule)
            syncAndPromptVpnIfNeeded()
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            repository.deleteRule(ruleId)
            syncAndPromptVpnIfNeeded()
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        refreshSystemReadiness()
        viewModelScope.launch {
            if (granted) {
                LockCoordinator.syncProtection(app)
                _events.emit(UiEvent.ShowMessage("Firewall local ativado."))
            } else {
                _events.emit(
                    UiEvent.ShowMessage(
                        "Permissão de VPN necessária para bloquear Wi-Fi e internet móvel.",
                    ),
                )
            }
        }
    }

    private fun runWithPinIfRequired(
        action: ProtectedAction,
        title: String,
        subtitle: String,
    ) {
        if (uiState.value.prefs.antiTamperPinRequired) {
            _pinErrorMessage.value = null
            _pinModalMode.value = PinModalMode.VerifyForAction(
                action = action,
                title = title,
                subtitle = subtitle,
            )
        } else {
            viewModelScope.launch { executeProtectedAction(action) }
        }
    }

    private suspend fun executeProtectedAction(action: ProtectedAction) {
        when (action) {
            ProtectedAction.ImmediateUnlock -> {
                repository.unlockImmediate()
                LockCoordinator.syncProtection(app)
                _events.emit(UiEvent.ShowMessage("Internet desbloqueada imediatamente."))
            }

            is ProtectedAction.PauseProtection -> {
                repository.pauseProtection(action.durationMinutes)
                LockCoordinator.syncProtection(app)
                _events.emit(
                    UiEvent.ShowMessage(
                        "Proteção pausada por ${formatDurationLabel(action.durationMinutes)}.",
                    ),
                )
            }

            is ProtectedAction.DisableProfile -> {
                repository.setProfileEnabled(action.profileId, false)
                LockCoordinator.syncProtection(app)
                _events.emit(UiEvent.ShowMessage("Perfil desativado."))
            }

            is ProtectedAction.OpenProfileForEdit -> {
                _events.emit(UiEvent.NavigateToProfileEditor(action.profileId))
            }

            is ProtectedAction.DeleteProfile -> {
                repository.deleteProfile(action.profileId)
                LockCoordinator.syncProtection(app)
                _events.emit(UiEvent.ShowMessage("Perfil excluído."))
            }

            is ProtectedAction.DisableRule -> {
                repository.setRuleEnabled(action.ruleId, false)
                LockCoordinator.syncProtection(app)
            }

            ProtectedAction.DisableAntiTamper -> {
                repository.setAntiTamperPinRequired(false)
                _events.emit(UiEvent.ShowMessage("Trava anti-desligamento desativada."))
            }

            ProtectedAction.ChangePin -> Unit

            ProtectedAction.DeactivateDeviceAdmin -> {
                _events.emit(UiEvent.RequestDeactivateAdmin)
            }

            ProtectedAction.UninstallApp -> {
                repository.unlockImmediate(bypassHours = 0)
                LockCoordinator.syncProtection(app)
                _events.emit(UiEvent.LaunchUninstall)
            }
        }
    }

    private suspend fun syncAndPromptVpnIfNeeded() {
        val state = repository.evaluateCurrentLockState()
        val readiness = readSystemReadiness()
        _systemReadiness.value = readiness
        if (state.isBlocked && !readiness.isVpnPrepared) {
            _events.emit(UiEvent.RequestVpnPermission)
        } else {
            LockCoordinator.syncProtection(app)
        }
    }

    private fun readSystemReadiness(): SystemReadiness {
        val vpnPrepared = try {
            VpnService.prepare(app) == null
        } catch (_: Exception) {
            false
        }
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                app,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val adminActive = try {
            val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val comp = ComponentName(app, ParentalDeviceAdminReceiver::class.java)
            dpm.isAdminActive(comp)
        } catch (_: Exception) {
            false
        }
        return SystemReadiness(
            isVpnPrepared = vpnPrepared,
            isNotificationGranted = notifGranted,
            isDeviceAdminActive = adminActive,
        )
    }

    private fun formatDurationLabel(minutes: Int): String = when (minutes) {
        30 -> "30 min"
        60 -> "1 hora"
        120 -> "2 horas"
        else -> "$minutes min"
    }
}
