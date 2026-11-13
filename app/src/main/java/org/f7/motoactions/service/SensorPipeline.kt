package org.f7.motoactions.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import org.f7.motoactions.MotoActionsApp
import org.f7.motoactions.detect.ChopChopDetector
import org.f7.motoactions.detect.TemporalTwistDetector
import org.f7.motoactions.util.CalibrationStore
import org.f7.motoactions.util.CameraLauncher
import org.f7.motoactions.util.EnergyMonitor
import org.f7.motoactions.util.EventLog
import org.f7.motoactions.util.Prefs
import org.f7.motoactions.util.TorchController

/**
 * LSM6DSV accel/gyro on onyx are **non-wakeup**. Screen-off detection therefore keeps
 * the IMU registered and holds a [PowerManager.PARTIAL_WAKE_LOCK] while
 * [Prefs.screenOffEnabled] is on — the motion_detect gate alone cannot deliver chop/twist
 * reliably from deep sleep on this device.
 */
class SensorPipeline(
    private val context: Context,
    private val prefs: Prefs,
    private val torch: TorchController,
    private val camera: CameraLauncher,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val chop = ChopChopDetector()
    private val twist = TemporalTwistDetector()

    private var proxCovered = false
    private var imuArmed = false
    private var screenOn = true
    private var capturing = false
    private var captureLabel = "idle"
    private var registeredHz = -1
    private var lastSensorEventMs = 0L
    private var running = false

    private var lastAx = 0f
    private var lastAy = 0f
    private var lastAz = 0f
    private var lastGx = 0f
    private var lastGy = 0f
    private var lastGz = 0f
    private var lastTemporalEvent = "IDLE"

    private val wakeLock: PowerManager.WakeLock =
        powerManager
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "F7MotoActions:ScreenOffImu")
            .apply { setReferenceCounted(false) }

    private val energy: EnergyMonitor
        get() = MotoActionsApp.instance.energy

    private val heartbeat =
        object : Runnable {
            override fun run() {
                if (!running) return
                watchdogTick()
                handler.postDelayed(this, HEARTBEAT_MS)
            }
        }

    fun start() {
        running = true
        applyThresholds()
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        screenOn = powerManager.isInteractive
        syncListeningMode("start")
        handler.removeCallbacks(heartbeat)
        handler.postDelayed(heartbeat, HEARTBEAT_MS)
        EventLog.add("pipeline start screenOn=$screenOn screenOffGestures=${prefs.screenOffEnabled}")
    }

    fun stop() {
        running = false
        handler.removeCallbacks(heartbeat)
        sensorManager.unregisterListener(this)
        imuArmed = false
        registeredHz = -1
        releaseScreenOffWakeLock()
        energy.onImuMode(EnergyMonitor.ImuMode.OFF)
        chop.reset()
        twist.reset()
        lastTemporalEvent = "IDLE"
    }

    /** Called periodically while the service lives — rebind if IMU went silent. */
    fun watchdogTick() {
        if (!prefs.serviceEnabled || !running) return
        val now = SystemClock.elapsedRealtime()
        screenOn = powerManager.isInteractive
        if (imuArmed && lastSensorEventMs != 0L && now - lastSensorEventMs > SILENT_REBIND_MS) {
            EventLog.add("watchdog: IMU silent ${now - lastSensorEventMs}ms → rebind")
            imuArmed = false
            registeredHz = -1
            syncListeningMode("watchdog-silent")
        } else if (!screenOn && prefs.screenOffEnabled) {
            // Refresh timed wakelock (HyperOS / API 31+ revoke unlimited holds).
            acquireScreenOffWakeLock(forceRefresh = true)
        }
    }

    fun onScreenChanged(on: Boolean) {
        screenOn = on
        if (!prefs.serviceEnabled) return
        syncListeningMode(if (on) "screen on" else "screen off")
    }

    fun applyThresholds() {
        chop.minAxP2p = prefs.effectiveChopAxP2p()
        chop.peakMag = prefs.chopPeakMag
        chop.windowMs = prefs.chopWindowMs
        chop.maxPeakGapMs = prefs.chopMaxGapMs
        chop.minPeakGapMs = 70L
        chop.refractoryAfterGestureMs = prefs.cooldownMs
        twist.refractoryAfterGestureMs = prefs.cooldownMs
        // Sensitivity eases temporal enter/peak gates (higher sens → lower enter).
        val sens = prefs.sensitivity.coerceIn(0.5f, 2.0f)
        twist.rateEnter = 6f / sens
        twist.minPeakRate = 9f / sens
        twist.minHalfIntegral = 0.55f / sens
        twist.accelAbortThreshold = prefs.twistAccelVeto.coerceAtLeast(28f)
        screenOn = powerManager.isInteractive
        syncListeningMode("prefs")
    }

    fun setCapturing(enabled: Boolean, label: String = "idle") {
        capturing = enabled
        captureLabel = label
        if (enabled) ensureImu(forceHz = prefs.samplingHz)
    }

    private fun syncListeningMode(reason: String) {
        val wantScreenOffListen = prefs.screenOffEnabled && prefs.serviceEnabled
        when {
            screenOn || capturing -> {
                releaseScreenOffWakeLock()
                ensureImu(forceHz = prefs.samplingHz)
                energy.onImuMode(EnergyMonitor.ImuMode.INTERACTIVE)
                EventLog.add("listen=$reason interactive hz=${prefs.samplingHz}")
            }
            wantScreenOffListen -> {
                // Non-wakeup LSM6DSV needs an awake process; keep full gesture rate.
                acquireScreenOffWakeLock(forceRefresh = true)
                ensureImu(forceHz = prefs.samplingHz.coerceAtLeast(SCREEN_OFF_MIN_HZ))
                energy.onImuMode(EnergyMonitor.ImuMode.SCREEN_OFF)
                EventLog.add(
                    "listen=$reason screen-off IMU+wakelock @${prefs.samplingHz.coerceAtLeast(SCREEN_OFF_MIN_HZ)}Hz",
                )
            }
            else -> {
                releaseScreenOffWakeLock()
                disarmImu("screen-off gestures disabled")
                energy.onImuMode(EnergyMonitor.ImuMode.OFF)
                EventLog.add("listen=$reason idle (no screen-off)")
            }
        }
    }

    private fun ensureImu(forceHz: Int) {
        if (accel == null) {
            EventLog.add("no accelerometer")
            return
        }
        if (imuArmed && registeredHz == forceHz) return

        if (imuArmed) {
            accel.let { sensorManager.unregisterListener(this, it) }
            gyro?.let { sensorManager.unregisterListener(this, it) }
            imuArmed = false
        }

        val us = (1_000_000 / forceHz).coerceAtLeast(5_000)
        // Screen-off: no batching — delayed deliveries look like "dead" gestures.
        val batchUs = if (!screenOn && prefs.screenOffEnabled) 0 else 80_000
        val okAccel = sensorManager.registerListener(this, accel, us, batchUs)
        val okGyro = gyro?.let { sensorManager.registerListener(this, it, us, batchUs) } ?: true
        imuArmed = okAccel
        registeredHz = if (okAccel) forceHz else -1
        if (!okAccel || !okGyro) {
            EventLog.add("IMU register failed accel=$okAccel gyro=$okGyro")
        } else {
            EventLog.add("IMU armed hz=$forceHz batchUs=$batchUs screenOn=$screenOn")
        }
        // Proximity must stay registered after accel rebind.
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun disarmImu(reason: String) {
        if (!imuArmed) {
            releaseScreenOffWakeLock()
            return
        }
        accel?.let { sensorManager.unregisterListener(this, it) }
        gyro?.let { sensorManager.unregisterListener(this, it) }
        imuArmed = false
        registeredHz = -1
        proximity?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        releaseScreenOffWakeLock()
        if (prefs.verboseLogs) EventLog.add("IMU disarmed: $reason")
    }

    private fun acquireScreenOffWakeLock(forceRefresh: Boolean = false) {
        // Short renewable holds — HyperOS revokes long PARTIAL_WAKE_LOCKs.
        if (!wakeLock.isHeld || forceRefresh) {
            runCatching {
                if (wakeLock.isHeld) wakeLock.release()
            }
            wakeLock.acquire(WAKE_LOCK_MS)
            energy.onWakeLock(true)
            if (prefs.verboseLogs || forceRefresh) {
                EventLog.add("PARTIAL_WAKE_LOCK ${WAKE_LOCK_MS}ms (screen-off)")
            }
        }
    }

    private fun releaseScreenOffWakeLock() {
        if (wakeLock.isHeld) {
            runCatching { wakeLock.release() }
            energy.onWakeLock(false)
            if (prefs.verboseLogs) EventLog.add("PARTIAL_WAKE_LOCK released")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val t = SystemClock.elapsedRealtime()
        lastSensorEventMs = t
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                // Match Lineage ChopForTorch on G42:
                // values[0] < rint(maxRange * 10) / 10
                val max = event.sensor.maximumRange
                val threshold = kotlin.math.round(max * 10.0f) / 10.0f
                val distance = event.values[0]
                proxCovered = distance >= 0f && distance < threshold
            }
            Sensor.TYPE_ACCELEROMETER -> {
                lastAx = event.values[0]
                lastAy = event.values[1]
                lastAz = event.values[2]
                maybeCapture(t)
                if (proxCovered) {
                    if (prefs.verboseLogs) EventLog.add("chop skip: proximity")
                } else if (prefs.chopEnabled) {
                    val chopped = chop.onAccelerometer(lastAx, lastAy, lastAz, t)
                    if (chopped) {
                        onChop(t)
                    } else if (prefs.verboseLogs) {
                        val r = chop.lastRejectReason()
                        if (r.startsWith("ACCEPT") || r.startsWith("ax_p2p") || r.startsWith("no_opposite")) {
                            // throttle: only notable rejects occasionally via reason change
                        }
                    }
                }
                chop.onIdleTick(t)
                twist.onAccelerometerDynamic(chop.lastDynamicMagnitude())
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGx = event.values[0]
                lastGy = event.values[1]
                lastGz = event.values[2]
                maybeCapture(t)
                chop.onGyroscope(lastGx, lastGy, lastGz)
                if (prefs.twistEnabled) {
                    val fired = twist.onGyroscope(lastGx, lastGy, lastGz, t)
                    val ev = twist.lastEventCode()
                    if (ev != lastTemporalEvent) {
                        lastTemporalEvent = ev
                        if (
                            ev.startsWith("ROTATION_") ||
                                ev.startsWith("REVERSAL_") ||
                                ev.startsWith("GESTURE_")
                        ) {
                            EventLog.add("TWIST_SM $ev")
                        }
                    }
                    if (fired) onTwist(t)
                }
            }
        }
    }

    private fun onChop(t: Long) {
        twist.suppressUntil(t)
        energy.onChop()
        EventLog.add("CHOP-CHOP ${chop.lastRejectReason()}")
        runCatching {
            vibrator.vibrate(
                VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
        if (torch.toggle()) {
            EventLog.add("torch → ${torch.torchEnabled}")
        }
    }

    private fun onTwist(t: Long) {
        chop.suppressUntil(t)
        energy.onTwist()
        EventLog.add("TWIST ${twist.lastRejectReason()}")
        camera.launch()
    }

    private fun maybeCapture(t: Long) {
        if (!capturing) return
        CalibrationStore.append(
            context,
            t,
            lastAx,
            lastAy,
            lastAz,
            lastGx,
            lastGy,
            lastGz,
            captureLabel,
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        /** Floor rate while screen off (gesture timing needs ~this). */
        private const val SCREEN_OFF_MIN_HZ = 80
        private const val HEARTBEAT_MS = 8_000L
        private const val SILENT_REBIND_MS = 2_000L
        /** Renew often — long holds get revoked on PixelOS/HyperOS. */
        private const val WAKE_LOCK_MS = 45_000L
    }
}
