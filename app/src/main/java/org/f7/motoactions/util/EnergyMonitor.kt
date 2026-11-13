package org.f7.motoactions.util

import android.content.Context
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.SystemClock
import androidx.core.content.edit
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

/**
 * Tracks wake-lock / IMU exposure and optional coulomb-counter deltas.
 * Power estimates are **order-of-magnitude** only — use [scripts/battery_report.ps1]
 * + `dumpsys batterystats` for OS-attributed numbers.
 */
class EnergyMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val sp: SharedPreferences =
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val battery = appContext.getSystemService(BatteryManager::class.java)

    private val listeners = CopyOnWriteArrayList<(Snapshot) -> Unit>()

    @Volatile private var wakeLockSinceElapsed: Long = 0L
    @Volatile private var imuMode: ImuMode = ImuMode.OFF
    @Volatile private var imuModeSinceElapsed: Long = SystemClock.elapsedRealtime()

    enum class ImuMode { OFF, INTERACTIVE, SCREEN_OFF }

    data class Snapshot(
        val wakeLockMs: Long,
        val screenOffImuMs: Long,
        val interactiveImuMs: Long,
        val chopCount: Long,
        val twistCount: Long,
        val resetAtWallMs: Long,
        val chargeCounterUah: Long?,
        val chargeDeltaUah: Long?,
        /** Rough mAh from timed model (not coulomb). */
        val estimatedMahFromTimers: Double,
        val wakeLockHeldNow: Boolean,
        val imuMode: ImuMode,
    )

    fun start() {
        // no-op; pipeline drives updates
        notifyListeners()
    }

    fun onWakeLock(held: Boolean) {
        val now = SystemClock.elapsedRealtime()
        synchronized(this) {
            if (held) {
                if (wakeLockSinceElapsed == 0L) wakeLockSinceElapsed = now
            } else if (wakeLockSinceElapsed != 0L) {
                val heldMs = now - wakeLockSinceElapsed
                sp.edit { putLong(KEY_WL, sp.getLong(KEY_WL, 0L) + heldMs) }
                wakeLockSinceElapsed = 0L
            }
        }
        notifyListeners()
    }

    fun onImuMode(mode: ImuMode) {
        val now = SystemClock.elapsedRealtime()
        synchronized(this) {
            flushImuLocked(now)
            imuMode = mode
            imuModeSinceElapsed = now
        }
        notifyListeners()
    }

    fun onChop() {
        sp.edit { putLong(KEY_CHOP, sp.getLong(KEY_CHOP, 0L) + 1L) }
        notifyListeners()
    }

    fun onTwist() {
        sp.edit { putLong(KEY_TWIST, sp.getLong(KEY_TWIST, 0L) + 1L) }
        notifyListeners()
    }

    fun reset() {
        synchronized(this) {
            val now = SystemClock.elapsedRealtime()
            flushImuLocked(now)
            if (wakeLockSinceElapsed != 0L) {
                val heldMs = now - wakeLockSinceElapsed
                // discard into reset baseline instead of keeping
                wakeLockSinceElapsed = now
            }
            val uah = readChargeCounterUah()
            sp.edit {
                putLong(KEY_WL, 0L)
                putLong(KEY_SO, 0L)
                putLong(KEY_IX, 0L)
                putLong(KEY_CHOP, 0L)
                putLong(KEY_TWIST, 0L)
                putLong(KEY_RESET_WALL, System.currentTimeMillis())
                if (uah != null) putLong(KEY_CHARGE_BASE, uah) else remove(KEY_CHARGE_BASE)
            }
            imuModeSinceElapsed = now
        }
        EventLog.add("energy stats reset")
        notifyListeners()
    }

    fun snapshot(): Snapshot {
        val now = SystemClock.elapsedRealtime()
        var wl = sp.getLong(KEY_WL, 0L)
        var so = sp.getLong(KEY_SO, 0L)
        var ix = sp.getLong(KEY_IX, 0L)
        synchronized(this) {
            if (wakeLockSinceElapsed != 0L) wl += now - wakeLockSinceElapsed
            when (imuMode) {
                ImuMode.SCREEN_OFF -> so += now - imuModeSinceElapsed
                ImuMode.INTERACTIVE -> ix += now - imuModeSinceElapsed
                ImuMode.OFF -> Unit
            }
        }
        val uah = readChargeCounterUah()
        val base = if (sp.contains(KEY_CHARGE_BASE)) sp.getLong(KEY_CHARGE_BASE, 0L) else null
        val delta =
            if (uah != null && base != null) {
                // CHARGE_COUNTER decreases as battery drains (µAh remaining).
                max(0L, base - uah)
            } else {
                null
            }
        val est =
            (so / 3_600_000.0) * EST_MA_SCREEN_OFF_IMU +
                (ix / 3_600_000.0) * EST_MA_INTERACTIVE_IMU
        return Snapshot(
            wakeLockMs = wl,
            screenOffImuMs = so,
            interactiveImuMs = ix,
            chopCount = sp.getLong(KEY_CHOP, 0L),
            twistCount = sp.getLong(KEY_TWIST, 0L),
            resetAtWallMs = sp.getLong(KEY_RESET_WALL, System.currentTimeMillis()),
            chargeCounterUah = uah,
            chargeDeltaUah = delta,
            estimatedMahFromTimers = est,
            wakeLockHeldNow = wakeLockSinceElapsed != 0L,
            imuMode = imuMode,
        )
    }

    fun subscribe(listener: (Snapshot) -> Unit) {
        listeners.add(listener)
        listener(snapshot())
    }

    fun unsubscribe(listener: (Snapshot) -> Unit) {
        listeners.remove(listener)
    }

    private fun flushImuLocked(now: Long) {
        val elapsed = now - imuModeSinceElapsed
        when (imuMode) {
            ImuMode.SCREEN_OFF -> sp.edit { putLong(KEY_SO, sp.getLong(KEY_SO, 0L) + elapsed) }
            ImuMode.INTERACTIVE -> sp.edit { putLong(KEY_IX, sp.getLong(KEY_IX, 0L) + elapsed) }
            ImuMode.OFF -> Unit
        }
    }

    private fun readChargeCounterUah(): Long? =
        runCatching {
            val v = battery.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (v == Long.MIN_VALUE || v <= 0L) null else v
        }.getOrNull()

    private fun notifyListeners() {
        val snap = snapshot()
        listeners.forEach { it(snap) }
    }

    companion object {
        private const val PREFS = "f7_energy"
        private const val KEY_WL = "wl_ms"
        private const val KEY_SO = "so_ms"
        private const val KEY_IX = "ix_ms"
        private const val KEY_CHOP = "chop_n"
        private const val KEY_TWIST = "twist_n"
        private const val KEY_RESET_WALL = "reset_wall"
        private const val KEY_CHARGE_BASE = "charge_base_uah"

        /** Rough extras vs idle — LSM6DSV + batching + FGS, screen off with wakelock. */
        const val EST_MA_SCREEN_OFF_IMU = 12.0
        /** Interactive listening without dedicated screen-off wakelock. */
        const val EST_MA_INTERACTIVE_IMU = 5.0
    }
}
