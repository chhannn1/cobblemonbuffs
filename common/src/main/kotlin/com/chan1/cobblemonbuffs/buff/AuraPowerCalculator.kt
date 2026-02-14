package com.chan1.cobblemonbuffs.buff

import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfig
import com.chan1.cobblemonbuffs.config.CobblemonBuffsConfigData
import com.cobblemon.mod.common.pokemon.Pokemon

object AuraPowerCalculator {

    data class TypeCandidate(
        val typeName: String,
        val bestLevel: Int,
        val bestFriendship: Int,
        val bestPokemonName: String,
        val partyOrder: Int
    )

    data class ComputeResult(
        val scores: Map<String, AuraPowerScore>,
        val candidates: Map<String, TypeCandidate>
    )

    fun computeScores(
        slottedPokemon: List<Pair<Int, Pokemon>>,
        config: CobblemonBuffsConfigData
    ): ComputeResult {
        val candidates = collectBestPerType(slottedPokemon)
        val scores = mutableMapOf<String, AuraPowerScore>()

        for ((typeName, candidate) in candidates) {
            val tierOverride = CobblemonBuffsConfig.getTierOverride(typeName)
            if (tierOverride != null) {
                val tier = when (tierOverride) {
                    3 -> AuraTier.T3
                    2 -> AuraTier.T2
                    else -> AuraTier.T1
                }
                scores[typeName] = AuraPowerScore(
                    typeName = typeName,
                    score = when (tier) {
                        AuraTier.T3 -> config.levelMaxPoints + config.friendshipMaxPoints
                        AuraTier.T2 -> config.t2Threshold
                        AuraTier.T1 -> config.t1Threshold
                    },
                    tier = tier,
                    levelPoints = 0,
                    friendshipPoints = 0
                )
                continue
            }

            val levelPoints = (candidate.bestLevel / 100.0 * config.levelMaxPoints).toInt()
            val friendshipPoints = (candidate.bestFriendship / 255.0 * config.friendshipMaxPoints).toInt()
            val totalScore = (levelPoints + friendshipPoints).coerceAtLeast(0)

            var tier = AuraTier.fromScore(totalScore, config.t1Threshold, config.t2Threshold, config.t3Threshold)

            // enable flags
            if (tier == AuraTier.T3 && !config.enableTier3) tier = AuraTier.T2
            if (tier == AuraTier.T2 && !config.enableTier2) tier = AuraTier.T1

            scores[typeName] = AuraPowerScore(
                typeName = typeName,
                score = totalScore,
                tier = tier,
                levelPoints = levelPoints,
                friendshipPoints = friendshipPoints
            )
        }

        return ComputeResult(scores, candidates)
    }

    fun collectBestPerType(slottedPokemon: List<Pair<Int, Pokemon>>): Map<String, TypeCandidate> {
        val candidates = mutableMapOf<String, TypeCandidate>()

        for ((slotIndex, pokemon) in slottedPokemon) {
            val types = mutableListOf(pokemon.primaryType.name.lowercase())
            pokemon.secondaryType?.let { types.add(it.name.lowercase()) }

            for (typeName in types) {
                if (!CobblemonBuffsConfig.isTypeEnabled(typeName)) continue
                if (BuffRegistry.getForType(typeName) == null) continue

                val existing = candidates[typeName]
                if (existing == null || pokemon.level > existing.bestLevel ||
                    (pokemon.level == existing.bestLevel && pokemon.friendship > existing.bestFriendship)) {
                    candidates[typeName] = TypeCandidate(
                        typeName = typeName,
                        bestLevel = pokemon.level,
                        bestFriendship = pokemon.friendship,
                        bestPokemonName = pokemon.species.name,
                        partyOrder = slotIndex
                    )
                }
            }
        }

        return candidates
    }
}
