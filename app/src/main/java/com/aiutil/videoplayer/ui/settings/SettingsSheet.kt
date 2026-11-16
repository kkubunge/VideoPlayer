package com.aiutil.videoplayer.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aiutil.videoplayer.BuildConfig
import com.aiutil.videoplayer.model.PlayerSettings
import com.aiutil.videoplayer.model.SeekUnit
import com.aiutil.videoplayer.model.SupportedLanguages
import com.aiutil.videoplayer.model.TranslationEngineMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsSheet(
    settings: PlayerSettings,
    onUpdate: ((PlayerSettings) -> PlayerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showApiKey by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "플레이어 & AI 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider()

            SeekStepSelector(
                title = "더블터치 앞/뒤 이동 설정",
                unit = settings.doubleTapUnit,
                seconds = settings.doubleTapSeconds,
                frames = settings.doubleTapFrames,
                secondsLabelPrefix = "더블터치 이동 시간",
                framesLabelPrefix = "더블터치 이동 프레임",
                secondOptions = listOf(3, 5, 10, 15, 20, 30),
                frameOptions = listOf(1, 5, 10, 15, 30, 60),
                onUnitChange = { unit -> onUpdate { it.copy(doubleTapUnit = unit) } },
                onSecondsChange = { sec -> onUpdate { it.copy(doubleTapSeconds = sec) } },
                onFramesChange = { fr -> onUpdate { it.copy(doubleTapFrames = fr) } },
            )

            HorizontalDivider()

            SeekStepSelector(
                title = "좌/우 드래그 & 앞/뒤 이동 설정",
                unit = settings.seekMoveUnit,
                seconds = settings.seekMoveSeconds,
                frames = settings.seekMoveFrames,
                secondsLabelPrefix = "좌/우 이동 범위",
                framesLabelPrefix = "좌/우 이동 프레임",
                secondOptions = listOf(3, 5, 10, 15, 30, 60, 120, 300),
                frameOptions = listOf(1, 5, 10, 15, 30, 60, 120),
                onUnitChange = { unit -> onUpdate { it.copy(seekMoveUnit = unit) } },
                onSecondsChange = { sec -> onUpdate { it.copy(seekMoveSeconds = sec) } },
                onFramesChange = { fr -> onUpdate { it.copy(seekMoveFrames = fr) } },
            )

            HorizontalDivider()

            Text(
                text = "재생 및 제스처",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            SettingsSwitchRow(
                title = "이어보기 (마지막 재생 위치 저장)",
                subtitle = "동영상 재실행 시 마지막 시청 지점부터 재생합니다.",
                checked = settings.resumePlayback,
                onCheckedChange = { checked -> onUpdate { it.copy(resumePlayback = checked) } },
            )

            SettingsSwitchRow(
                title = "길게 터치 배속 조절 후 x1.0 자동 복귀",
                subtitle = "끄면 드래그로 맞춘 배속(x0.5~x6.0)이 손을 뗀 후에도 유지됩니다.",
                checked = settings.resetSpeedOnLongPressRelease,
                onCheckedChange = { checked ->
                    onUpdate { it.copy(resetSpeedOnLongPressRelease = checked) }
                },
            )

            HorizontalDivider()

            Text(
                text = "AI 자막 자동 번역",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            SettingsSwitchRow(
                title = "AI 자동 번역 기본 활성화",
                subtitle = "자막이나 오디오를 설정된 언어로 실시간 번역합니다.",
                checked = settings.aiTranslateEnabled,
                onCheckedChange = { checked -> onUpdate { it.copy(aiTranslateEnabled = checked) } },
            )

            SettingsSwitchRow(
                title = "이중 자막 (원문 + 번역 동시 표시)",
                subtitle = "번역문 위에 원본 자막을 작게 함께 표시합니다.",
                checked = settings.dualSubtitles,
                onCheckedChange = { checked -> onUpdate { it.copy(dualSubtitles = checked) } },
            )

            SettingsSwitchRow(
                title = "자막 없는 영상 오디오 AI 전사·번역",
                subtitle = "Gemini API 키가 있을 때 음성을 추출해 자동 자막을 만듭니다.",
                checked = settings.autoTranscribeNoSubtitle,
                onCheckedChange = { checked ->
                    onUpdate { it.copy(autoTranscribeNoSubtitle = checked) }
                },
            )

            Text(
                text = "번역 대상 언어: ${SupportedLanguages.displayName(settings.targetLangCode)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SupportedLanguages.items.forEach { (code, name) ->
                    FilterChip(
                        selected = settings.targetLangCode == code,
                        onClick = { onUpdate { it.copy(targetLangCode = code) } },
                        label = { Text(name) },
                    )
                }
            }

            Text(
                text = "자막 크기: ${settings.subtitleFontSizeSp}sp",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.subtitleFontSizeSp.toFloat(),
                onValueChange = { v ->
                    onUpdate { it.copy(subtitleFontSizeSp = v.roundToInt().coerceIn(12, 32)) }
                },
                valueRange = 12f..32f,
                steps = 19,
            )

            Text(
                text = "번역 엔진",
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TranslationEngineMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.translationEngine == mode,
                        onClick = { onUpdate { it.copy(translationEngine = mode) } },
                        label = { Text(mode.label) },
                    )
                }
            }

            OutlinedTextField(
                value = settings.geminiApiKey,
                onValueChange = { key -> onUpdate { it.copy(geminiApiKey = key) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini API 키 (선택 - 무자막 음성 번역용)") },
                placeholder = { Text("AQ.... 또는 AIza...") },
                singleLine = true,
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { showApiKey = !showApiKey }) {
                        Text(if (showApiKey) "숨김" else "보기")
                    }
                },
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "빌드 ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_ID})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SeekStepSelector(
    title: String,
    unit: SeekUnit,
    seconds: Int,
    frames: Int,
    secondsLabelPrefix: String,
    framesLabelPrefix: String,
    secondOptions: List<Int>,
    frameOptions: List<Int>,
    onUnitChange: (SeekUnit) -> Unit,
    onSecondsChange: (Int) -> Unit,
    onFramesChange: (Int) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SeekUnit.entries.forEach { item ->
            FilterChip(
                selected = unit == item,
                onClick = { onUnitChange(item) },
                label = { Text(item.label) },
            )
        }
    }

    if (unit == SeekUnit.SECONDS) {
        Text(
            text = "$secondsLabelPrefix: ${seconds}초",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            secondOptions.forEach { sec ->
                FilterChip(
                    selected = seconds == sec,
                    onClick = { onSecondsChange(sec) },
                    label = { Text("${sec}초") },
                )
            }
        }
    } else {
        Text(
            text = "$framesLabelPrefix: ${frames}프레임",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            frameOptions.forEach { count ->
                FilterChip(
                    selected = frames == count,
                    onClick = { onFramesChange(count) },
                    label = { Text("${count}F") },
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
