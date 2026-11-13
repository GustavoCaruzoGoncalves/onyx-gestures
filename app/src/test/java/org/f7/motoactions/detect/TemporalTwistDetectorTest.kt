package org.f7.motoactions.detect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporalTwistDetectorTest {
    @Test
    fun doubleOppositeRotationConfirms() {
        val d = TemporalTwistDetector(refractoryAfterGestureMs = 1000)
        var t = 0L
        var fired = false
        fun step(gx: Float, gy: Float, gz: Float, dt: Long = 8) {
            if (d.onGyroscope(gx, gy, gz, t)) fired = true
            t += dt
        }
        repeat(10) { step(0f, 0.2f, 0f) }
        // ROT1 positive gy
        for (gy in listOf(8f, 12f, 14f, 10f, 6f, 3f, 1f, 0f)) step(1f, gy, 0.2f)
        // reverse into negative
        for (gy in listOf(-2f, -8f, -14f, -12f, -6f, -2f, 0f)) step(1f, gy, 0.2f)
        assertTrue("reject=${d.lastRejectReason()}", fired)
        assertTrue(d.lastRejectReason().contains("GESTURE_CONFIRMED"))
    }

    @Test
    fun singleDirectionDoesNotConfirm() {
        val d = TemporalTwistDetector()
        var t = 0L
        var fired = false
        fun step(gy: Float, dt: Long = 8) {
            if (d.onGyroscope(0.5f, gy, 0.2f, t)) fired = true
            t += dt
        }
        repeat(8) { step(0.1f) }
        for (gy in listOf(8f, 14f, 12f, 8f, 4f, 1f, 0f, 0f, 0f)) step(gy)
        // linger without opposite half
        repeat(80) { step(0.2f) }
        assertFalse(d.lastRejectReason(), fired)
    }

    @Test
    fun hardAccelAborts() {
        val d = TemporalTwistDetector(accelAbortThreshold = 22f)
        d.onAccelerometerDynamic(35f)
        var t = 0L
        var fired = false
        fun step(gy: Float, dt: Long = 8) {
            if (d.onGyroscope(0.5f, gy, 0.2f, t)) fired = true
            t += dt
        }
        repeat(8) { step(0.1f) }
        for (gy in listOf(10f, 14f, 0f, -10f, -14f, 0f)) step(gy)
        assertFalse(fired)
    }
}
