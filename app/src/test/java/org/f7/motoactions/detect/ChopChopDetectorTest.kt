package org.f7.motoactions.detect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChopChopDetectorTest {
    private fun rest(d: ChopChopDetector, t0: Long, n: Int = 40): Long {
        var t = t0
        repeat(n) {
            d.onAccelerometer(0.2f, 0.1f, 9.8f, t)
            t += 5
        }
        return t
    }

    @Test
    fun oppositeAxDoubleChopFires() {
        val d =
            ChopChopDetector(
                minAxP2p = 70f,
                peakMag = 20f,
                windowMs = 800,
                maxPeakGapMs = 140,
                refractoryAfterGestureMs = 1000,
            )
        var t = rest(d, 0L)
        var fired = false
        fun step(ax: Float, ay: Float, az: Float, dt: Long = 5) {
            if (d.onAccelerometer(ax, ay, az, t)) fired = true
            t += dt
        }
        // First strike +ax
        step(20f, 0f, 9f)
        step(40f, 0f, 8f)
        step(55f, 1f, 7f)
        step(30f, 0f, 9f)
        step(0.2f, 0f, 9.8f)
        // valley ~90ms
        repeat(18) { step(0.2f, 0f, 9.8f) }
        // Second strike -ax
        step(-20f, 0f, 9f)
        step(-40f, 0f, 8f)
        step(-55f, -1f, 7f)
        step(-30f, 0f, 9f)
        step(0.2f, 0f, 9.8f)
        assertTrue("reject=${d.lastRejectReason()}", fired)
    }

    @Test
    fun walkingLowAxP2pDoesNotFire() {
        val d = ChopChopDetector(minAxP2p = 70f, peakMag = 20f, refractoryAfterGestureMs = 1000)
        var t = rest(d, 0L)
        repeat(50) { i ->
            val az = 9.8f + if (i % 12 < 3) 12f else 0f
            assertFalse(d.onAccelerometer(0.8f, 0.3f, az, t))
            t += 5
        }
    }
}
