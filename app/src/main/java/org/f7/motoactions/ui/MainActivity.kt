package org.f7.motoactions.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.f7.motoactions.MotoActionsApp
import org.f7.motoactions.service.GestureService
import org.f7.motoactions.util.CalibrationStore
import org.f7.motoactions.util.EnergyMonitor
import org.f7.motoactions.util.EventLog
import org.f7.motoactions.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) maybeStartService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensurePermissions()
        requestBatteryExemption()
        maybeStartService()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    SettingsScreen(
                        prefs = MotoActionsApp.instance.prefs,
                        energy = MotoActionsApp.instance.energy,
                        onToggleService = { enabled ->
                            MotoActionsApp.instance.prefs.serviceEnabled = enabled
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
                            suggestion.chopPeak?.let {
                                MotoActionsApp.instance.prefs.chopPeakThreshold = it
                            }
                            suggestion.twistPeak?.let {
                                MotoActionsApp.instance.prefs.twistPeakThreshold = it
                            }
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
    onToggleService: (Boolean) -> Unit,
    onCapture: (label: String, on: Boolean) -> Unit,
    onApplyCalibration: () -> String,
    onOpenSystemBattery: () -> Unit,
) {
    var service by remember { mutableStateOf(prefs.serviceEnabled) }
    var chop by remember { mutableStateOf(prefs.chopEnabled) }
    var twist by remember { mutableStateOf(prefs.twistEnabled) }
    var temporalTwist by remember { mutableStateOf(prefs.useTemporalTwist) }
    var screenOff by remember { mutableStateOf(prefs.screenOffEnabled) }
    var logs by remember { mutableStateOf(prefs.verboseLogs) }
    var sens by remember { mutableFloatStateOf(prefs.sensitivity) }
    var cooldown by remember { mutableFloatStateOf(prefs.cooldownMs.toFloat()) }
    var capturing by remember { mutableStateOf(false) }
    var captureLabel by remember { mutableStateOf("chop") }
    var calibNote by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(EventLog.snapshot()) }
    var energySnap by remember { mutableStateOf(energy.snapshot()) }

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

    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val dayFmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("F7 Moto Actions", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Software chop-chop + twist for POCO F7 (onyx). Motorola MMI sensors are absent; " +
                "thresholds should be calibrated on-device.",
            style = MaterialTheme.typography.bodyMedium,
        )

        RowToggle("Engine running", service) {
            service = it
            onToggleService(it)
        }
        RowToggle("Chop-Chop → flashlight", chop) {
            chop = it
            prefs.chopEnabled = it
        }
        RowToggle("Twist → camera", twist) {
            twist = it
            prefs.twistEnabled = it
        }
        RowToggle("Temporal Twist detector", temporalTwist) {
            temporalTwist = it
            prefs.useTemporalTwist = it
            // Pref listener in GestureService re-applies thresholds / engine selection.
            GestureService.start(MotoActionsApp.instance)
        }
        Text(
            if (temporalTwist) {
                "Twist engine: temporal sequence (default)"
            } else {
                "Twist engine: legacy magnitude gates"
            },
            style = MaterialTheme.typography.bodySmall,
        )
        RowToggle("Screen-off (IMU + wakelock)", screenOff) {
            screenOff = it
            prefs.screenOffEnabled = it
        }
        RowToggle("Verbose sensor logs", logs) {
            logs = it
            prefs.verboseLogs = it
        }

        Text("Battery / energia", style = MaterialTheme.typography.titleMedium)
        Text(
            "Modo agora: ${energySnap.imuMode} · wakelock=${energySnap.wakeLockHeldNow}\n" +
                "Screen-off IMU: ${formatDuration(energySnap.screenOffImuMs)}\n" +
                "Interactive IMU: ${formatDuration(energySnap.interactiveImuMs)}\n" +
                "WakeLock total: ${formatDuration(energySnap.wakeLockMs)}\n" +
                "Gestos: chop=${energySnap.chopCount} twist=${energySnap.twistCount}\n" +
                "Estimativa timers: ~${"%.1f".format(energySnap.estimatedMahFromTimers)} mAh " +
                "(${EnergyMonitor.EST_MA_SCREEN_OFF_IMU.toInt()} mA off / " +
                "${EnergyMonitor.EST_MA_INTERACTIVE_IMU.toInt()} mA on)\n" +
                "Coulomb Δ: " +
                (energySnap.chargeDeltaUah?.let { "${"%.1f".format(it / 1000.0)} mAh" } ?: "n/d") +
                " · reset ${dayFmt.format(Date(energySnap.resetAtWallMs))}",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { energy.reset() }) { Text("Reset energia") }
            OutlinedButton(onClick = onOpenSystemBattery) { Text("App info / bateria") }
        }

        Text("Sensitivity ×${"%.2f".format(sens)}")
        Slider(
            value = sens,
            onValueChange = {
                sens = it
                prefs.sensitivity = it
            },
            valueRange = 0.5f..2.0f,
        )

        Text("Cooldown ${cooldown.toInt()} ms")
        Slider(
            value = cooldown,
            onValueChange = {
                cooldown = it
                prefs.cooldownMs = it.toLong()
            },
            valueRange = 400f..5000f,
        )

        Text("Calibration", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = captureLabel == "chop",
                onClick = { captureLabel = "chop" },
                label = { Text("chop") },
            )
            FilterChip(
                selected = captureLabel == "twist",
                onClick = { captureLabel = "twist" },
                label = { Text("twist") },
            )
            FilterChip(
                selected = captureLabel == "walk",
                onClick = { captureLabel = "walk" },
                label = { Text("walk") },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    capturing = !capturing
                    onCapture(captureLabel, capturing)
                },
            ) {
                Text(if (capturing) "Stop capture" else "Record samples")
            }
            OutlinedButton(
                onClick = { calibNote = onApplyCalibration() },
            ) {
                Text("Apply stats → thresholds")
            }
        }
        if (calibNote.isNotBlank()) Text(calibNote)

        Text("Event log", style = MaterialTheme.typography.titleMedium)
        entries.asReversed().take(40).forEach { e ->
            Text(
                "${fmt.format(Date(e.ts))}  ${e.message}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RowToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%dh %02dm %02ds".format(h, m, s)
}
