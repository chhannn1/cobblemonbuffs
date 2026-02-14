package com.chan1.cobblemonbuffs.utils.render

object AnimationUtil {
    fun easeOutCubic(t: Float): Float {
        val t1 = 1f - t
        return 1f - t1 * t1 * t1
    }

    fun easeInCubic(t: Float): Float {
        return t * t * t
    }

    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    fun scalePulse(t: Float, peak: Float = 1.4f): Float {
        return if (t < 0.3f) {
            val p = t / 0.3f
            1f + (peak - 1f) * easeOutCubic(p)
        } else {
            val p = (t - 0.3f) / 0.7f
            peak + (1f - peak) * easeOutCubic(p)
        }
    }
}
