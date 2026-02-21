package com.chan1.cobblemonbuffs.client

import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfig

object ClientConfigState {
    var t1Threshold: Int = CobblemonBuffsConfig.data.t1Threshold
        private set
    var t2Threshold: Int = CobblemonBuffsConfig.data.t2Threshold
        private set
    var t3Threshold: Int = CobblemonBuffsConfig.data.t3Threshold
        private set
    var levelMaxPoints: Int = CobblemonBuffsConfig.data.levelMaxPoints
        private set
    var friendshipMaxPoints: Int = CobblemonBuffsConfig.data.friendshipMaxPoints
        private set

    fun update(t1: Int, t2: Int, t3: Int, levelMax: Int, friendshipMax: Int) {
        t1Threshold = t1
        t2Threshold = t2
        t3Threshold = t3
        levelMaxPoints = levelMax
        friendshipMaxPoints = friendshipMax
    }
}
