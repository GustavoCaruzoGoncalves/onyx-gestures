package org.f7.motoactions.detect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwistDetectorTest {
    @Test
    fun oppositeGyWiggleFires() {
        val d =
            TwistDetector(
                peakThreshold = 8f,
                minGyP2p = 25f,
                minPeakW = 15f,
                maxPeakGapMs = 350,
                refractoryAfterGestureMs = 1000,
            )
        var t = 0L
        var fired = false
        fun g(gy: Float, dt: Long = 15) {
            if (d.onGyroscope(1f, gy, 0.2f, t)) fired = true
            t += dt
        }
        repeat(8) { g(0.1f) }
        g(10f); g(16f); g(18f); g(8f); g(0f); g(0f)
        g(-8f); g(-16f); g(-18f); g(-10f); g(0f)
        assertTrue("reject=${d.lastRejectReason()}", fired)
    }

    @Test
    fun hardChopAccelAbortsTwist() {
        val d =
            TwistDetector(
                peakThreshold = 8f,
                minGyP2p = 25f,
                minPeakW = 15f,
                accelAbortThreshold = 22f,
            )
        d.onAccelerometerDynamic(30f)
        var t = 0L
        var fired = false
        fun g(gy: Float, dt: Long = 15) {
            if (d.onGyroscope(1f, gy, 0.2f, t)) fired = true
            t += dt
        }
        repeat(8) { g(0.1f) }
        g(10f); g(18f); g(0f); g(0f)
        g(-10f); g(-18f); g(0f)
        assertFalse(fired)
    }
}
