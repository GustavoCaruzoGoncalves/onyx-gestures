package org.f7.motoactions.detect

import kotlin.math.sqrt

/**
 * Experimental software chop detector for POCO F7 (LSM6DSV).
 *
 * Grounded in Mot-paired G42 captures + F7 repeats (see docs/CLASSIFIER_DESIGN.md):
 * - |a| alone does **not** separate walk from chop.
 * - Valid portrait chops: **ax-dominant**, high [ax_p2p], ≥2 |a| peaks, short interpeak.
 *
 * Streaming approximation of the offline rule (not Mot SensorHub bit-exact).
 */
class ChopChopDetector(
    /** Minimum ax peak-to-peak in the analysis window (m/s²). */
    var minAxP2p: Float = 70f,
    /** |a| peak gate for counting strikes (m/s²). */
    var peakMag: Float = 20f,
    var windowMs: Long = 800L,
    var minPeakGapMs: Long = 70L,
    /** Upper bound — tighter than offline 160 ms study to reduce CHOP_SLOW FPs. */
    var maxPeakGapMs: Long = 140L,
    var peakRefractoryMs: Long = 80L,
    var refractoryAfterGestureMs: Long = 900L,
) {
    private data class Sample(val t: Long, val ax: Float, val ay: Float, val az: Float, val mag: Float)

    private val buf = ArrayDeque<Sample>(256)
    private var lastGestureMs = 0L
    private var lastDynamic = 0f
    private var lastReject = "idle"
    private var armed = true

    fun reset() {
        buf.clear()
        lastGestureMs = 0L
        lastDynamic = 0f
        lastReject = "reset"
        armed = true
    }

    fun suppressUntil(timestampMs: Long) {
        lastGestureMs = timestampMs
        armed = true
        lastReject = "suppressed"
    }

    /** Optional: unused for veto in v1.1 (kept for pipeline API stability). */
    fun onGyroscope(gx: Float, gy: Float, gz: Float) = Unit

    fun onAccelerometer(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        val mag = sqrt(x * x + y * y + z * z)
        lastDynamic = mag
        buf.addLast(Sample(timestampMs, x, y, z, mag))
        prune(timestampMs)

        if (lastGestureMs != 0L && timestampMs - lastGestureMs < refractoryAfterGestureMs) {
            lastReject = "cooldown"
            return false
        }
        if (buf.size < 8) {
            lastReject = "warmup"
            return false
        }

        val axP2p = p2p { it.ax }
        val ayP2p = p2p { it.ay }
        val azP2p = p2p { it.az }
        if (axP2p < minAxP2p) {
            lastReject = "ax_p2p=$axP2p<$minAxP2p"
            return false
        }
        if (axP2p < ayP2p || axP2p < azP2p) {
            lastReject = "not_ax_dominant ax=$axP2p ay=$ayP2p az=$azP2p"
            return false
        }

        val peaks = findPeaks()
        if (peaks.size < 2) {
            lastReject = "n_peaks=${peaks.size}"
            return false
        }

        // Prefer a consecutive pair with opposite ax sign and Mot-like spacing.
        var accepted = false
        var bestGap = -1L
        for (i in 1 until peaks.size) {
            val gap = peaks[i].t - peaks[i - 1].t
            if (gap < minPeakGapMs || gap > maxPeakGapMs) continue
            val s0 = sign(peaks[i - 1].ax)
            val s1 = sign(peaks[i].ax)
            if (s0 != 0 && s1 != 0 && s0 == s1) continue
            accepted = true
            bestGap = gap
            break
        }
        if (!accepted) {
            lastReject = "no_opposite_ax_pair_in_${minPeakGapMs}-${maxPeakGapMs}ms peaks=${peaks.size}"
            return false
        }

        // One-shot edge: require re-arm after dropping below soft activity.
        if (!armed) {
            lastReject = "not_rearmed"
            return false
        }

        armed = false
        lastGestureMs = timestampMs
        lastReject = "ACCEPT gap=${bestGap}ms ax_p2p=$axP2p n=${peaks.size}"
        return true
    }

    fun onIdleTick(timestampMs: Long) {
        // Re-arm when window is quiet (between gestures).
        if (!armed && buf.isNotEmpty()) {
            val recent = buf.last()
            if (recent.mag < peakMag * 0.55f) armed = true
        }
        prune(timestampMs)
    }

    fun lastDynamicMagnitude(): Float = lastDynamic

    fun lastRejectReason(): String = lastReject

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
            lo = minOf(lo, v)
            hi = maxOf(hi, v)
        }
        return if (lo.isFinite()) hi - lo else 0f
    }

    private fun findPeaks(): List<Sample> {
        val out = ArrayList<Sample>(8)
        var lastPeakT = Long.MIN_VALUE / 4
        var i = 1
        val list = buf.toList()
        while (i < list.size - 1) {
            val s = list[i]
            if (s.mag >= peakMag &&
                s.mag >= list[i - 1].mag &&
                s.mag >= list[i + 1].mag &&
                s.t - lastPeakT >= peakRefractoryMs
            ) {
                // Local refine ±40ms
                var best = s
                var j = i
                while (j < list.size && list[j].t - s.t <= 40) {
                    if (list[j].mag > best.mag) best = list[j]
                    j++
                }
                if (out.isEmpty() || best.t - out.last().t >= peakRefractoryMs) {
                    out.add(best)
                    lastPeakT = best.t
                } else if (best.mag > out.last().mag) {
                    out[out.lastIndex] = best
                    lastPeakT = best.t
                }
                i = j
                continue
            }
            i++
        }
        return out
    }

    private fun sign(v: Float): Int =
        when {
            v > 2f -> 1
            v < -2f -> -1
            else -> 0
        }
}
