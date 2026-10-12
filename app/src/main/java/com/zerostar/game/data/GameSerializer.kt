package com.zerostar.game.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.zerostar.game.game.*
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

object GameSerializer : Serializer<GameState> {
    override val defaultValue = GameState()
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    fun encode(state: GameState): String = json.encodeToString(state)
    override suspend fun readFrom(input: InputStream): GameState {
        try {
            val text = input.readBytes().decodeToString(throwOnInvalidSequence = true)
            val fields = json.parseToJsonElement(text).jsonObject
            require(fields.keys.containsAll(setOf("schemaVersion", "balanceMilli", "earnedMilli", "upgradeLevel", "collectedCount", "lastSettledEpochMs", "soundEnabled", "reduceMotion", "tutorialSeen")))
            return json.decodeFromString<GameState>(text).also(::validate)
        } catch (error: SerializationException) {
            throw CorruptionException("Invalid game save", error)
        } catch (error: IllegalArgumentException) {
            throw CorruptionException("Unsupported or invalid game save", error)
        } catch (error: java.nio.charset.CharacterCodingException) {
            throw CorruptionException("Invalid save encoding", error)
        }
    }
    private fun validate(state: GameState) {
        require(state.schemaVersion == 1)
        require(state.balanceMilli in 0..MAX_MILLI && state.earnedMilli in 0..MAX_MILLI)
        require(state.balanceMilli <= state.earnedMilli)
        require(state.upgradeLevel in 0..30 && state.collectedCount in 0..6)
        require(state.lastSettledEpochMs >= 0)
        require(state.collectedCount != 6 || stageOf(state.earnedMilli) == 5)
    }
    override suspend fun writeTo(t: GameState, output: OutputStream) {
        validate(t)
        output.write(encode(t).encodeToByteArray())
    }
}
