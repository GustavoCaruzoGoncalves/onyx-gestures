package org.f7.motoactions.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.f7.motoactions.BuildConfig
import org.f7.motoactions.MotoActionsApp
import org.f7.motoactions.R
import org.f7.motoactions.service.GestureService
import org.f7.motoactions.ui.theme.F7MotoActionsTheme
import org.f7.motoactions.ui.theme.ThemeMode
import org.f7.motoactions.util.CalibrationStore
import org.f7.motoactions.util.EnergyMonitor
import org.f7.motoactions.util.EventLog
import org.f7.motoactions.util.LocaleHelper
import org.f7.motoactions.util.ModuleUpdater
import org.f7.motoactions.util.Prefs
import org.f7.motoactions.util.UpdateCheckResult
import org.f7.motoactions.util.UpdateInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) maybeStartService()
        }

    override fun attachBaseContext(newBase: Context) {
        val lang = Prefs(newBase).appLanguage
        super.attachBaseContext(LocaleHelper.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ensurePermissions()
        requestBatteryExemption()
        maybeStartService()
        setContent {
            val prefs = MotoActionsApp.instance.prefs
            var themeMode by remember { mutableIntStateOf(prefs.themeMode) }
            F7MotoActionsTheme(mode = ThemeMode.fromPref(themeMode)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsScreen(
                        prefs = prefs,
                        energy = MotoActionsApp.instance.energy,
                        themeMode = themeMode,
                        onThemeMode = {
                            themeMode = it
                            prefs.themeMode = it
                        },
                        onLanguage = { tag ->
                            prefs.appLanguage = tag
                            recreate()
                        },
                        onToggleService = { enabled ->
                            prefs.serviceEnabled = enabled
                            if (enabled) GestureService.start(this) else GestureService.stop(this)
                        },
                        onCapture = { label, on ->
                            val action =
                                if (on) GestureService.ACTION_START_CAPTURE
                                else GestureService.ACTION_STOP_CAPTURE
                            startService(
                                Intent(this, GestureService::class.java)
                                    .setAction(action)
                                    .putExtra(GestureService.EXTRA_LABEL, label),
                            )
                        },
                        onApplyCalibration = {
                            val suggestion =
                                CalibrationStore.suggestFromCsv(CalibrationStore.captureFile(this))
                            suggestion.chopPeak?.let { prefs.chopPeakThreshold = it }
                            suggestion.twistPeak?.let { prefs.twistPeakThreshold = it }
                            EventLog.add("calibration applied: ${suggestion.notes}")
                            suggestion.notes
                        },
                        onOpenSystemBattery = {
                            runCatching {
                                startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:$packageName")
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun ensurePermissions() {
        val needed = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS
        val missing =
            needed.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private fun requestBatteryExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName"),
                    ),
                )
            }
        }
    }

    private fun maybeStartService() {
        if (MotoActionsApp.instance.prefs.serviceEnabled) {
            GestureService.start(this)
        }
    }
}

@Composable
private fun SettingsScreen(
    prefs: Prefs,
    energy: EnergyMonitor,
    themeMode: Int,
    onThemeMode: (Int) -> Unit,
    onLanguage: (String) -> Unit,
    onToggleService: (Boolean) -> Unit,
    onCapture: (label: String, on: Boolean) -> Unit,
    onApplyCalibration: () -> String,
    onOpenSystemBattery: () -> Unit,
) {
    var service by remember { mutableStateOf(prefs.serviceEnabled) }
    var chop by remember { mutableStateOf(prefs.chopEnabled) }
    var twist by remember { mutableStateOf(prefs.twistEnabled) }
    var screenOff by remember { mutableStateOf(prefs.screenOffEnabled) }
    var logs by remember { mutableStateOf(prefs.verboseLogs) }
    var sens by remember { mutableFloatStateOf(prefs.sensitivity) }
    var cooldown by remember { mutableFloatStateOf(prefs.cooldownMs.toFloat()) }
    var capturing by remember { mutableStateOf(false) }
    var captureLabel by remember { mutableStateOf("chop") }
    var calibNote by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(EventLog.snapshot()) }
    var energySnap by remember { mutableStateOf(energy.snapshot()) }
    var language by remember { mutableStateOf(prefs.appLanguage) }
    var updateStatus by remember { mutableStateOf("") }
    var updateBusy by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var installedVersionLabel by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val logListener: (List<EventLog.Entry>) -> Unit = { entries = it }
        val energyListener: (EnergyMonitor.Snapshot) -> Unit = { energySnap = it }
        EventLog.subscribe(logListener)
        energy.subscribe(energyListener)
        onDispose {
            EventLog.unsubscribe(logListener)
            energy.unsubscribe(energyListener)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            energySnap = energy.snapshot()
        }
    }

    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(R.string.author_by),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.author_credit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.version_line, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        SectionCard(title = stringResource(R.string.section_gestures)) {
            RowToggle(stringResource(R.string.toggle_engine), service) {
                service = it
                onToggleService(it)
            }
            RowToggle(stringResource(R.string.toggle_chop), chop) {
                chop = it
                prefs.chopEnabled = it
            }
            RowToggle(stringResource(R.string.toggle_twist), twist) {
                twist = it
                prefs.twistEnabled = it
            }
            RowToggle(stringResource(R.string.toggle_screen_off), screenOff) {
                screenOff = it
                prefs.screenOffEnabled = it
            }
            RowToggle(stringResource(R.string.toggle_verbose), logs) {
                logs = it
                prefs.verboseLogs = it
            }
        }

        SectionCard(title = stringResource(R.string.section_updates)) {
            Text(
                stringResource(R.string.update_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (updateStatus.isNotBlank()) {
                Text(updateStatus, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !updateBusy,
                    onClick = {
                        scope.launch {
                            updateBusy = true
                            updateStatus = context.getString(R.string.update_checking)
                            val result =
                                withContext(Dispatchers.IO) { ModuleUpdater.checkForUpdate() }
                            updateStatus =
                                when (result) {
                                    is UpdateCheckResult.Available -> {
                                        pendingUpdate = result.info
                                        context.getString(
                                            R.string.update_available,
                                            result.info.version,
                                            result.info.versionCode,
                                        )
                                    }
                                    is UpdateCheckResult.UpToDate -> {
                                        pendingUpdate = null
                                        context.getString(
                                            R.string.update_up_to_date,
                                            result.currentCode,
                                        )
                                    }
                                    is UpdateCheckResult.Error -> {
                                        pendingUpdate = null
                                        context.getString(R.string.update_failed, result.message)
                                    }
                                }
                            updateBusy = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.btn_check_updates))
                }
                OutlinedButton(
                    enabled = !updateBusy,
                    onClick = { ModuleUpdater.openKernelSu(context) },
                ) {
                    Text(stringResource(R.string.btn_open_kernelsu))
                }
            }
            Button(
                enabled = !updateBusy,
                onClick = {
                    scope.launch {
                        updateBusy = true
                        runCatching {
                            if (!withContext(Dispatchers.IO) { ModuleUpdater.hasRoot() }) {
                                updateStatus = context.getString(R.string.update_no_root)
                                return@runCatching
                            }
                            updateStatus = context.getString(R.string.update_checking)
                            val info =
                                pendingUpdate
                                    ?: withContext(Dispatchers.IO) { ModuleUpdater.fetchUpdateInfo() }
                            updateStatus =
                                context.getString(R.string.update_downloading, info.version)
                            val zip =
                                withContext(Dispatchers.IO) {
                                    ModuleUpdater.downloadZip(context, info.zipUrl)
                                }
                            updateStatus = context.getString(R.string.update_installing)
                            val log =
                                withContext(Dispatchers.IO) { ModuleUpdater.installWithRoot(zip) }
                            EventLog.add("update installed ${info.version}")
                            EventLog.add(log.lineSequence().firstOrNull() ?: "ksud ok")
                            installedVersionLabel = info.version
                            updateStatus =
                                context.getString(R.string.update_installed, info.version)
                            showRebootDialog = true
                        }.onFailure {
                            updateStatus =
                                context.getString(
                                    R.string.update_failed,
                                    it.message ?: it.toString(),
                                )
                            EventLog.add("update failed: ${it.message}")
                        }
                        updateBusy = false
                    }
                },
            ) {
                Text(stringResource(R.string.btn_install_update))
            }
        }

        if (showRebootDialog) {
            AlertDialog(
                onDismissRequest = { showRebootDialog = false },
                title = { Text(stringResource(R.string.update_reboot_title)) },
                text = {
                    Text(
                        stringResource(R.string.update_reboot_message) +
                            if (installedVersionLabel.isNotBlank()) {
                                "\n\n$installedVersionLabel"
                            } else {
                                ""
                            },
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRebootDialog = false
                            scope.launch(Dispatchers.IO) {
                                runCatching { ModuleUpdater.rebootDevice() }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.update_reboot_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRebootDialog = false }) {
                        Text(stringResource(R.string.update_reboot_later))
                    }
                },
            )
        }

        SectionCard(title = stringResource(R.string.section_appearance)) {
            Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = themeMode == 0,
                    onClick = { onThemeMode(0) },
                    label = { Text(stringResource(R.string.theme_system)) },
                )
                FilterChip(
                    selected = themeMode == 1,
                    onClick = { onThemeMode(1) },
                    label = { Text(stringResource(R.string.theme_light)) },
                )
                FilterChip(
                    selected = themeMode == 2,
                    onClick = { onThemeMode(2) },
                    label = { Text(stringResource(R.string.theme_dark)) },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.language_label), style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LocaleHelper.OPTIONS.forEach { opt ->
                    val selected =
                        if (opt.tag == "system") {
                            language == "system" || language.isBlank()
                        } else {
                            language.equals(opt.tag, ignoreCase = true)
                        }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            language = opt.tag
                            onLanguage(opt.tag)
                        },
                        label = { Text(opt.label) },
                    )
                }
            }
        }

        SectionCard(title = stringResource(R.string.section_power)) {
            val coulomb =
                energySnap.chargeDeltaUah?.let { "%.1f mAh".format(it / 1000.0) }
                    ?: stringResource(R.string.energy_na)
            Text(
                stringResource(
                    R.string.energy_mode,
                    energySnap.imuMode.name,
                    energySnap.wakeLockHeldNow.toString(),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.energy_screen_off, formatDuration(energySnap.screenOffImuMs)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.energy_interactive, formatDuration(energySnap.interactiveImuMs)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.energy_wakelock, formatDuration(energySnap.wakeLockMs)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.energy_gestures, energySnap.chopCount, energySnap.twistCount),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(
                    R.string.energy_estimate,
                    energySnap.estimatedMahFromTimers,
                    EnergyMonitor.EST_MA_SCREEN_OFF_IMU.toInt(),
                    EnergyMonitor.EST_MA_INTERACTIVE_IMU.toInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(
                    R.string.energy_coulomb,
                    coulomb,
                    dayFmt.format(Date(energySnap.resetAtWallMs)),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { energy.reset() }) {
                    Text(stringResource(R.string.btn_reset_energy))
                }
                OutlinedButton(onClick = onOpenSystemBattery) {
                    Text(stringResource(R.string.btn_app_battery))
                }
            }
        }

        SectionCard(title = null) {
            Text(stringResource(R.string.sensitivity, sens))
            Slider(
                value = sens,
                onValueChange = {
                    sens = it
                    prefs.sensitivity = it
                },
                valueRange = 0.5f..2.0f,
            )
            Text(stringResource(R.string.cooldown, cooldown.toInt()))
            Slider(
                value = cooldown,
                onValueChange = {
                    cooldown = it
                    prefs.cooldownMs = it.toLong()
                },
                valueRange = 400f..5000f,
            )
        }

        SectionCard(title = stringResource(R.string.section_calibration)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = captureLabel == "chop",
                    onClick = { captureLabel = "chop" },
                    label = { Text(stringResource(R.string.chip_chop)) },
                )
                FilterChip(
                    selected = captureLabel == "twist",
                    onClick = { captureLabel = "twist" },
                    label = { Text(stringResource(R.string.chip_twist)) },
                )
                FilterChip(
                    selected = captureLabel == "walk",
                    onClick = { captureLabel = "walk" },
                    label = { Text(stringResource(R.string.chip_walk)) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        capturing = !capturing
                        onCapture(captureLabel, capturing)
                    },
                ) {
                    Text(
                        if (capturing) {
                            stringResource(R.string.btn_stop_capture)
                        } else {
                            stringResource(R.string.btn_record)
                        },
                    )
                }
                OutlinedButton(onClick = { calibNote = onApplyCalibration() }) {
                    Text(stringResource(R.string.btn_apply_calibration))
                }
            }
            if (calibNote.isNotBlank()) Text(calibNote, style = MaterialTheme.typography.bodySmall)
        }

        SectionCard(title = stringResource(R.string.section_log)) {
            entries.asReversed().take(40).forEach { e ->
                Text(
                    "${fmt.format(Date(e.ts))}  ${e.message}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.author_credit),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun SectionCard(
    title: String?,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}

@Composable
private fun RowToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%dh %02dm %02ds".format(h, m, s)
}
