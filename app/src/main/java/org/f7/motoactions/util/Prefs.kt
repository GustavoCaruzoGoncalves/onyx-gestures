package org.f7.motoactions.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Runtime knobs for experimental Mot-inspired detectors on POCO F7 LSM6DSV.
 * Schema 12 resets defaults to feature-based chop/twist (docs/CLASSIFIER_DESIGN.md).
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    init {
        migrateIfNeeded()
    }

    var serviceEnabled: Boolean
        get() = sp.getBoolean(KEY_SERVICE, true)
        set(v) = sp.edit { putBoolean(KEY_SERVICE, v) }

    var chopEnabled: Boolean
        get() = sp.getBoolean(KEY_CHOP, true)
        set(v) = sp.edit { putBoolean(KEY_CHOP, v) }

    var twistEnabled: Boolean
        get() = sp.getBoolean(KEY_TWIST, true)
        set(v) = sp.edit { putBoolean(KEY_TWIST, v) }

    var screenOffEnabled: Boolean
        get() = sp.getBoolean(KEY_SCREEN_OFF, true)
        set(v) = sp.edit { putBoolean(KEY_SCREEN_OFF, v) }

    var verboseLogs: Boolean
        get() = sp.getBoolean(KEY_LOGS, false)
        set(v) = sp.edit { putBoolean(KEY_LOGS, v) }

    /** 0.5 … 2.0 — higher = easier to trigger (divides gates). */
    var sensitivity: Float
        get() = sp.getFloat(KEY_SENS, DEFAULT_SENSITIVITY)
        set(v) = sp.edit { putFloat(KEY_SENS, v.coerceIn(0.5f, 2.0f)) }

    var cooldownMs: Long
        get() = sp.getLong(KEY_COOLDOWN, DEFAULT_COOLDOWN_MS)
        set(v) = sp.edit { putLong(KEY_COOLDOWN, v.coerceIn(400L, 5_000L)) }

    var chopAxP2p: Float
        get() = sp.getFloat(KEY_CHOP_AX_P2P, DEFAULT_CHOP_AX_P2P)
        set(v) = sp.edit { putFloat(KEY_CHOP_AX_P2P, v) }

    var chopPeakMag: Float
        get() = sp.getFloat(KEY_CHOP_PEAK_MAG, DEFAULT_CHOP_PEAK_MAG)
        set(v) = sp.edit { putFloat(KEY_CHOP_PEAK_MAG, v) }

    var chopWindowMs: Long
        get() = sp.getLong(KEY_CHOP_WINDOW, DEFAULT_CHOP_WINDOW_MS)
        set(v) = sp.edit { putLong(KEY_CHOP_WINDOW, v) }

    var chopMaxGapMs: Long
        get() = sp.getLong(KEY_CHOP_MAX_GAP, DEFAULT_CHOP_MAX_GAP_MS)
        set(v) = sp.edit { putLong(KEY_CHOP_MAX_GAP, v) }

    var twistEdgePeak: Float
        get() = sp.getFloat(KEY_TWIST_EDGE, DEFAULT_TWIST_EDGE)
        set(v) = sp.edit { putFloat(KEY_TWIST_EDGE, v) }

    var twistGyP2p: Float
        get() = sp.getFloat(KEY_TWIST_GY_P2P, DEFAULT_TWIST_GY_P2P)
        set(v) = sp.edit { putFloat(KEY_TWIST_GY_P2P, v) }

    var twistPeakW: Float
        get() = sp.getFloat(KEY_TWIST_PEAK_W, DEFAULT_TWIST_PEAK_W)
        set(v) = sp.edit { putFloat(KEY_TWIST_PEAK_W, v) }

    var twistWindowMs: Long
        get() = sp.getLong(KEY_TWIST_WINDOW, DEFAULT_TWIST_WINDOW_MS)
        set(v) = sp.edit { putLong(KEY_TWIST_WINDOW, v) }

    var twistMaxGapMs: Long
        get() = sp.getLong(KEY_TWIST_MAX_GAP, DEFAULT_TWIST_MAX_GAP_MS)
        set(v) = sp.edit { putLong(KEY_TWIST_MAX_GAP, v) }

    var samplingHz: Int
        get() = sp.getInt(KEY_HZ, DEFAULT_HZ)
        set(v) = sp.edit { putInt(KEY_HZ, v.coerceIn(25, 200)) }

    var twistAccelVeto: Float
        get() = sp.getFloat(KEY_TWIST_ACCEL_VETO, DEFAULT_TWIST_ACCEL_VETO)
        set(v) = sp.edit { putFloat(KEY_TWIST_ACCEL_VETO, v) }

    // Legacy keys kept for UI / migration compatibility
    var chopPeakThreshold: Float
        get() = chopAxP2p
        set(v) {
            chopAxP2p = v
        }

    var twistPeakThreshold: Float
        get() = twistEdgePeak
        set(v) {
            twistEdgePeak = v
        }

    var chopGyroVeto: Float
        get() = sp.getFloat(KEY_CHOP_GYRO_VETO, 99f)
        set(v) = sp.edit { putFloat(KEY_CHOP_GYRO_VETO, v) }

    fun register(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun effectiveChopAxP2p(): Float = chopAxP2p / sensitivity

    fun effectiveTwistGyP2p(): Float = twistGyP2p / sensitivity

    fun effectiveTwistPeakW(): Float = twistPeakW / sensitivity

    fun effectiveTwistEdge(): Float = twistEdgePeak / sensitivity

    /** @deprecated use effectiveChopAxP2p */
    fun effectiveChopPeak(): Float = effectiveChopAxP2p()

    /** @deprecated use effectiveTwistEdge */
    fun effectiveTwistPeak(): Float = effectiveTwistEdge()

    private fun migrateIfNeeded() {
        val ver = sp.getInt(KEY_SCHEMA, 0)
        if (ver >= SCHEMA) return
        sp.edit {
            putFloat(KEY_CHOP_AX_P2P, DEFAULT_CHOP_AX_P2P)
            putFloat(KEY_CHOP_PEAK_MAG, DEFAULT_CHOP_PEAK_MAG)
            putLong(KEY_CHOP_WINDOW, DEFAULT_CHOP_WINDOW_MS)
            putLong(KEY_CHOP_MAX_GAP, DEFAULT_CHOP_MAX_GAP_MS)
            putFloat(KEY_TWIST_EDGE, DEFAULT_TWIST_EDGE)
            putFloat(KEY_TWIST_GY_P2P, DEFAULT_TWIST_GY_P2P)
            putFloat(KEY_TWIST_PEAK_W, DEFAULT_TWIST_PEAK_W)
            putLong(KEY_TWIST_WINDOW, DEFAULT_TWIST_WINDOW_MS)
            putLong(KEY_TWIST_MAX_GAP, DEFAULT_TWIST_MAX_GAP_MS)
            putFloat(KEY_TWIST_ACCEL_VETO, DEFAULT_TWIST_ACCEL_VETO)
            putFloat(KEY_SENS, DEFAULT_SENSITIVITY)
            putLong(KEY_COOLDOWN, DEFAULT_COOLDOWN_MS)
            putInt(KEY_HZ, DEFAULT_HZ)
            putInt(KEY_SCHEMA, SCHEMA)
        }
    }

    companion object {
        const val NAME = "f7_moto_actions"
        const val SCHEMA = 12
        const val KEY_SCHEMA = "prefs_schema"

        const val KEY_SERVICE = "service_enabled"
        const val KEY_CHOP = "chop_enabled"
        const val KEY_TWIST = "twist_enabled"
        const val KEY_SCREEN_OFF = "screen_off_enabled"
        const val KEY_LOGS = "verbose_logs"
        const val KEY_SENS = "sensitivity"
        const val KEY_COOLDOWN = "cooldown_ms"
        const val KEY_CHOP_AX_P2P = "chop_ax_p2p"
        const val KEY_CHOP_PEAK_MAG = "chop_peak_mag"
        const val KEY_CHOP_WINDOW = "chop_window_ms"
        const val KEY_CHOP_MAX_GAP = "chop_max_gap_ms"
        const val KEY_TWIST_EDGE = "twist_edge"
        const val KEY_TWIST_GY_P2P = "twist_gy_p2p"
        const val KEY_TWIST_PEAK_W = "twist_peak_w"
        const val KEY_TWIST_WINDOW = "twist_window_ms"
        const val KEY_TWIST_MAX_GAP = "twist_max_gap_ms"
        const val KEY_HZ = "sampling_hz"
        const val KEY_CHOP_GYRO_VETO = "chop_gyro_veto"
        const val KEY_TWIST_ACCEL_VETO = "twist_accel_veto"

        // Legacy prefs keys (ignored after schema 12 migration writes new keys)
        const val KEY_CHOP_PEAK = "chop_peak"
        const val KEY_TWIST_PEAK = "twist_peak"

        const val DEFAULT_CHOP_AX_P2P = 70f
        const val DEFAULT_CHOP_PEAK_MAG = 20f
        const val DEFAULT_CHOP_WINDOW_MS = 800L
        const val DEFAULT_CHOP_MAX_GAP_MS = 140L
        const val DEFAULT_TWIST_EDGE = 8f
        const val DEFAULT_TWIST_GY_P2P = 25f
        const val DEFAULT_TWIST_PEAK_W = 15f
        const val DEFAULT_TWIST_WINDOW_MS = 900L
        const val DEFAULT_TWIST_MAX_GAP_MS = 350L
        const val DEFAULT_SENSITIVITY = 1.0f
        const val DEFAULT_COOLDOWN_MS = 1000L
        const val DEFAULT_HZ = 100
        const val DEFAULT_TWIST_ACCEL_VETO = 22f
    }
}
