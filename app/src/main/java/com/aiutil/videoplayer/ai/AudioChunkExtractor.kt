package com.aiutil.videoplayer.ai

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * 자막 없는 영상의 현재 구간 오디오 트랙을 16kHz 모노 PCM WAV 바이트로 추출한다.
 * 추출된 WAV는 AI 음성인식/번역(Gemini 등)에 전달된다.
 */
object AudioChunkExtractor {

    private const val TARGET_SAMPLE_RATE = 16_000
    private const val MAX_DECODE_WALL_MS = 4_500L

    suspend fun extractWavChunk(
        context: Context,
        uriString: String,
        filePath: String,
        startMs: Long,
        durationMs: Long = 10_000L,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            if (filePath.isNotBlank() && java.io.File(filePath).canRead()) {
                extractor.setDataSource(filePath)
            } else {
                extractor.setDataSource(context, Uri.parse(uriString), null)
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return@withContext null

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            extractor.selectTrack(audioTrackIndex)
            val startUs = startMs.coerceAtLeast(0L) * 1_000L
            val endUs = startUs + durationMs.coerceIn(2_000L, 15_000L) * 1_000L
            // 구간 시작점 대사가 잘리지 않도록 직전 동기 프레임으로 이동
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var inputSampleRate = if (audioFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                44_100
            }
            var channelCount = if (audioFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else {
                2
            }
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            runCatching {
                audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            }

            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(audioFormat, null, null, 0)
                start()
            }

            val pcmMono16k = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val wallStart = System.currentTimeMillis()

            while (!outputDone && (System.currentTimeMillis() - wallStart) < MAX_DECODE_WALL_MS) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            val sampleTimeUs = extractor.sampleTime
                            if (sampleSize < 0 || sampleTimeUs > endUs) {
                                codec.queueInputBuffer(
                                    inIdx,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(
                                    inIdx,
                                    0,
                                    sampleSize,
                                    sampleTimeUs,
                                    0
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFmt = codec.outputFormat
                        if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            inputSampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (outFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            pcmEncoding = outFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }

                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0 && bufferInfo.presentationTimeUs >= startUs) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            downsampleToMono16k(
                                pcmBuffer = outBuf,
                                byteCount = bufferInfo.size,
                                sampleRate = inputSampleRate,
                                channels = channelCount.coerceAtLeast(1),
                                pcmEncoding = pcmEncoding,
                                out = pcmMono16k,
                            )
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            val pcmData = pcmMono16k.toByteArray()
            if (pcmData.size < 1600) return@withContext null
            wrapAsWav(pcmData, TARGET_SAMPLE_RATE, 1, 16)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun downsampleToMono16k(
        pcmBuffer: ByteBuffer,
        byteCount: Int,
        sampleRate: Int,
        channels: Int,
        pcmEncoding: Int,
        out: ByteArrayOutputStream,
    ) {
        val ordered = pcmBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val monoSamples: ShortArray = if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
            val floatBuf = ordered.asFloatBuffer()
            val sampleCountTotal = minOf(byteCount / (4 * channels), floatBuf.remaining() / channels)
            if (sampleCountTotal <= 0) return
            ShortArray(sampleCountTotal) { i ->
                var sum = 0f
                for (ch in 0 until channels) {
                    sum += floatBuf.get(i * channels + ch)
                }
                val normalized = (sum / channels).coerceIn(-1f, 1f)
                (normalized * 32767f).roundToInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        } else {
            val shortBuf = ordered.asShortBuffer()
            val sampleCountTotal = minOf(byteCount / (2 * channels), shortBuf.remaining() / channels)
            if (sampleCountTotal <= 0) return
            ShortArray(sampleCountTotal) { i ->
                var sum = 0
                for (ch in 0 until channels) {
                    sum += shortBuf.get(i * channels + ch).toInt()
                }
                (sum / channels).toShort()
            }
        }
        val sampleCountTotal = monoSamples.size

        if (sampleRate == TARGET_SAMPLE_RATE) {
            writeShortsLittleEndian(monoSamples, sampleCountTotal, out)
            return
        }

        val ratio = sampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
        val outLen = (sampleCountTotal / ratio).roundToInt().coerceAtLeast(1)
        val resampled = ShortArray(outLen)
        for (i in 0 until outLen) {
            val srcIdx = (i * ratio).toInt().coerceIn(0, sampleCountTotal - 1)
            resampled[i] = monoSamples[srcIdx]
        }
        writeShortsLittleEndian(resampled, outLen, out)
    }

    private fun writeShortsLittleEndian(samples: ShortArray, count: Int, out: ByteArrayOutputStream) {
        val bytes = ByteArray(count * 2)
        for (i in 0 until count) {
            val v = samples[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        out.write(bytes)
    }

    private fun wrapAsWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM fmt chunk size
            putShort(1) // PCM format
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * bitsPerSample / 8).toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmData.size)
        }.array()
        return header + pcmData
    }
}
