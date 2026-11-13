package org.f7.motoactions.detect

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Experimental twist / Quick Capture detector for POCO F7.
 *
 * Mot-paired evidence: **gy-dominant** bidirectional wiggle with
 * gy_p2p ≳ 25 rad/s and |ω| peak ≳ 15 rad/s (docs/CLASSIFIER_DESIGN.md).
 * Opposite-sign gy peaks required; hard linear chop aborts.
 */
class TwistDetector(
    /** Minimum |gy| for a half-wiggle rising edge (rad/s). */
    var peakThreshold: Float = 8f,
    /** Minimum gy peak-to-peak in window (rad/s). */
    var minGyP2p: Float = 25f,
    /** Minimum |ω| peak in window (rad/s). */
    var minPeakW: Float = 15f,
    var windowMs: Long = 900L,
    var minPeakGapMs: Long = 70L,
    /** Reject stretched "slow wiggle" Mot-rejects when possible. */
    var maxPeakGapMs: Long = 350L,
    var refractoryAfterGestureMs: Long = 900L,
    var accelAbortThreshold: Float = 22f,
) {
    private data class Sample(val t: Long, val gx: Float, val gy: Float, val gz: Float, val mag: Float)

    private val buf = ArrayDeque<Sample>(256)
    private var lastGestureMs = 0L
    private var lastAxis = 0f
    private var latestAccelDyn = 0f
    private var lastReject = "idle"
    private var firstPeakT = 0L
    private var firstPeakSign = 0
    private var sawValley = true
    private var halfCount = 0
    private var wasAbove = false
    private var armed = true

    fun reset() {
        buf.clear()
        lastGestureMs = 0L
        lastAxis = 0f
        latestAccelDyn = 0f
        lastReject = "reset"
        clearHalf()
        armed = true
    }

    fun onAccelerometerDynamic(dynamicMs2: Float) {
        latestAccelDyn = dynamicMs2
    }

    fun suppressUntil(timestampMs: Long) {
        lastGestureMs = timestampMs
        clearHalf()
        armed = true
    }

    fun onGyroscope(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        val mag = sqrt(x * x + y * y + z * z)
        lastAxis = y
        buf.addLast(Sample(timestampMs, x, y, z, mag))
        prune(timestampMs)

        if (lastGestureMs != 0L && timestampMs - lastGestureMs < refractoryAfterGestureMs) {
            lastReject = "cooldown"
            updateHysteresis(abs(y))
            return false
        }
        if (buf.size < 8) {
            lastReject = "warmup"
            updateHysteresis(abs(y))
            return false
        }

        val gyP2p = p2p { it.gy }
        val gxP2p = p2p { it.gx }
        val gzP2p = p2p { it.gz }
        val peakW = buf.maxOf { it.mag }

        // Gate on window features (Mot evidence).
        if (gyP2p < minGyP2p || peakW < minPeakW) {
            lastReject = "gy_p2p=$gyP2p peak_w=$peakW"
            // Still track half-gestures below for state, but don't accept.
            trackHalf(y, timestampMs)
            return false
        }
        if (gyP2p < gxP2p || gyP2p < gzP2p) {
            lastReject = "not_gy_dominant"
            trackHalf(y, timestampMs)
            return false
        }
        if (latestAccelDyn >= accelAbortThreshold) {
            lastReject = "accel_abort dyn=$latestAccelDyn"
            clearHalf()
            updateHysteresis(abs(y))
            return false
        }

        val fired = trackHalf(y, timestampMs)
        if (!fired) return false
        if (!armed) {
            lastReject = "not_rearmed"
            return false
        }

        armed = false
        lastGestureMs = timestampMs
        lastReject = "ACCEPT gy_p2p=$gyP2p peak_w=$peakW"
        clearHalf()
        return true
    }

    fun lastAxisRate(): Float = lastAxis

    fun lastRejectReason(): String = lastReject

    private fun trackHalf(y: Float, timestampMs: Long): Boolean {
        val level = abs(y)
        if (halfCount == 1 && level < peakThreshold * 0.4f) {
            sawValley = true
        }
        val above = level >= peakThreshold
        val rising = above && !wasAbove
        updateHysteresis(level)
        if (!rising) return false

        val sign = if (y >= 0f) 1 else -1
        if (halfCount == 0) {
            halfCount = 1
            firstPeakT = timestampMs
            firstPeakSign = sign
            sawValley = false
            return false
        }
        if (!sawValley) return false
        val gap = timestampMs - firstPeakT
        if (gap < minPeakGapMs) return false
        if (gap > maxPeakGapMs) {
            halfCount = 1
            firstPeakT = timestampMs
            firstPeakSign = sign
            sawValley = false
            return false
        }
        if (sign == firstPeakSign) {
            firstPeakT = timestampMs
            firstPeakSign = sign
            sawValley = false
            return false
        }
        return true
    }

    private fun updateHysteresis(level: Float) {
        if (level < peakThreshold * 0.45f) {
            wasAbove = false
            if (!armed && level < peakThreshold * 0.35f) armed = true
        } else if (level >= peakThreshold) {
            wasAbove = true
        }
    }

    private fun clearHalf() {
        halfCount = 0
        firstPeakT = 0L
        firstPeakSign = 0
        sawValley = true
        wasAbove = false
    }

    private fun prune(now: Long) {
        while (buf.isNotEmpty() && now - buf.first().t > windowMs) {
            buf.removeFirst()
        }
    }

    private fun p2p(sel: (Sample) -> Float): Float {
        var lo = Float.POSITIVE_INFINITY
        var hi = Float.NEGATIVE_INFINITY
        for (s in buf) {
            val v = sel(s)
            if (v < lo) lo = v
            if (v > hi) hi = v
        }
        return if (lo.isFinite()) hi - lo else 0f
    }
}
