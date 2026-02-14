package com.chan1.cobblemonbuffs.buff

enum class AuraTier(val level: Int) {
    T1(1),
    T2(2),
    T3(3);

    companion object {
        fun fromScore(score: Int, t1Threshold: Int = 10, t2Threshold: Int = 35, t3Threshold: Int = 75): AuraTier {
            return when {
                score >= t3Threshold -> T3
                score >= t2Threshold -> T2
                else -> T1
            }
        }
    }
}

data class AuraPowerScore(
    val typeName: String,
    val score: Int,
    val tier: AuraTier,
    val levelPoints: Int,
    val friendshipPoints: Int
)
