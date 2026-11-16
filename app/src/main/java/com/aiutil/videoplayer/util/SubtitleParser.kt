package com.aiutil.videoplayer.util

import com.aiutil.videoplayer.model.SubtitleCue
import java.util.Locale

/**
 * SRT, WebVTT, ASS/SSA 자막 파일 파서 및 SRT 내보내기 변환기.
 */
object SubtitleParser {

    private val HTML_TAG_REGEX = Regex("<[^>]+>")
    private val ASS_TAG_REGEX = Regex("\\{[^}]*\\}")
    private val TIME_SRT_VTT_REGEX = Regex(
        """(?:(\d{1,2}):)?(\d{1,2}):(\d{1,2})[,.](\d{1,3})"""
    )
    private val TIME_ASS_REGEX = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})\.(\d{2})"""
    )

    fun parseAuto(content: String, extension: String = ""): List<SubtitleCue> {
        val clean = content.removePrefix("\uFEFF").trim()
        if (clean.isEmpty()) return emptyList()
        val ext = extension.lowercase(Locale.US).trimStart('.')
        return when {
            ext == "vtt" || clean.startsWith("WEBVTT", ignoreCase = true) -> parseVtt(clean)
            ext == "ass" || ext == "ssa" || clean.contains("[Events]", ignoreCase = true) -> parseAss(clean)
            else -> parseSrt(clean)
        }
    }

    fun parseSrt(content: String): List<SubtitleCue> {
        val normalized = content.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val blocks = normalized.split(Regex("\n{2,}"))
        val result = ArrayList<SubtitleCue>(blocks.size)
        var nextIndex = 1

        for (rawBlock in blocks) {
            val lines = rawBlock.lines().map { it.trimEnd() }.dropWhile { it.isBlank() }
            if (lines.isEmpty()) continue

            val arrowLineIdx = lines.indexOfFirst { it.contains("-->") }
            if (arrowLineIdx < 0) continue

            val times = parseArrowLine(lines[arrowLineIdx]) ?: continue
            val textLines = lines.drop(arrowLineIdx + 1)
            val text = cleanText(textLines.joinToString("\n"))
            if (text.isBlank()) continue

            result.add(
                SubtitleCue(
                    index = nextIndex++,
                    startMs = times.first,
                    endMs = times.second.coerceAtLeast(times.first + 500L),
                    originalText = text,
                )
            )
        }
        return result.sortedBy { it.startMs }
    }

    fun parseVtt(content: String): List<SubtitleCue> {
        val normalized = content.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val blocks = normalized.split(Regex("\n{2,}"))
        val result = ArrayList<SubtitleCue>(blocks.size)
        var nextIndex = 1

        for (rawBlock in blocks) {
            val trimmed = rawBlock.trim()
            if (trimmed.isEmpty() ||
                trimmed.startsWith("WEBVTT", ignoreCase = true) ||
                trimmed.startsWith("NOTE", ignoreCase = true) ||
                trimmed.startsWith("STYLE", ignoreCase = true)
            ) {
                continue
            }
            val lines = trimmed.lines()
            val arrowLineIdx = lines.indexOfFirst { it.contains("-->") }
            if (arrowLineIdx < 0) continue

            val times = parseArrowLine(lines[arrowLineIdx]) ?: continue
            val text = cleanText(lines.drop(arrowLineIdx + 1).joinToString("\n"))
            if (text.isBlank()) continue

            result.add(
                SubtitleCue(
                    index = nextIndex++,
                    startMs = times.first,
                    endMs = times.second.coerceAtLeast(times.first + 500L),
                    originalText = text,
                )
            )
        }
        return result.sortedBy { it.startMs }
    }

    fun parseAss(content: String): List<SubtitleCue> {
        val normalized = content.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val result = ArrayList<SubtitleCue>()
        var formatFields: List<String> = emptyList()
        var inEvents = false
        var nextIndex = 1

        for (rawLine in normalized.lines()) {
            val line = rawLine.trim()
            if (line.startsWith("[") && line.endsWith("]")) {
                inEvents = line.equals("[Events]", ignoreCase = true)
                continue
            }
            if (!inEvents) continue
            if (line.startsWith("Format:", ignoreCase = true)) {
                formatFields = line.substringAfter(":").split(",").map { it.trim().lowercase(Locale.US) }
                continue
            }
            if (!line.startsWith("Dialogue:", ignoreCase = true)) continue

            val payload = line.substringAfter(":")
            val fieldCount = if (formatFields.isNotEmpty()) formatFields.size else 10
            val parts = payload.split(",", limit = fieldCount)
            if (parts.size < 4) continue

            val startIdx = formatFields.indexOf("start").takeIf { it >= 0 } ?: 1
            val endIdx = formatFields.indexOf("end").takeIf { it >= 0 } ?: 2
            val textIdx = formatFields.indexOf("text").takeIf { it >= 0 } ?: (parts.size - 1)

            if (startIdx >= parts.size || endIdx >= parts.size || textIdx >= parts.size) continue
            val startMs = parseAssTimestamp(parts[startIdx].trim()) ?: continue
            val endMs = parseAssTimestamp(parts[endIdx].trim()) ?: continue
            val rawText = parts[textIdx]
                .replace("\\N", "\n")
                .replace("\\n", "\n")
                .replace("\\h", " ")
                .replace(ASS_TAG_REGEX, "")
            val text = cleanText(rawText)
            if (text.isBlank()) continue

            result.add(
                SubtitleCue(
                    index = nextIndex++,
                    startMs = startMs,
                    endMs = endMs.coerceAtLeast(startMs + 500L),
                    originalText = text,
                )
            )
        }
        return result.sortedBy { it.startMs }
    }

    fun findActiveCue(
        cues: List<SubtitleCue>,
        positionMs: Long,
        syncOffsetMs: Long = 0L,
    ): SubtitleCue? {
        if (cues.isEmpty()) return null
        val adjusted = positionMs - syncOffsetMs
        // 이진 탐색으로 시작 시간 <= adjusted 인 마지막 구간 부근 탐색
        var low = 0
        var high = cues.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cue = cues[mid]
            if (adjusted < cue.startMs) {
                high = mid - 1
            } else if (adjusted > cue.endMs) {
                low = mid + 1
            } else {
                return cue
            }
        }
        // 겹치는 자막이 있을 경우를 대비해 인접 구간 보완 검사
        val candidateStart = (high - 3).coerceAtLeast(0)
        val candidateEnd = (low + 1).coerceAtMost(cues.lastIndex)
        for (i in candidateEnd downTo candidateStart) {
            val c = cues[i]
            if (adjusted in c.startMs..c.endMs) return c
        }
        return null
    }

    fun toSrt(cues: List<SubtitleCue>, useTranslated: Boolean = true): String {
        if (cues.isEmpty()) return ""
        return buildString {
            cues.forEachIndexed { idx, cue ->
                val text = if (useTranslated && !cue.translatedText.isNullOrBlank()) {
                    cue.translatedText
                } else {
                    cue.originalText
                }
                append(idx + 1).append('\n')
                append(formatSrtTime(cue.startMs))
                append(" --> ")
                append(formatSrtTime(cue.endMs))
                append('\n')
                append(text).append("\n\n")
            }
        }
    }

    private fun parseArrowLine(line: String): Pair<Long, Long>? {
        val parts = line.split("-->")
        if (parts.size < 2) return null
        val start = parseSrtVttTimestamp(parts[0].trim()) ?: return null
        val endPart = parts[1].trim().split(Regex("\\s+")).firstOrNull() ?: return null
        val end = parseSrtVttTimestamp(endPart) ?: return null
        return start to end
    }

    private fun parseSrtVttTimestamp(token: String): Long? {
        val match = TIME_SRT_VTT_REGEX.find(token) ?: return null
        val g = match.groupValues
        val hasHours = g[1].isNotEmpty()
        val hours = if (hasHours) g[1].toLongOrNull() ?: 0L else 0L
        val minutes = g[2].toLongOrNull() ?: return null
        val seconds = g[3].toLongOrNull() ?: return null
        val millisRaw = g[4].padEnd(3, '0').take(3)
        val millis = millisRaw.toLongOrNull() ?: return null
        return ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L + millis
    }

    private fun parseAssTimestamp(token: String): Long? {
        val match = TIME_ASS_REGEX.find(token) ?: return null
        val g = match.groupValues
        val hours = g[1].toLongOrNull() ?: return null
        val minutes = g[2].toLongOrNull() ?: return null
        val seconds = g[3].toLongOrNull() ?: return null
        val centis = g[4].toLongOrNull() ?: return null
        return ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L + (centis * 10L)
    }

    private fun formatSrtTime(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val hours = safe / 3_600_000L
        val minutes = (safe % 3_600_000L) / 60_000L
        val seconds = (safe % 60_000L) / 1000L
        val millis = safe % 1000L
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    private fun cleanText(raw: String): String {
        return raw
            .replace(HTML_TAG_REGEX, "")
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()
    }
}
