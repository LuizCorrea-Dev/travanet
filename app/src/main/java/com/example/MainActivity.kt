package com.example

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.service.ParentalDeviceAdminReceiver
import com.example.service.ParentalVpnService
import com.example.ui.TravaNetViewModel
import com.example.ui.UiEvent
import com.example.ui.components.OnboardingShieldScreen
import com.example.ui.components.PauseDurationBottomSheet
import com.example.ui.components.PinSetupScreen
import com.example.ui.components.PinVerificationBottomSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProfileEditorScreen
import com.example.ui.screens.ProfilesScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.theme.TravaNetTheme
import kotlinx.coroutines.launch

object Routes {
    const val DASHBOARD = "dashboard"
    const val PROFILES = "profiles"
    const val SECURITY = "security"
    const val PROFILE_EDITOR = "profile_editor/{profileId}"

    fun profileEditor(profileId: Long): String = "profile_editor/$profileId"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TOP_TABS = listOf(
    NavTab(Routes.DASHBOARD, "Painel", Icons.Filled.Home, Icons.Outlined.Home),
    NavTab(Routes.PROFILES, "Perfis", Icons.Filled.Tune, Icons.Outlined.Tune),
    NavTab(Routes.SECURITY, "Segurança", Icons.Filled.Security, Icons.Outlined.Security),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TravaNetTheme {
                TravaNetApp()
            }
        }
    }
}

@Composable
fun TravaNetApp(
    viewModel: TravaNetViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinModalMode by viewModel.pinModalMode.collectAsStateWithLifecycle()
    val pinErrorMessage by viewModel.pinErrorMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPauseSheet by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshSystemReadiness()
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Notificações ativadas." else "Permissão de notificação negada.",
            )
        }
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshSystemReadiness()
    }

    fun launchVpnPermissionIfNeeded() {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            viewModel.onVpnPermissionResult(true)
        }
    }

    fun launchDeviceAdminActivation() {
        val comp = ComponentName(context, ParentalDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.device_admin_description),
            )
        }
        deviceAdminLauncher.launch(intent)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemReadiness()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                UiEvent.RequestVpnPermission -> launchVpnPermissionIfNeeded()
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.NavigateToProfileEditor -> {
                    navController.navigate(Routes.profileEditor(event.profileId))
                }
                UiEvent.RequestDeactivateAdmin -> {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val comp = ComponentName(context, ParentalDeviceAdminReceiver::class.java)
                    if (dpm.isAdminActive(comp)) {
                        dpm.removeActiveAdmin(comp)
                    }
                    viewModel.refreshSystemReadiness()
                    snackbarHostState.showSnackbar("Proteção contra desinstalação desativada.")
                }
                UiEvent.LaunchUninstall -> {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val comp = ComponentName(context, ParentalDeviceAdminReceiver::class.java)
                    if (dpm.isAdminActive(comp)) {
                        dpm.removeActiveAdmin(comp)
                    }
                    ParentalVpnService.stop(context)
                    viewModel.refreshSystemReadiness()
                    val uninstallIntent = Intent(
                        Intent.ACTION_DELETE,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(uninstallIntent)
                }
            }
        }
    }

    if (!uiState.isLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!uiState.prefs.hasPinConfigured) {
        PinSetupScreen(
            onPinConfirmed = { newPin -> viewModel.createInitialPin(newPin) },
        )
        return
    }

    if (!uiState.prefs.onboardingShieldSeen) {
        OnboardingShieldScreen(
            isVpnPrepared = uiState.systemReadiness.isVpnPrepared,
            isDeviceAdminActive = uiState.systemReadiness.isDeviceAdminActive,
            onActivateVpn = { launchVpnPermissionIfNeeded() },
            onActivateDeviceAdmin = { launchDeviceAdminActivation() },
            onContinue = { viewModel.completeOnboardingShield() },
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevelRoute = TOP_TABS.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = this.maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (isTopLevelRoute && !useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        TOP_TABS.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isTopLevelRoute && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        TOP_TABS.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}"),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            uiState = uiState,
                            onSelectScope = viewModel::selectQuickScope,
                            onQuickBlock = viewModel::startQuickBlock,
                            onTotalLock = viewModel::activateTotalLock,
                            onRequestImmediateUnlock = viewModel::requestImmediateUnlock,
                            onOpenPauseSheet = { showPauseSheet = true },
                            onResumeSchedules = viewModel::resumeSchedulesAfterBypass,
                            onRequestVpnPermission = { launchVpnPermissionIfNeeded() },
                            onActivateDeviceAdmin = { launchDeviceAdminActivation() },
                            onNavigateToProfiles = {
                                navController.navigate(Routes.PROFILES) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }

                    composable(Routes.PROFILES) {
                        ProfilesScreen(
                            profiles = uiState.profiles,
                            onCreateProfile = { viewModel.requestEditProfile(0L) },
                            onEditProfile = { id -> viewModel.requestEditProfile(id) },
                            onToggleProfile = viewModel::toggleProfileEnabled,
                            onDeleteProfile = viewModel::requestDeleteProfile,
                        )
                    }

                    composable(Routes.SECURITY) {
                        SecurityScreen(
                            uiState = uiState,
                            onChangePin = viewModel::requestChangePin,
                            onToggleAntiTamper = viewModel::toggleAntiTamper,
                            onRequestVpnPermission = { launchVpnPermissionIfNeeded() },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onOpenSystemVpnSettings = {
                                val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                                context.startActivity(intent)
                            },
                            onToggleDeviceAdmin = {
                                if (uiState.systemReadiness.isDeviceAdminActive) {
                                    viewModel.requestDeactivateDeviceAdmin()
                                } else {
                                    launchDeviceAdminActivation()
                                }
                            },
                            onUninstallApp = viewModel::requestUninstallApp,
                        )
                    }

                    composable(
                        route = Routes.PROFILE_EDITOR,
                        arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
                    ) { backStackEntry ->
                        val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
                        ProfileEditorScreen(
                            profileId = profileId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    if (showPauseSheet) {
        PauseDurationBottomSheet(
            onSelectDuration = { minutes ->
                showPauseSheet = false
                viewModel.requestPauseProtection(minutes)
            },
            onDismiss = { showPauseSheet = false },
        )
    }

    pinModalMode?.let { mode ->
        PinVerificationBottomSheet(
            mode = mode,
            errorMessage = pinErrorMessage,
            onSubmitPin = viewModel::submitPinForModal,
            onDismiss = viewModel::dismissPinModal,
        )
    }
}
