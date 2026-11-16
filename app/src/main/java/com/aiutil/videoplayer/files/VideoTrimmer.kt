package com.aiutil.videoplayer.files

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.aiutil.videoplayer.model.SubtitleCue
import com.aiutil.videoplayer.model.VideoItem
import com.aiutil.videoplayer.util.SubtitleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * MediaExtractor + MediaMuxer 기반 무손실 고속 동영상 구간 잘라내기 엔진.
 * 재인코딩 없이 원본 화질 그대로 지정 구간([startMs, endMs])을 새 MP4 파일로 저장한다.
 */
object VideoTrimmer {

    private const val MIN_BUFFER_BYTES = 2 shl 20

    suspend fun trimVideo(
        context: Context,
        video: VideoItem,
        startMs: Long,
        endMs: Long,
        desiredFileName: String,
        cues: List<SubtitleCue> = emptyList(),
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(endMs > startMs + 300L) { "잘라내기 구간은 최소 0.3초 이상이어야 합니다." }

            val outDir = resolveOutputDirectory(context, video.path)
            if (!outDir.exists()) outDir.mkdirs()

            val sanitized = desiredFileName.trim()
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .ifBlank { "${File(video.title).nameWithoutExtension}_cut" }
            val finalName = if (sanitized.lowercase().endsWith(".mp4")) sanitized else "$sanitized.mp4"
            val targetFile = FileExplorerManager.uniqueDestination(outDir, finalName)
            val tempFile = File(outDir, ".${targetFile.name}.tmp")

            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            var muxerStarted = false

            try {
                if (video.path.isNotBlank() && File(video.path).canRead()) {
                    extractor.setDataSource(video.path)
                } else {
                    extractor.setDataSource(context, Uri.parse(video.uri), null)
                }

                var videoInputTrack = -1
                var audioInputTrack = -1
                var bufferSize = MIN_BUFFER_BYTES
                val trackMap = HashMap<Int, Int>()

                muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                // 원본 영상 회전 방향 메타데이터 유지
                val rotation = readRotationDegrees(context, video)
                if (rotation in setOf(90, 180, 270)) {
                    runCatching { muxer.setOrientationHint(rotation) }
                }

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("video/") && videoInputTrack < 0) {
                        videoInputTrack = i
                        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                            bufferSize = maxOf(bufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                        }
                        extractor.selectTrack(i)
                        trackMap[i] = muxer.addTrack(format)
                    } else if (mime.startsWith("audio/") && audioInputTrack < 0) {
                        audioInputTrack = i
                        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                            bufferSize = maxOf(bufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                        }
                        extractor.selectTrack(i)
                        trackMap[i] = muxer.addTrack(format)
                    }
                }

                check(trackMap.isNotEmpty()) { "추출할 비디오 또는 오디오 트랙을 찾을 수 없습니다." }

                muxer.start()
                muxerStarted = true

                val startUs = startMs.coerceAtLeast(0L) * 1_000L
                val endUs = endMs * 1_000L
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                val buffer = ByteBuffer.allocate(bufferSize)
                val info = MediaCodec.BufferInfo()
                val lastPtsByTrack = HashMap<Int, Long>()
                val endedTracks = HashSet<Int>()

                var anchorPtsUs = -1L
                var seenVideoKeyFrame = (videoInputTrack < 0)
                var writtenSamples = 0

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val inputTrack = extractor.sampleTrackIndex
                    val outputTrack = trackMap[inputTrack]
                    if (outputTrack == null) {
                        if (!extractor.advance()) break
                        continue
                    }

                    val sampleTimeUs = extractor.sampleTime
                    val isSync = (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0

                    if (inputTrack == videoInputTrack) {
                        if (!seenVideoKeyFrame) {
                            if (!isSync) {
                                if (!extractor.advance()) break
                                continue
                            }
                            seenVideoKeyFrame = true
                            anchorPtsUs = sampleTimeUs.coerceAtLeast(0L)
                        }
                    } else if (!seenVideoKeyFrame) {
                        // 영상 첫 키프레임 이전 오디오 샘플은 싱크 유지를 위해 건너뜀
                        if (!extractor.advance()) break
                        continue
                    }

                    if (anchorPtsUs < 0L) {
                        anchorPtsUs = sampleTimeUs.coerceAtLeast(0L)
                    }

                    if (sampleTimeUs > endUs) {
                        endedTracks.add(inputTrack)
                        if (endedTracks.size >= trackMap.size || sampleTimeUs > endUs + 1_000_000L) {
                            break
                        }
                        if (!extractor.advance()) break
                        continue
                    }

                    var relativePtsUs = (sampleTimeUs - anchorPtsUs).coerceAtLeast(0L)
                    val prev = lastPtsByTrack[outputTrack]
                    if (prev != null && relativePtsUs <= prev) {
                        relativePtsUs = prev + 1L
                    }

                    info.offset = 0
                    info.size = sampleSize
                    info.presentationTimeUs = relativePtsUs
                    info.flags = if (isSync) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

                    muxer.writeSampleData(outputTrack, buffer, info)
                    lastPtsByTrack[outputTrack] = relativePtsUs
                    writtenSamples++

                    if (!extractor.advance()) break
                }

                check(writtenSamples > 0) { "지정한 구간에서 추출된 프레임이 없습니다." }

                muxer.stop()
                muxerStarted = false
                muxer.release()
                muxer = null

                check(tempFile.renameTo(targetFile)) { "결과 파일을 저장하지 못했습니다." }

                // 자막이 있으면 구간 자막도 동반 .srt 로 함께 저장
                val actualStartMs = if (anchorPtsUs >= 0L) anchorPtsUs / 1_000L else startMs
                val trimmedCues = SubtitleParser.trimCues(cues, actualStartMs, endMs)
                if (trimmedCues.isNotEmpty()) {
                    val subOut = File(outDir, "${targetFile.nameWithoutExtension}.srt")
                    runCatching {
                        subOut.writeText(SubtitleParser.toSrt(trimmedCues, useTranslated = false), Charsets.UTF_8)
                    }
                }

                runCatching {
                    MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
                }

                targetFile
            } catch (t: Throwable) {
                runCatching { if (tempFile.exists()) tempFile.delete() }
                throw t
            } finally {
                runCatching { extractor.release() }
                muxer?.let { m ->
                    runCatching { if (muxerStarted) m.stop() }
                    runCatching { m.release() }
                }
            }
        }
    }

    private fun resolveOutputDirectory(context: Context, videoPath: String): File {
        val parent = videoPath.takeIf { it.isNotBlank() }?.let { File(it).parentFile }
        if (parent != null && parent.isDirectory && parent.canWrite()) {
            return parent
        }
        val publicMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (publicMovies != null) {
            val candidate = File(publicMovies, "VideoPlayer_Clips")
            if (candidate.exists() || candidate.mkdirs()) {
                if (candidate.canWrite()) return candidate
            }
        }
        return context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
    }

    private fun readRotationDegrees(context: Context, video: VideoItem): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            if (video.path.isNotBlank() && File(video.path).canRead()) {
                retriever.setDataSource(video.path)
            } else {
                retriever.setDataSource(context, Uri.parse(video.uri))
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } catch (_: Throwable) {
            0
        } finally {
            runCatching { retriever.release() }
        }
    }
}
