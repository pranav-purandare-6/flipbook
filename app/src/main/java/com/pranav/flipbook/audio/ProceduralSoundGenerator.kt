package com.pranav.flipbook.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates lightweight offline WAV assets at runtime and caches them on disk.
 */
object ProceduralSoundGenerator {

    private const val SAMPLE_RATE = 22050

    fun ensurePageTurnSound(cacheDir: File): File {
        val dir = File(cacheDir, "sounds").apply { mkdirs() }
        val file = File(dir, "page_turn.wav")
        if (!file.exists() || file.length() < 100) {
            writeWav(file, generatePageTurnSamples())
        }
        return file
    }

    fun ensureAmbientLoop(cacheDir: File, soundName: String): File {
        val dir = File(cacheDir, "sounds").apply { mkdirs() }
        val file = File(dir, "ambient_$soundName.wav")
        if (!file.exists() || file.length() < 100) {
            val samples = when (soundName) {
                "rain" -> generateRainLoop()
                "fireplace" -> generateFireplaceLoop()
                "cafe" -> generateCafeLoop()
                "forest" -> generateForestLoop()
                "whitenoise" -> generateWhiteNoiseLoop()
                else -> generateWhiteNoiseLoop()
            }
            writeWav(file, samples)
        }
        return file
    }

    private fun generatePageTurnSamples(): ShortArray {
        val durationMs = 180
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val random = Random(42)
        for (i in samples.indices) {
            val t = i.toFloat() / count
            val envelope = sin(t * PI.toFloat()) * (1f - t * 0.3f)
            val noise = (random.nextFloat() * 2f - 1f) * 0.35f
            val low = sin(i * 0.08f) * 0.08f
            val sample = ((noise + low) * envelope * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateRainLoop(): ShortArray {
        val durationMs = 3000
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val random = Random(7)
        var filter = 0f
        for (i in samples.indices) {
            val raw = random.nextFloat() * 2f - 1f
            filter = filter * 0.92f + raw * 0.08f
            val drip = if (random.nextFloat() < 0.002f) random.nextFloat() * 0.4f else 0f
            val sample = ((filter * 0.25f + drip) * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateFireplaceLoop(): ShortArray {
        val durationMs = 3000
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val random = Random(13)
        var rumble = 0f
        for (i in samples.indices) {
            rumble = rumble * 0.995f + (random.nextFloat() * 2f - 1f) * 0.02f
            val crackle = if (random.nextFloat() < 0.004f) random.nextFloat() * 0.5f else 0f
            val sample = ((rumble * 0.35f + crackle) * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateCafeLoop(): ShortArray {
        val durationMs = 3000
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val random = Random(21)
        var pink = 0f
        for (i in samples.indices) {
            val white = random.nextFloat() * 2f - 1f
            pink = pink * 0.97f + white * 0.03f
            val murmur = sin(i * 0.003f) * 0.04f + sin(i * 0.007f) * 0.03f
            val sample = ((pink * 0.18f + murmur) * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateForestLoop(): ShortArray {
        val durationMs = 3000
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val random = Random(31)
        var wind = 0f
        for (i in samples.indices) {
            wind = wind * 0.98f + (random.nextFloat() * 2f - 1f) * 0.015f
            val bird = if (random.nextFloat() < 0.0015f) {
                sin(i * 0.05f) * 0.15f * (1f - (i % 800) / 800f)
            } else 0f
            val sample = ((wind * 0.2f + bird) * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateWhiteNoiseLoop(): ShortArray {
        val durationMs = 2000
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val random = Random(99)
        for (i in samples.indices) {
            val sample = (random.nextFloat() * 2f - 1f) * 0.12f * Short.MAX_VALUE
            samples[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        val headerSize = 44
        val totalSize = headerSize + dataSize

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalSize - 8)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16)
            header.putShort(1) // PCM
            header.putShort(1) // mono
            header.putInt(SAMPLE_RATE)
            header.putInt(SAMPLE_RATE * 2)
            header.putShort(2)
            header.putShort(16)
            header.put("data".toByteArray())
            header.putInt(dataSize)
            out.write(header.array())

            val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { buffer.putShort(it) }
            out.write(buffer.array())
        }
    }
}
