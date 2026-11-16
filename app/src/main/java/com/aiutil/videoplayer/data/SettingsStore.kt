package com.aiutil.videoplayer.data

import android.content.Context
import com.aiutil.videoplayer.model.PlayerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 플레이어 제스처, 자막/AI 번역, 정렬 등 앱 설정을 영속화한다.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settings = MutableStateFlow(loadInitial())
    val settings: StateFlow<PlayerSettings> = _settings.asStateFlow()

    private fun loadInitial(): PlayerSettings {
        val raw = prefs.getString(KEY_SETTINGS_JSON, null) ?: return PlayerSettings()
        return runCatching { json.decodeFromString<PlayerSettings>(raw) }
            .getOrDefault(PlayerSettings())
    }

    fun update(transform: (PlayerSettings) -> PlayerSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.edit().putString(KEY_SETTINGS_JSON, json.encodeToString(updated)).apply()
    }

    private companion object {
        const val KEY_SETTINGS_JSON = "settings_json"
    }
}
