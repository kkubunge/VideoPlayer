package com.aiutil.videoplayer.ai

import android.util.Base64
import com.aiutil.videoplayer.model.TranslationEngineMode
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 동영상 자막 및 음성을 다국어 AI 번역하는 엔진.
 * - ML Kit 온디바이스 번역 (오프라인 지원)
 * - 온라인 자동 번역 폴백
 * - Gemini 멀티모달 AI (텍스트 문맥 번역 + 자막 없는 영상 오디오 전사/번역)
 */
class TranslationEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, String>()
    private val translatorLock = Mutex()
    private val translators = HashMap<String, Translator>()
    private val languageIdentifier by lazy { LanguageIdentification.getClient() }

    data class TranscribedSubtitle(
        val startOffsetMs: Long,
        val endOffsetMs: Long,
        val originalText: String,
        val translatedText: String,
    )

    suspend fun translateText(
        text: String,
        targetLangCode: String,
        mode: TranslationEngineMode,
        geminiApiKey: String,
    ): String {
        val clean = text.trim()
        if (clean.isEmpty()) return ""
        val cacheKey = "${mode.name}:$targetLangCode:$clean"
        cache[cacheKey]?.let { return it }

        val detectedLang = detectLanguage(clean)
        if (detectedLang == targetLangCode.lowercase()) {
            cache[cacheKey] = clean
            return clean
        }

        val translated = when (mode) {
            TranslationEngineMode.GEMINI -> {
                if (geminiApiKey.isNotBlank()) {
                    translateWithGemini(listOf(clean), targetLangCode, geminiApiKey).firstOrNull()
                } else {
                    null
                } ?: translateWithWeb(clean, targetLangCode)
            }

            TranslationEngineMode.ML_KIT -> {
                translateWithMlKit(clean, detectedLang, targetLangCode)
                    ?: translateWithWeb(clean, targetLangCode)
            }

            TranslationEngineMode.WEB -> {
                translateWithWeb(clean, targetLangCode)
            }

            TranslationEngineMode.AUTO -> {
                if (geminiApiKey.isNotBlank()) {
                    translateWithGemini(listOf(clean), targetLangCode, geminiApiKey).firstOrNull()
                } else {
                    null
                } ?: translateWithWeb(clean, targetLangCode)
                    ?: translateWithMlKit(clean, detectedLang, targetLangCode)
            }
        }

        val finalResult = translated?.takeIf { it.isNotBlank() } ?: clean
        cache[cacheKey] = finalResult
        return finalResult
    }

    suspend fun translateBatch(
        texts: List<String>,
        targetLangCode: String,
        mode: TranslationEngineMode,
        geminiApiKey: String,
    ): List<String> {
        if (texts.isEmpty()) return emptyList()

        // Gemini API가 활성화되어 있으면 한 번의 요청으로 문맥을 살려 일괄 번역
        if ((mode == TranslationEngineMode.GEMINI || mode == TranslationEngineMode.AUTO) &&
            geminiApiKey.isNotBlank()
        ) {
            val uncachedIndices = ArrayList<Int>()
            val results = MutableList(texts.size) { "" }
            texts.forEachIndexed { index, raw ->
                val clean = raw.trim()
                val key = "${mode.name}:$targetLangCode:$clean"
                val hit = cache[key]
                if (clean.isEmpty()) {
                    results[index] = ""
                } else if (hit != null) {
                    results[index] = hit
                } else {
                    uncachedIndices.add(index)
                }
            }
            if (uncachedIndices.isNotEmpty()) {
                val batchInputs = uncachedIndices.map { texts[it].trim() }
                val geminiTranslated = translateWithGemini(batchInputs, targetLangCode, geminiApiKey)
                if (geminiTranslated.size == batchInputs.size) {
                    uncachedIndices.forEachIndexed { i, origIndex ->
                        val orig = texts[origIndex].trim()
                        val tr = geminiTranslated[i].ifBlank { orig }
                        cache["${mode.name}:$targetLangCode:$orig"] = tr
                        results[origIndex] = tr
                    }
                    return results
                }
            } else {
                return results
            }
        }

        return texts.map { translateText(it, targetLangCode, mode, geminiApiKey) }
    }

    /**
     * 자막이 없는 영상의 오디오 WAV 청크를 Gemini 멀티모달 모델로 음성 인식 + 번역한다.
     */
    suspend fun transcribeAndTranslateAudio(
        wavBytes: ByteArray,
        targetLangCode: String,
        geminiApiKey: String,
    ): List<TranscribedSubtitle> = withContext(Dispatchers.IO) {
        if (geminiApiKey.isBlank() || wavBytes.size <= 44) return@withContext emptyList()
        runCatching {
            val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
            val prompt = """
                Listen to this video audio segment.
                1. Transcribe spoken dialogue accurately in its original language.
                2. Translate each dialogue line naturally into target language code "$targetLangCode".
                3. Return ONLY a JSON array of objects with fields:
                   "startMs" (integer milliseconds from start of this clip),
                   "endMs" (integer milliseconds from start of this clip),
                   "original" (string),
                   "translated" (string).
                If there is only silence, music, or no speech, return [].
            """.trimIndent()

            val bodyJson = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("text", prompt)
                            })
                            add(buildJsonObject {
                                put("inline_data", buildJsonObject {
                                    put("mime_type", "audio/wav")
                                    put("data", base64Audio)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }.toString()

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${
                    URLEncoder.encode(geminiApiKey.trim(), "UTF-8")
                }"
            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val raw = resp.body?.string() ?: return@withContext emptyList()
                val textPayload = extractGeminiText(raw) ?: return@withContext emptyList()
                val cleanArrayText = extractJsonArray(textPayload) ?: return@withContext emptyList()
                val arr = json.parseToJsonElement(cleanArrayText).jsonArray
                arr.mapNotNull { el ->
                    val obj = el.jsonObject
                    val orig = obj["original"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val trans = obj["translated"]?.jsonPrimitive?.content?.trim().orEmpty()
                    if (orig.isBlank() && trans.isBlank()) return@mapNotNull null
                    val s = obj["startMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    val e = obj["endMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: (s + 3000L)
                    TranscribedSubtitle(
                        startOffsetMs = s.coerceAtLeast(0L),
                        endOffsetMs = e.coerceAtLeast(s + 800L),
                        originalText = orig.ifBlank { trans },
                        translatedText = trans.ifBlank { orig },
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun detectLanguage(text: String): String {
        val sample = text.take(120)
        val mlDetected = runCatching {
            val tag = languageIdentifier.identifyLanguage(sample).await()
            if (tag == "und" || tag.isBlank()) null else tag.substringBefore('-').lowercase()
        }.getOrNull()
        if (mlDetected != null) return mlDetected

        // 간단한 유니코드 휴리스틱 보조 판별
        return when {
            sample.any { it in '\uAC00'..'\uD7A3' } -> "ko"
            sample.any { it in '\u3040'..'\u30FF' } -> "ja"
            sample.any { it in '\u4E00'..'\u9FFF' } -> "zh"
            else -> "en"
        }
    }

    private suspend fun translateWithMlKit(
        text: String,
        sourceLangCode: String,
        targetLangCode: String,
    ): String? {
        val src = TranslateLanguage.fromLanguageTag(sourceLangCode) ?: TranslateLanguage.ENGLISH
        val dst = TranslateLanguage.fromLanguageTag(targetLangCode) ?: return null
        if (src == dst) return text

        return runCatching {
            val translator = getOrCreateTranslator(src, dst)
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
            translator.translate(text).await()
        }.getOrNull()
    }

    private suspend fun getOrCreateTranslator(src: String, dst: String): Translator {
        val key = "$src->$dst"
        return translatorLock.withLock {
            translators.getOrPut(key) {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(src)
                    .setTargetLanguage(dst)
                    .build()
                Translation.getClient(options)
            }
        }
    }

    private suspend fun translateWithWeb(text: String, targetLangCode: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val url =
                    "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${
                        URLEncoder.encode(targetLangCode, "UTF-8")
                    }&dt=t&q=$encoded"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val body = resp.body?.string() ?: return@withContext null
                    val root = JSONArray(body)
                    val segments = root.optJSONArray(0) ?: return@withContext null
                    buildString {
                        for (i in 0 until segments.length()) {
                            val part = segments.optJSONArray(i)?.optString(0).orEmpty()
                            append(part)
                        }
                    }.trim().ifBlank { null }
                }
            }.getOrNull()
        }

    private suspend fun translateWithGemini(
        lines: List<String>,
        targetLangCode: String,
        geminiApiKey: String,
    ): List<String> = withContext(Dispatchers.IO) {
        if (lines.isEmpty() || geminiApiKey.isBlank()) return@withContext emptyList()
        runCatching {
            val indexedInput = buildJsonArray {
                lines.forEach { add(JsonPrimitive(it)) }
            }.toString()

            val prompt = """
                Translate the following JSON array of video subtitle lines naturally into language code "$targetLangCode".
                Keep line breaks inside each subtitle if appropriate.
                Return ONLY a JSON array of translated strings with the exact same length (${lines.size}) and order.
                Input:
                $indexedInput
            """.trimIndent()

            val bodyJson = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }.toString()

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${
                    URLEncoder.encode(geminiApiKey.trim(), "UTF-8")
                }"
            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val raw = resp.body?.string() ?: return@withContext emptyList()
                val textPayload = extractGeminiText(raw) ?: return@withContext emptyList()
                val arrText = extractJsonArray(textPayload) ?: return@withContext emptyList()
                val arr = json.parseToJsonElement(arrText).jsonArray
                arr.map { it.jsonPrimitive.content }
            }
        }.getOrDefault(emptyList())
    }

    private fun extractGeminiText(responseBody: String): String? {
        return runCatching {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val candidates = root["candidates"] as? JsonArray ?: return null
            val first = candidates.firstOrNull() as? JsonObject ?: return null
            val content = first["content"] as? JsonObject ?: return null
            val parts = content["parts"] as? JsonArray ?: return null
            parts.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
        }.getOrNull()
    }

    private fun extractJsonArray(raw: String): String? {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    fun close() {
        translators.values.forEach { runCatching { it.close() } }
        translators.clear()
        runCatching { languageIdentifier.close() }
    }
}
