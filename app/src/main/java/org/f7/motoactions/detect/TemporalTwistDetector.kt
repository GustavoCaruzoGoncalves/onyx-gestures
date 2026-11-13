package org.f7.motoactions.detect

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Temporal Twist / Quick Capture detector (production default).
 *
 * State machine: IDLE → ROTATION_1 → REVERSAL → ROTATION_2 → CONFIRM
 *
 * Recognizes a bidirectional wrist rotation using angular integral + direction
 * change, rather than a single gy_p2p gate. Axis is chosen at ROT1 onset
 * (dominant |ω| component), not hard-coded to Y.
 */
class TemporalTwistDetector(
    var rateEnter: Float = 6f,
    var rateExit: Float = 2.5f,
    var minHalfIntegral: Float = 0.55f,
    var minHalfMs: Long = 40L,
    var maxHalfMs: Long = 550L,
    var maxGapBetweenHalvesMs: Long = 280L,
    var maxTotalMs: Long = 900L,
    var minSecondIntegralRatio: Float = 0.35f,
    var refractoryAfterGestureMs: Long = 900L,
    var accelAbortThreshold: Float = 28f,
    var minPeakRate: Float = 9f,
) {
    private enum class State { IDLE, ROT1, REVERSAL, ROT2 }

    private var state = State.IDLE
    private var axis = 1
    private var sign1 = 0
    private var int1 = 0f
    private var int2 = 0f
    private var peak1 = 0f
    private var peak2 = 0f
    private var tRot1 = 0L
    private var tRev = 0L
    private var tRot2 = 0L
    private var lastT = 0L
    private var lastGestureMs = 0L
    private var latestAccelDyn = 0f
    private var lastReject = "IDLE"
    private var lastEvent = "IDLE"

    fun reset() {
        state = State.IDLE
        axis = 1
        sign1 = 0
        int1 = 0f
        int2 = 0f
        peak1 = 0f
        peak2 = 0f
        tRot1 = 0L
        tRev = 0L
        tRot2 = 0L
        lastT = 0L
        lastGestureMs = 0L
        latestAccelDyn = 0f
        lastReject = "reset"
        lastEvent = "IDLE"
    }

    fun onAccelerometerDynamic(dynamicMs2: Float) {
        latestAccelDyn = dynamicMs2
    }

    fun suppressUntil(timestampMs: Long) {
        lastGestureMs = timestampMs
        toIdle("suppressed")
    }

    fun lastRejectReason(): String = lastReject

    fun lastEventCode(): String = lastEvent

    fun onGyroscope(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        if (lastGestureMs != 0L && timestampMs - lastGestureMs < refractoryAfterGestureMs) {
            lastReject = "GESTURE_REJECTED cooldown"
            lastT = timestampMs
            return false
        }

        val v = floatArrayOf(x, y, z)
        val mag = sqrt(x * x + y * y + z * z)
        val dt =
            if (lastT == 0L) {
                0f
            } else {
                ((timestampMs - lastT).coerceAtLeast(0L) / 1000f).coerceAtMost(0.05f)
            }
        lastT = timestampMs

        if (latestAccelDyn >= accelAbortThreshold) {
            if (state != State.IDLE) reject("GESTURE_REJECTED accel_abort")
            return false
        }

        when (state) {
            State.IDLE -> {
                val ax = dominantAxis(v)
                val r = v[ax]
                if (abs(r) >= rateEnter && mag >= rateEnter) {
                    state = State.ROT1
                    axis = ax
                    sign1 = if (r >= 0f) 1 else -1
                    int1 = 0f
                    peak1 = abs(r)
                    tRot1 = timestampMs
                    event("ROTATION_1_DETECTED", "axis=$ax sign=$sign1 r=${"%.2f".format(r)}")
                }
                return false
            }

            State.ROT1 -> {
                val r = v[axis]
                if (dt > 0f) {
                    int1 += r * dt
                    peak1 = max(peak1, abs(r))
                }
                if (timestampMs - tRot1 > maxTotalMs) {
                    reject("GESTURE_TIMEOUT total")
                    return false
                }
                val halfMs = timestampMs - tRot1
                val sameDir = r * sign1 > 0f
                if (halfMs >= minHalfMs && abs(int1) >= minHalfIntegral * 0.5f) {
                    if (r * sign1 <= rateExit || !sameDir) {
                        if (peak1 >= minPeakRate && abs(int1) >= minHalfIntegral * 0.45f) {
                            state = State.REVERSAL
                            tRev = timestampMs
                            event("REVERSAL_DETECTED", "int1=${"%.3f".format(int1)} peak1=${"%.2f".format(peak1)}")
                        }
                    }
                }
                if (halfMs > maxHalfMs && state == State.ROT1) {
                    reject("GESTURE_TIMEOUT half1")
                }
                return false
            }

            State.REVERSAL -> {
                val r = v[axis]
                if (timestampMs - tRev > maxGapBetweenHalvesMs) {
                    reject("GESTURE_TIMEOUT gap")
                    return false
                }
                if (r * sign1 <= -rateEnter * 0.85f) {
                    state = State.ROT2
                    tRot2 = timestampMs
                    int2 = 0f
                    peak2 = abs(r)
                    event("ROTATION_2_DETECTED", "r=${"%.2f".format(r)}")
                }
                return false
            }

            State.ROT2 -> {
                val r = v[axis]
                if (dt > 0f) {
                    int2 += r * dt
                    peak2 = max(peak2, abs(r))
                }
                val halfMs = timestampMs - tRot2
                var oppOk = abs(int2) >= minHalfIntegral * minSecondIntegralRatio
                if (int1 * int2 > 0f) oppOk = false
                val peakOk = peak2 >= minPeakRate * 0.75f
                val slowing = abs(r) < rateEnter || r * sign1 >= -rateExit

                if (halfMs >= minHalfMs && oppOk && peakOk && (slowing || halfMs > 120L)) {
                    if (peak2 < peak1 * 0.25f && abs(int2) < abs(int1) * 0.25f) {
                        reject("GESTURE_REJECTED weak_second")
                        return false
                    }
                    return confirm(timestampMs)
                }
                if (halfMs > maxHalfMs) {
                    if (oppOk && peakOk && abs(int2) >= minHalfIntegral * 0.5f) {
                        return confirm(timestampMs)
                    }
                    reject("GESTURE_TIMEOUT half2")
                }
                if (timestampMs - tRot1 > maxTotalMs) {
                    reject("GESTURE_TIMEOUT total")
                }
                return false
            }
        }
    }

    private fun confirm(timestampMs: Long): Boolean {
        val msg =
            "GESTURE_CONFIRMED int1=${"%.3f".format(int1)} int2=${"%.3f".format(int2)} " +
                "peak1=${"%.2f".format(peak1)} peak2=${"%.2f".format(peak2)} axis=$axis"
        event("GESTURE_CONFIRMED", msg)
        lastReject = msg
        lastGestureMs = timestampMs
        toIdle(null)
        return true
    }

    private fun reject(why: String) {
        event("GESTURE_REJECTED", why)
        lastReject = why
        toIdle(null)
    }

    private fun toIdle(detail: String?) {
        state = State.IDLE
        int1 = 0f
        int2 = 0f
        peak1 = 0f
        peak2 = 0f
        sign1 = 0
        if (detail != null) lastEvent = detail
    }

    private fun event(code: String, detail: String) {
        lastEvent = "$code $detail"
        lastReject = lastEvent
    }

    private fun dominantAxis(v: FloatArray): Int {
        var best = 0
        var bestAbs = abs(v[0])
        for (i in 1..2) {
            val a = abs(v[i])
            if (a > bestAbs) {
                bestAbs = a
                best = i
            }
        }
        return best
    }
}
