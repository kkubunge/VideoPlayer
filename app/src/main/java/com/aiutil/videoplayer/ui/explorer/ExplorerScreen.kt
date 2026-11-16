package com.aiutil.videoplayer.ui.explorer

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aiutil.videoplayer.App
import com.aiutil.videoplayer.files.FileExplorerManager
import com.aiutil.videoplayer.files.VideoScanner
import com.aiutil.videoplayer.model.ClipboardOp
import com.aiutil.videoplayer.model.ExplorerEntry
import com.aiutil.videoplayer.model.ExplorerTab
import com.aiutil.videoplayer.model.FileClipboard
import com.aiutil.videoplayer.model.PlaybackHistoryItem
import com.aiutil.videoplayer.model.SortOrder
import com.aiutil.videoplayer.model.VideoFolder
import com.aiutil.videoplayer.model.VideoItem
import com.aiutil.videoplayer.ui.components.VideoThumbnail
import com.aiutil.videoplayer.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExplorerScreen(
    hasReadPermission: Boolean,
    hasAllFilesAccess: Boolean,
    refreshSignal: Int,
    onRequestReadPermission: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by App.settings.settings.collectAsState()
    val historyItems by App.history.items.collectAsState()

    var selectedTab by remember { mutableStateOf(ExplorerTab.FOLDERS) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var allVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var localRefresh by remember { mutableIntStateOf(0) }

    // 동영상 폴더 탭 내부 진입 폴더 경로 (null 이면 폴더 목록 화면)
    var openedVideoFolderPath by remember { mutableStateOf<String?>(null) }

    // 파일 탐색기 탭 현재 디렉토리
    val storageRoot = remember {
        Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
    }
    var currentDirPath by remember { mutableStateOf(storageRoot) }
    var directoryEntries by remember { mutableStateOf<List<ExplorerEntry>>(emptyList()) }

    // 다중 선택 및 클립보드 (복사 / 이동)
    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var clipboard by remember { mutableStateOf<FileClipboard?>(null) }

    // 다이얼로그 상태
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTargetPath by remember { mutableStateOf<String?>(null) }
    var deleteTargets by remember { mutableStateOf<List<String>?>(null) }

    fun triggerRefresh() {
        localRefresh++
    }

    LaunchedEffect(hasReadPermission, hasAllFilesAccess, refreshSignal, localRefresh) {
        if (!hasReadPermission) {
            allVideos = emptyList()
            return@LaunchedEffect
        }
        isScanning = true
        val scanned = withContext(Dispatchers.IO) {
            VideoScanner.scanVideos(context)
        }
        allVideos = scanned
        isScanning = false
    }

    val videoIndex = remember(allVideos) {
        allVideos.filter { it.path.isNotBlank() }.associateBy { it.path }
    }

    LaunchedEffect(currentDirPath, settings.sortOrder, videoIndex, localRefresh, refreshSignal, hasReadPermission) {
        if (!hasReadPermission) {
            directoryEntries = emptyList()
            return@LaunchedEffect
        }
        directoryEntries = withContext(Dispatchers.IO) {
            FileExplorerManager.listDirectory(
                dir = File(currentDirPath),
                sortOrder = settings.sortOrder,
                videoIndex = videoIndex,
            )
        }
    }

    val folders = remember(allVideos, settings.sortOrder, searchQuery) {
        val grouped = VideoScanner.groupByFolder(allVideos, settings.sortOrder)
        if (searchQuery.isBlank()) {
            grouped
        } else {
            val q = searchQuery.trim().lowercase()
            grouped.mapNotNull { folder ->
                val matched = folder.videos.filter { it.title.lowercase().contains(q) }
                if (folder.name.lowercase().contains(q)) {
                    folder
                } else if (matched.isNotEmpty()) {
                    folder.copy(videos = matched)
                } else {
                    null
                }
            }
        }
    }

    val activeVideoFolder: VideoFolder? = remember(folders, openedVideoFolderPath) {
        openedVideoFolderPath?.let { p -> folders.firstOrNull { it.path == p } }
    }

    val filteredEntries = remember(directoryEntries, searchQuery) {
        if (searchQuery.isBlank()) {
            directoryEntries
        } else {
            val q = searchQuery.trim().lowercase()
            directoryEntries.filter { it.name.lowercase().contains(q) }
        }
    }

    fun toggleSelect(path: String) {
        if (path.isBlank()) return
        selectedPaths = if (selectedPaths.contains(path)) {
            selectedPaths - path
        } else {
            selectedPaths + path
        }
    }

    fun startClipboard(op: ClipboardOp, paths: List<String>) {
        val valid = paths.filter { it.isNotBlank() }.distinct()
        if (valid.isEmpty()) {
            Toast.makeText(context, "파일 경로가 없는 항목은 이동/복사할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        clipboard = FileClipboard(op, valid)
        selectedPaths = emptySet()
        // 파일 탐색기 탭으로 전환하면 원하는 폴더로 바로 이동해 붙여넣기 가능
        selectedTab = ExplorerTab.FILES
        Toast.makeText(
            context,
            "${valid.size}개 항목 ${op.label} 준비됨 · 대상 폴더로 이동해 '붙여넣기'를 누르세요.",
            Toast.LENGTH_LONG
        ).show()
    }

    fun pasteToCurrentDirectory() {
        val cb = clipboard ?: return
        val destDir = File(currentDirPath)
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                FileExplorerManager.copyOrMove(
                    sources = cb.sourcePaths.map { File(it) },
                    destDir = destDir,
                    operation = cb.operation,
                    context = context,
                )
            }
            res.onSuccess { count ->
                Toast.makeText(
                    context,
                    "${count}개 항목 ${cb.operation.label} 완료",
                    Toast.LENGTH_SHORT
                ).show()
                clipboard = null
                triggerRefresh()
            }.onFailure { err ->
                Toast.makeText(
                    context,
                    err.message ?: "${cb.operation.label} 실패",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            if (selectedPaths.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedPaths.size}개 선택됨") },
                    navigationIcon = {
                        IconButton(onClick = { selectedPaths = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "선택 취소")
                        }
                    },
                    actions = {
                        IconButton(onClick = { startClipboard(ClipboardOp.COPY, selectedPaths.toList()) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "복사")
                        }
                        IconButton(onClick = { startClipboard(ClipboardOp.MOVE, selectedPaths.toList()) }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "이동")
                        }
                        IconButton(onClick = { deleteTargets = selectedPaths.toList() }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        if (selectedTab == ExplorerTab.FOLDERS && activeVideoFolder != null) {
                            Text(
                                text = activeVideoFolder.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text("MX AI 플레이어")
                        }
                    },
                    navigationIcon = {
                        if (selectedTab == ExplorerTab.FOLDERS && activeVideoFolder != null) {
                            IconButton(onClick = { openedVideoFolderPath = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "폴더 목록으로")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchVisible = !searchVisible }) {
                            Icon(Icons.Default.Search, contentDescription = "검색")
                        }
                        if (selectedTab == ExplorerTab.FILES) {
                            IconButton(onClick = { showNewFolderDialog = true }) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = "새 폴더")
                            }
                        }
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "정렬")
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (settings.sortOrder == order) "✓ ${order.label}" else order.label
                                            )
                                        },
                                        onClick = {
                                            sortMenuExpanded = false
                                            App.settings.update { it.copy(sortOrder = order) }
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { triggerRefresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "설정")
                        }
                    },
                )
            }
        },
        bottomBar = {
            val cb = clipboard
            if (cb != null) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${cb.sourcePaths.size}개 ${cb.operation.label} 대기 중",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "대상: ${File(currentDirPath).name.ifBlank { currentDirPath }}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { clipboard = null }) {
                                Text("취소")
                            }
                            Button(
                                onClick = {
                                    if (selectedTab != ExplorerTab.FILES) {
                                        selectedTab = ExplorerTab.FILES
                                    } else {
                                        pasteToCurrentDirectory()
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (selectedTab == ExplorerTab.FILES) "여기에 붙여넣기" else "폴더 선택")
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!hasReadPermission) {
                PermissionCard(
                    title = "동영상 저장소 읽기 권한이 필요합니다",
                    description = "기기 내 동영상 폴더와 파일을 불러오려면 미디어 읽기 권한을 허용해 주세요.",
                    buttonText = "권한 허용하기",
                    onClick = onRequestReadPermission,
                )
            } else if (!hasAllFilesAccess) {
                PermissionCard(
                    title = "폴더/파일 이동·복사·삭제 권한 안내",
                    description = "모든 폴더 탐색과 파일 이동/복사/이름변경을 자유롭게 쓰려면 '모든 파일에 대한 액세스'를 허용해 주세요.",
                    buttonText = "전체 파일 권한 설정",
                    onClick = onRequestAllFilesAccess,
                )
            }

            if (searchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("폴더 또는 파일 이름 검색") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "지우기")
                            }
                        }
                    },
                )
            }

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ExplorerTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            selectedPaths = emptySet()
                        },
                        text = { Text(tab.label) },
                    )
                }
            }

            if (isScanning && allVideos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("동영상 폴더 스캔 중...")
                    }
                }
            } else {
                when (selectedTab) {
                    ExplorerTab.FOLDERS -> {
                        if (activeVideoFolder != null) {
                            VideoListContent(
                                videos = activeVideoFolder.videos,
                                historyItems = historyItems,
                                selectedPaths = selectedPaths,
                                onVideoClick = { video ->
                                    if (selectedPaths.isNotEmpty()) {
                                        toggleSelect(video.path)
                                    } else {
                                        onPlayVideo(video, activeVideoFolder.videos)
                                    }
                                },
                                onVideoLongClick = { video -> toggleSelect(video.path) },
                                onCopy = { v -> startClipboard(ClipboardOp.COPY, listOf(v.path)) },
                                onMove = { v -> startClipboard(ClipboardOp.MOVE, listOf(v.path)) },
                                onRename = { v ->
                                    if (v.path.isNotBlank()) renameTargetPath = v.path
                                },
                                onDelete = { v ->
                                    if (v.path.isNotBlank()) {
                                        deleteTargets = listOf(v.path)
                                    } else {
                                        scope.launch {
                                            FileExplorerManager.deleteEntries(
                                                targets = emptyList(),
                                                context = context,
                                                fallbackUris = listOf(v.uri),
                                            )
                                            triggerRefresh()
                                        }
                                    }
                                },
                            )
                        } else {
                            FolderListContent(
                                folders = folders,
                                selectedPaths = selectedPaths,
                                onFolderClick = { folder ->
                                    if (selectedPaths.isNotEmpty()) {
                                        toggleSelect(folder.path)
                                    } else {
                                        openedVideoFolderPath = folder.path
                                    }
                                },
                                onFolderLongClick = { folder -> toggleSelect(folder.path) },
                                onOpenInExplorer = { folder ->
                                    if (File(folder.path).isDirectory) {
                                        currentDirPath = folder.path
                                        selectedTab = ExplorerTab.FILES
                                    }
                                },
                                onCopy = { folder -> startClipboard(ClipboardOp.COPY, listOf(folder.path)) },
                                onMove = { folder -> startClipboard(ClipboardOp.MOVE, listOf(folder.path)) },
                                onRename = { folder -> renameTargetPath = folder.path },
                                onDelete = { folder -> deleteTargets = listOf(folder.path) },
                            )
                        }
                    }

                    ExplorerTab.FILES -> {
                        FileExplorerContent(
                            storageRoot = storageRoot,
                            currentDirPath = currentDirPath,
                            entries = filteredEntries,
                            selectedPaths = selectedPaths,
                            onNavigateDir = { newPath ->
                                currentDirPath = newPath
                                selectedPaths = emptySet()
                            },
                            onEntryClick = { entry ->
                                if (selectedPaths.isNotEmpty()) {
                                    toggleSelect(entry.path)
                                } else if (entry.isDirectory) {
                                    currentDirPath = entry.path
                                } else if (entry.isVideo && entry.videoItem != null) {
                                    val folderPlaylist = filteredEntries
                                        .mapNotNull { it.videoItem }
                                        .ifEmpty { listOf(entry.videoItem) }
                                    onPlayVideo(entry.videoItem, folderPlaylist)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "동영상 파일만 재생할 수 있습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onEntryLongClick = { entry -> toggleSelect(entry.path) },
                            onCopy = { entry -> startClipboard(ClipboardOp.COPY, listOf(entry.path)) },
                            onMove = { entry -> startClipboard(ClipboardOp.MOVE, listOf(entry.path)) },
                            onRename = { entry -> renameTargetPath = entry.path },
                            onDelete = { entry -> deleteTargets = listOf(entry.path) },
                        )
                    }

                    ExplorerTab.RECENT -> {
                        RecentHistoryContent(
                            historyItems = historyItems,
                            allVideos = allVideos,
                            onPlayHistory = { hist ->
                                val matched = allVideos.firstOrNull {
                                    it.id == hist.id || (hist.path.isNotBlank() && it.path == hist.path)
                                } ?: VideoItem(
                                    id = hist.id,
                                    title = hist.title,
                                    uri = hist.uri,
                                    path = hist.path,
                                    folderPath = hist.folderPath,
                                    folderName = File(hist.folderPath).name.ifBlank { "최근 재생" },
                                    sizeBytes = 0L,
                                    durationMs = hist.durationMs,
                                    dateModifiedSec = hist.lastPlayedAtMs / 1000L,
                                )
                                val playlist = allVideos.filter { it.folderPath == matched.folderPath }
                                    .ifEmpty { listOf(matched) }
                                onPlayVideo(matched, playlist)
                            },
                            onRemoveItem = { App.history.removeByPathOrId(it.id) },
                            onClearAll = { App.history.clearAll() },
                        )
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        TextInputDialog(
            title = "새 폴더 만들기",
            initialText = "새 폴더",
            confirmLabel = "만들기",
            onConfirm = { folderName ->
                showNewFolderDialog = false
                FileExplorerManager.createFolder(File(currentDirPath), folderName)
                    .onSuccess {
                        Toast.makeText(context, "폴더가 생성되었습니다.", Toast.LENGTH_SHORT).show()
                        triggerRefresh()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "생성 실패", Toast.LENGTH_LONG).show()
                    }
            },
            onDismiss = { showNewFolderDialog = false },
        )
    }

    val renameTarget = renameTargetPath
    if (renameTarget != null) {
        val targetFile = File(renameTarget)
        TextInputDialog(
            title = "이름 변경",
            initialText = targetFile.name,
            confirmLabel = "변경",
            onConfirm = { newName ->
                renameTargetPath = null
                FileExplorerManager.renameEntry(targetFile, newName, context)
                    .onSuccess {
                        Toast.makeText(context, "이름이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        triggerRefresh()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "이름 변경 실패", Toast.LENGTH_LONG).show()
                    }
            },
            onDismiss = { renameTargetPath = null },
        )
    }

    val pendingDelete = deleteTargets
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTargets = null },
            title = { Text("삭제 확인") },
            text = {
                val preview = if (pendingDelete.size == 1) {
                    "'${File(pendingDelete.first()).name}' 항목을 삭제하시겠습니까?"
                } else {
                    "선택한 ${pendingDelete.size}개 항목을 삭제하시겠습니까?"
                }
                Text("$preview\n이 작업은 되돌릴 수 없습니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targets = pendingDelete
                        deleteTargets = null
                        selectedPaths = emptySet()
                        scope.launch {
                            val res = withContext(Dispatchers.IO) {
                                FileExplorerManager.deleteEntries(
                                    targets = targets.map { File(it) },
                                    context = context,
                                )
                            }
                            res.onSuccess { count ->
                                targets.forEach { App.history.removeByPathOrId(it) }
                                Toast.makeText(context, "${count}개 항목 삭제됨", Toast.LENGTH_SHORT).show()
                                triggerRefresh()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "삭제 실패", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargets = null }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderListContent(
    folders: List<VideoFolder>,
    selectedPaths: Set<String>,
    onFolderClick: (VideoFolder) -> Unit,
    onFolderLongClick: (VideoFolder) -> Unit,
    onOpenInExplorer: (VideoFolder) -> Unit,
    onCopy: (VideoFolder) -> Unit,
    onMove: (VideoFolder) -> Unit,
    onRename: (VideoFolder) -> Unit,
    onDelete: (VideoFolder) -> Unit,
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("표시할 동영상 폴더가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.path }) { folder ->
            val isSelected = selectedPaths.contains(folder.path)
            var menuExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onFolderClick(folder) },
                        onLongClick = { onFolderLongClick(folder) },
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedPaths.isNotEmpty()) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "동영상 ${folder.videos.size}개 · ${Format.size(folder.totalSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = folder.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "폴더 작업")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("탐색기에서 열기") },
                                onClick = { menuExpanded = false; onOpenInExplorer(folder) },
                            )
                            DropdownMenuItem(
                                text = { Text("복사") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                onClick = { menuExpanded = false; onCopy(folder) },
                            )
                            DropdownMenuItem(
                                text = { Text("이동") },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, null) },
                                onClick = { menuExpanded = false; onMove(folder) },
                            )
                            DropdownMenuItem(
                                text = { Text("이름 변경") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { menuExpanded = false; onRename(folder) },
                            )
                            DropdownMenuItem(
                                text = { Text("폴더 삭제", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { menuExpanded = false; onDelete(folder) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoListContent(
    videos: List<VideoItem>,
    historyItems: List<PlaybackHistoryItem>,
    selectedPaths: Set<String>,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onCopy: (VideoItem) -> Unit,
    onMove: (VideoItem) -> Unit,
    onRename: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit,
) {
    val historyMap = remember(historyItems) { historyItems.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(videos, key = { it.id }) { video ->
            val isSelected = video.path.isNotBlank() && selectedPaths.contains(video.path)
            val hist = historyMap[video.id] ?: historyMap[video.path]
            val hasSubtitle = remember(video.path) {
                VideoScanner.findCompanionSubtitles(video.path).isNotEmpty()
            }
            var menuExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onVideoClick(video) },
                        onLongClick = { onVideoLongClick(video) },
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedPaths.isNotEmpty()) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 112.dp, height = 66.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    ) {
                        VideoThumbnail(
                            uri = video.uri,
                            path = video.path,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (video.durationMs > 0L) {
                            Text(
                                text = Format.duration(video.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                                        RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = Format.size(video.sizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val res = Format.resolution(video.width, video.height)
                            if (res.isNotEmpty()) {
                                Text(
                                    text = res,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (hasSubtitle) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "자막 있음",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (hist != null && hist.durationMs > 0L && hist.positionMs > 0L) {
                            val ratio = (hist.positionMs.toFloat() / hist.durationMs.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "작업 메뉴")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("재생") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                                onClick = { menuExpanded = false; onVideoClick(video) },
                            )
                            if (video.path.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("복사") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                    onClick = { menuExpanded = false; onCopy(video) },
                                )
                                DropdownMenuItem(
                                    text = { Text("이동") },
                                    leadingIcon = { Icon(Icons.Default.DriveFileMove, null) },
                                    onClick = { menuExpanded = false; onMove(video) },
                                )
                                DropdownMenuItem(
                                    text = { Text("이름 변경") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { menuExpanded = false; onRename(video) },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("삭제", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { menuExpanded = false; onDelete(video) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileExplorerContent(
    storageRoot: String,
    currentDirPath: String,
    entries: List<ExplorerEntry>,
    selectedPaths: Set<String>,
    onNavigateDir: (String) -> Unit,
    onEntryClick: (ExplorerEntry) -> Unit,
    onEntryLongClick: (ExplorerEntry) -> Unit,
    onCopy: (ExplorerEntry) -> Unit,
    onMove: (ExplorerEntry) -> Unit,
    onRename: (ExplorerEntry) -> Unit,
    onDelete: (ExplorerEntry) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 경로 브레드크럼 바
        val relative = currentDirPath.removePrefix(storageRoot).trimStart('/')
        val segments = if (relative.isEmpty()) emptyList() else relative.split('/')

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (currentDirPath != storageRoot && File(currentDirPath).parent != null) {
                OutlinedButton(
                    onClick = {
                        val parent = File(currentDirPath).parent ?: storageRoot
                        onNavigateDir(parent)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("상위")
                }
            }
            FilterChip(
                selected = segments.isEmpty(),
                onClick = { onNavigateDir(storageRoot) },
                label = { Text("내부 저장소") },
            )
            var running = storageRoot
            segments.forEachIndexed { idx, seg ->
                running = "$running/$seg"
                val targetPath = running
                FilterChip(
                    selected = idx == segments.lastIndex,
                    onClick = { onNavigateDir(targetPath) },
                    label = { Text(seg) },
                )
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "빈 폴더이거나 접근 권한이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(entries, key = { it.path }) { entry ->
                val isSelected = selectedPaths.contains(entry.path)
                var menuExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEntryClick(entry) },
                            onLongClick = { onEntryLongClick(entry) },
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectedPaths.isNotEmpty()) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        when {
                            entry.isDirectory -> {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                            entry.isVideo -> {
                                VideoThumbnail(
                                    uri = entry.videoItem?.uri,
                                    path = entry.path,
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 42.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                            }
                            entry.isSubtitle -> {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val subInfo = if (entry.isDirectory) {
                                "항목 ${entry.childCount}개"
                            } else {
                                val dur = entry.videoItem?.durationMs?.takeIf { it > 0L }?.let {
                                    " · ${Format.duration(it)}"
                                }.orEmpty()
                                "${Format.size(entry.sizeBytes)}$dur"
                            }
                            Text(
                                text = subInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "파일 메뉴")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                if (entry.isVideo || entry.isDirectory) {
                                    DropdownMenuItem(
                                        text = { Text(if (entry.isDirectory) "열기" else "재생") },
                                        onClick = { menuExpanded = false; onEntryClick(entry) },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("복사") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                    onClick = { menuExpanded = false; onCopy(entry) },
                                )
                                DropdownMenuItem(
                                    text = { Text("이동") },
                                    leadingIcon = { Icon(Icons.Default.DriveFileMove, null) },
                                    onClick = { menuExpanded = false; onMove(entry) },
                                )
                                DropdownMenuItem(
                                    text = { Text("이름 변경") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                                    onClick = { menuExpanded = false; onRename(entry) },
                                )
                                DropdownMenuItem(
                                    text = { Text("삭제", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = { menuExpanded = false; onDelete(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentHistoryContent(
    historyItems: List<PlaybackHistoryItem>,
    allVideos: List<VideoItem>,
    onPlayHistory: (PlaybackHistoryItem) -> Unit,
    onRemoveItem: (PlaybackHistoryItem) -> Unit,
    onClearAll: () -> Unit,
) {
    if (historyItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text("최근 재생 기록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClearAll) {
                    Text("전체 기록 지우기")
                }
            }
        }
        items(historyItems, key = { it.id }) { item ->
            val matchedVideo = allVideos.firstOrNull { it.id == item.id }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayHistory(item) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VideoThumbnail(
                        uri = matchedVideo?.uri ?: item.uri,
                        path = item.path,
                        modifier = Modifier
                            .size(width = 96.dp, height = 56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val durText = if (item.durationMs > 0L) {
                            "${Format.duration(item.positionMs)} / ${Format.duration(item.durationMs)}"
                        } else {
                            Format.duration(item.positionMs)
                        }
                        Text(
                            text = durText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (item.durationMs > 0L) {
                            LinearProgressIndicator(
                                progress = {
                                    (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                            )
                        }
                    }
                    IconButton(onClick = { onRemoveItem(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "기록 삭제")
                    }
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initialText: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}
