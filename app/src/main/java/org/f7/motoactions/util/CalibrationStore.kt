package org.f7.motoactions.util

import android.content.Context
import java.io.File
import kotlin.math.max

/**
 * CSV capture + simple peak statistics used to refine detector thresholds.
 * Format: ts_ms,ax,ay,az,gx,gy,gz,label
 */
object CalibrationStore {
    fun captureFile(context: Context): File =
        File(context.getExternalFilesDir("calibration"), "samples.csv")

    fun ensureHeader(file: File) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeText("ts_ms,ax,ay,az,gx,gy,gz,label\n")
        }
    }

    fun append(
        context: Context,
        ts: Long,
        ax: Float,
        ay: Float,
        az: Float,
        gx: Float,
        gy: Float,
        gz: Float,
        label: String,
    ) {
        val f = captureFile(context)
        ensureHeader(f)
        f.appendText("$ts,$ax,$ay,$az,$gx,$gy,$gz,$label\n")
    }

    data class Suggestion(val chopPeak: Float?, val twistPeak: Float?, val notes: String)

    fun suggestFromCsv(file: File): Suggestion {
        if (!file.exists()) return Suggestion(null, null, "no samples")
        val chopPeaks = mutableListOf<Float>()
        val twistPeaks = mutableListOf<Float>()
        var gX = 0f
        var gY = 0f
        var gZ = 9.8f
        var init = false
        file.useLines { lines ->
            lines.drop(1).forEach { line ->
                val p = line.split(",")
                if (p.size < 8) return@forEach
                val ax = p[1].toFloatOrNull() ?: return@forEach
                val ay = p[2].toFloatOrNull() ?: return@forEach
                val az = p[3].toFloatOrNull() ?: return@forEach
                val gx = p[4].toFloatOrNull() ?: return@forEach
                val gy = p[5].toFloatOrNull() ?: return@forEach
                val gz = p[6].toFloatOrNull() ?: return@forEach
                val label = p[7]
                if (!init) {
                    gX = ax; gY = ay; gZ = az; init = true
                }
                val a = 0.92f
                gX = a * gX + (1 - a) * ax
                gY = a * gY + (1 - a) * ay
                gZ = a * gZ + (1 - a) * az
                val dx = ax - gX
                val dy = ay - gY
                val dz = az - gZ
                val dyn = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                val gyro = max(kotlin.math.abs(gy), kotlin.math.abs(gz))
                when (label) {
                    "chop" -> chopPeaks += dyn
                    "twist" -> twistPeaks += gyro
                }
            }
        }
        val chop =
            chopPeaks.filter { it > 5f }.sorted().let { list ->
                if (list.isEmpty()) null else list[list.size * 70 / 100] * 0.85f
            }
        val twist =
            twistPeaks.filter { it > 2f }.sorted().let { list ->
                if (list.isEmpty()) null else list[list.size * 70 / 100] * 0.85f
            }
        return Suggestion(
            chopPeak = chop,
            twistPeak = twist,
            notes = "chopPeaks=${chopPeaks.size} twistPeaks=${twistPeaks.size}",
        )
    }
}
