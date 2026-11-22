package com.aiutil.videoplayer.ui.explorer

import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
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
import com.aiutil.videoplayer.model.FavoriteFolderItem
import com.aiutil.videoplayer.model.FavoriteVideoItem
import com.aiutil.videoplayer.model.FileClipboard
import com.aiutil.videoplayer.model.PlaybackHistoryItem
import com.aiutil.videoplayer.model.SortOrder
import com.aiutil.videoplayer.model.VideoBookmarkItem
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
    onPlayVideo: (VideoItem, List<VideoItem>, Long?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by App.settings.settings.collectAsState()
    val historyItems by App.history.items.collectAsState()
    val favoriteVideos by App.bookmarks.favoriteVideos.collectAsState()
    val favoriteFolders by App.bookmarks.favoriteFolders.collectAsState()
    val videoBookmarks by App.bookmarks.videoBookmarks.collectAsState()

    val favoriteVideoIds = remember(favoriteVideos) {
        buildSet {
            favoriteVideos.forEach {
                add(it.id)
                if (it.path.isNotBlank()) add(it.path)
            }
        }
    }
    val favoriteFolderPaths = remember(favoriteFolders) {
        favoriteFolders.map { it.path }.toSet()
    }

    var selectedTab by remember { mutableStateOf(ExplorerTab.FOLDERS) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var allVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var isLoadingDirectory by remember { mutableStateOf(false) }
    var localRefresh by remember { mutableIntStateOf(0) }

    // 동영상 폴더 탭 내부 진입 폴더 경로 (null 이면 폴더 목록 화면)
    var openedVideoFolderPath by remember { mutableStateOf<String?>(null) }
    var folderSubtitleBases by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 파일 탐색기 탭 현재 디렉토리
    val storageRoot = remember {
        Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
    }
    var currentDirPath by remember { mutableStateOf(storageRoot) }

    // 목록 스크롤 위치 유지용 상태
    val folderListState = rememberLazyListState()
    val videoListStates = remember { mutableMapOf<String, LazyListState>() }
    val videoListState = remember(openedVideoFolderPath) {
        videoListStates.getOrPut(openedVideoFolderPath.orEmpty()) { LazyListState() }
    }
    val dirListStates = remember { mutableMapOf<String, LazyListState>() }
    val dirListState = remember(currentDirPath) {
        dirListStates.getOrPut(currentDirPath) { LazyListState() }
    }
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

    // 뒤로가기 버튼 처리: 폴더나 선택/검색 상태에서는 앱 종료 대신 이전 화면·상위 폴더로 이동
    val canHandleBack = selectedPaths.isNotEmpty() ||
        searchVisible ||
        (selectedTab == ExplorerTab.FOLDERS && openedVideoFolderPath != null) ||
        (selectedTab == ExplorerTab.FILES && currentDirPath != storageRoot) ||
        selectedTab != ExplorerTab.FOLDERS

    BackHandler(enabled = canHandleBack) {
        when {
            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
            searchVisible -> {
                searchVisible = false
                searchQuery = ""
            }
            selectedTab == ExplorerTab.FOLDERS && openedVideoFolderPath != null -> {
                openedVideoFolderPath = null
            }
            selectedTab == ExplorerTab.FILES && currentDirPath != storageRoot -> {
                val parent = File(currentDirPath).parent
                currentDirPath = if (parent != null && parent.length >= storageRoot.length) {
                    parent
                } else {
                    storageRoot
                }
            }
            selectedTab != ExplorerTab.FOLDERS -> {
                selectedTab = ExplorerTab.FOLDERS
            }
        }
    }

    LaunchedEffect(hasReadPermission, hasAllFilesAccess, refreshSignal, localRefresh) {
        if (!hasReadPermission) {
            allVideos = emptyList()
            return@LaunchedEffect
        }
        isScanning = true
        val fast = withContext(Dispatchers.IO) {
            VideoScanner.scanVideosFast(context)
        }
        allVideos = fast
        val merged = withContext(Dispatchers.IO) {
            VideoScanner.scanStorageExtras(context, fast)
        }
        allVideos = merged
        isScanning = false
    }

    LaunchedEffect(openedVideoFolderPath) {
        val folderPath = openedVideoFolderPath
        folderSubtitleBases = if (folderPath.isNullOrBlank()) {
            emptySet()
        } else {
            withContext(Dispatchers.IO) {
                VideoScanner.scanSubtitleBaseNamesInFolder(folderPath)
            }
        }
    }

    val videoIndex = remember(allVideos) {
        allVideos.filter { it.path.isNotBlank() }.associateBy { it.path }
    }

    LaunchedEffect(currentDirPath, settings.sortOrder, videoIndex, localRefresh, refreshSignal, hasReadPermission) {
        if (!hasReadPermission) {
            directoryEntries = emptyList()
            isLoadingDirectory = false
            return@LaunchedEffect
        }
        isLoadingDirectory = true
        directoryEntries = withContext(Dispatchers.IO) {
            FileExplorerManager.listDirectory(
                dir = File(currentDirPath),
                sortOrder = settings.sortOrder,
                videoIndex = videoIndex,
            )
        }
        isLoadingDirectory = false
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
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "이동")
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
                            Text(
                                text = "꺼벙이플레이어 (kkubunge Player)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "정렬")
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                SortOrder.entries.forEachIndexed { idx, order ->
                                    if (idx > 0 && idx % 2 == 0) {
                                        HorizontalDivider()
                                    }
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
                        text = {
                            Text(
                                text = tab.label,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                    )
                }
            }

            if (isScanning || isLoadingDirectory) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                )
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
                                subtitleBaseNames = folderSubtitleBases,
                                favoriteVideoIds = favoriteVideoIds,
                                listState = videoListState,
                                onVideoClick = { video ->
                                    if (selectedPaths.isNotEmpty()) {
                                        toggleSelect(video.path)
                                    } else {
                                        onPlayVideo(video, activeVideoFolder.videos, null)
                                    }
                                },
                                onVideoLongClick = { video -> toggleSelect(video.path) },
                                onToggleFavorite = { v ->
                                    val added = App.bookmarks.toggleFavoriteVideo(v)
                                    Toast.makeText(
                                        context,
                                        if (added) "즐겨찾기에 추가됨" else "즐겨찾기에서 해제됨",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
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
                                            App.bookmarks.removeForDeletedPath(v.id)
                                            triggerRefresh()
                                        }
                                    }
                                },
                            )
                        } else {
                            FolderListContent(
                                folders = folders,
                                selectedPaths = selectedPaths,
                                favoriteFolderPaths = favoriteFolderPaths,
                                listState = folderListState,
                                onFolderClick = { folder ->
                                    if (selectedPaths.isNotEmpty()) {
                                        toggleSelect(folder.path)
                                    } else {
                                        openedVideoFolderPath = folder.path
                                    }
                                },
                                onFolderLongClick = { folder -> toggleSelect(folder.path) },
                                onToggleFavorite = { folder ->
                                    val added = App.bookmarks.toggleFavoriteFolder(folder.path, folder.name)
                                    Toast.makeText(
                                        context,
                                        if (added) "즐겨찾는 폴더에 추가됨" else "즐겨찾는 폴더에서 해제됨",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
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
                            isLoading = isLoadingDirectory,
                            selectedPaths = selectedPaths,
                            favoriteFolderPaths = favoriteFolderPaths,
                            favoriteVideoIds = favoriteVideoIds,
                            listState = dirListState,
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
                                    onPlayVideo(entry.videoItem, folderPlaylist, null)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "동영상 파일만 재생할 수 있습니다.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onEntryLongClick = { entry -> toggleSelect(entry.path) },
                            onToggleFavorite = { entry ->
                                if (entry.isDirectory) {
                                    val added = App.bookmarks.toggleFavoriteFolder(entry.path, entry.name)
                                    Toast.makeText(
                                        context,
                                        if (added) "즐겨찾는 폴더에 추가됨" else "즐겨찾는 폴더에서 해제됨",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (entry.isVideo && entry.videoItem != null) {
                                    val added = App.bookmarks.toggleFavoriteVideo(entry.videoItem)
                                    Toast.makeText(
                                        context,
                                        if (added) "즐겨찾기에 추가됨" else "즐겨찾기에서 해제됨",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onCopy = { entry -> startClipboard(ClipboardOp.COPY, listOf(entry.path)) },
                            onMove = { entry -> startClipboard(ClipboardOp.MOVE, listOf(entry.path)) },
                            onRename = { entry -> renameTargetPath = entry.path },
                            onDelete = { entry -> deleteTargets = listOf(entry.path) },
                        )
                    }

                    ExplorerTab.FAVORITES -> {
                        FavoritesContent(
                            favoriteFolders = favoriteFolders,
                            favoriteVideos = favoriteVideos,
                            videoBookmarks = videoBookmarks,
                            allVideos = allVideos,
                            onOpenFolder = { favFolder ->
                                if (folders.any { it.path == favFolder.path }) {
                                    openedVideoFolderPath = favFolder.path
                                    selectedTab = ExplorerTab.FOLDERS
                                } else if (File(favFolder.path).isDirectory) {
                                    currentDirPath = favFolder.path
                                    selectedTab = ExplorerTab.FILES
                                } else {
                                    Toast.makeText(context, "폴더를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRemoveFavoriteFolder = { App.bookmarks.removeFavoriteFolder(it.path) },
                            onPlayFavoriteVideo = { favVideo ->
                                val matched = allVideos.firstOrNull {
                                    it.id == favVideo.id || (favVideo.path.isNotBlank() && it.path == favVideo.path)
                                } ?: VideoItem(
                                    id = favVideo.id,
                                    title = favVideo.title,
                                    uri = favVideo.uri,
                                    path = favVideo.path,
                                    folderPath = favVideo.folderPath,
                                    folderName = favVideo.folderName,
                                    sizeBytes = favVideo.sizeBytes,
                                    durationMs = favVideo.durationMs,
                                    dateModifiedSec = favVideo.addedAtMs / 1000L,
                                )
                                val playlist = allVideos.filter { it.folderPath == matched.folderPath }
                                    .ifEmpty { listOf(matched) }
                                onPlayVideo(matched, playlist, null)
                            },
                            onRemoveFavoriteVideo = { App.bookmarks.removeFavoriteVideo(it.id) },
                            onPlayBookmark = { bm ->
                                val matched = allVideos.firstOrNull {
                                    it.id == bm.videoId || (bm.path.isNotBlank() && it.path == bm.path)
                                } ?: VideoItem(
                                    id = bm.videoId,
                                    title = bm.videoTitle,
                                    uri = bm.uri,
                                    path = bm.path,
                                    folderPath = bm.folderPath,
                                    folderName = File(bm.folderPath).name.ifBlank { "북마크" },
                                    sizeBytes = 0L,
                                    durationMs = 0L,
                                    dateModifiedSec = bm.createdAtMs / 1000L,
                                )
                                val playlist = allVideos.filter { it.folderPath == matched.folderPath }
                                    .ifEmpty { listOf(matched) }
                                onPlayVideo(matched, playlist, bm.positionMs)
                            },
                            onRemoveBookmark = { App.bookmarks.removeVideoBookmark(it.bookmarkId) },
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
                                onPlayVideo(matched, playlist, null)
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
                                targets.forEach {
                                    App.history.removeByPathOrId(it)
                                    App.bookmarks.removeForDeletedPath(it)
                                }
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
    favoriteFolderPaths: Set<String>,
    listState: LazyListState,
    onFolderClick: (VideoFolder) -> Unit,
    onFolderLongClick: (VideoFolder) -> Unit,
    onToggleFavorite: (VideoFolder) -> Unit,
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
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(folders, key = { it.path }) { folder ->
            val isSelected = selectedPaths.contains(folder.path)
            val isFav = favoriteFolderPaths.contains(folder.path)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (isFav) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "즐겨찾기됨",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
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
                                text = { Text(if (isFav) "즐겨찾기 해제" else "즐겨찾기 추가") },
                                leadingIcon = {
                                    Icon(
                                        if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                        null,
                                        tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = { menuExpanded = false; onToggleFavorite(folder) },
                            )
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
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
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
    subtitleBaseNames: Set<String>,
    favoriteVideoIds: Set<String>,
    listState: LazyListState,
    onVideoClick: (VideoItem) -> Unit,
    onVideoLongClick: (VideoItem) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
    onCopy: (VideoItem) -> Unit,
    onMove: (VideoItem) -> Unit,
    onRename: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit,
) {
    val historyMap = remember(historyItems) { historyItems.associateBy { it.id } }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(videos, key = { it.id }) { video ->
            val isSelected = video.path.isNotBlank() && selectedPaths.contains(video.path)
            val isFav = favoriteVideoIds.contains(video.id) ||
                (video.path.isNotBlank() && favoriteVideoIds.contains(video.path))
            val hist = historyMap[video.id] ?: historyMap[video.path]
            val hasSubtitle = remember(video.path, subtitleBaseNames) {
                if (video.path.isBlank()) {
                    false
                } else {
                    val base = File(video.path).nameWithoutExtension.lowercase()
                    subtitleBaseNames.contains(base)
                }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (isFav) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "즐겨찾기됨",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
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
                            DropdownMenuItem(
                                text = { Text(if (isFav) "즐겨찾기 해제" else "즐겨찾기 추가") },
                                leadingIcon = {
                                    Icon(
                                        if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                        null,
                                        tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = { menuExpanded = false; onToggleFavorite(video) },
                            )
                            if (video.path.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("복사") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                    onClick = { menuExpanded = false; onCopy(video) },
                                )
                                DropdownMenuItem(
                                    text = { Text("이동") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
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
    isLoading: Boolean,
    selectedPaths: Set<String>,
    favoriteFolderPaths: Set<String>,
    favoriteVideoIds: Set<String>,
    listState: LazyListState,
    onNavigateDir: (String) -> Unit,
    onEntryClick: (ExplorerEntry) -> Unit,
    onEntryLongClick: (ExplorerEntry) -> Unit,
    onToggleFavorite: (ExplorerEntry) -> Unit,
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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(10.dp))
                    Text("폴더 내용을 불러오는 중...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return
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
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(entries, key = { it.path }) { entry ->
                val isSelected = selectedPaths.contains(entry.path)
                val isFav = (entry.isDirectory && favoriteFolderPaths.contains(entry.path)) ||
                    (entry.isVideo && favoriteVideoIds.contains(entry.path))
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
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isFav) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "즐겨찾기됨",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                            val subInfo = if (entry.isDirectory) {
                                if (entry.childCount >= 0) "항목 ${entry.childCount}개" else "폴더"
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
                                    DropdownMenuItem(
                                        text = { Text(if (isFav) "즐겨찾기 해제" else "즐겨찾기 추가") },
                                        leadingIcon = {
                                            Icon(
                                                if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                                null,
                                                tint = if (isFav) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = { menuExpanded = false; onToggleFavorite(entry) },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("복사") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                    onClick = { menuExpanded = false; onCopy(entry) },
                                )
                                DropdownMenuItem(
                                    text = { Text("이동") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
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
private fun FavoritesContent(
    favoriteFolders: List<FavoriteFolderItem>,
    favoriteVideos: List<FavoriteVideoItem>,
    videoBookmarks: List<VideoBookmarkItem>,
    allVideos: List<VideoItem>,
    onOpenFolder: (FavoriteFolderItem) -> Unit,
    onRemoveFavoriteFolder: (FavoriteFolderItem) -> Unit,
    onPlayFavoriteVideo: (FavoriteVideoItem) -> Unit,
    onRemoveFavoriteVideo: (FavoriteVideoItem) -> Unit,
    onPlayBookmark: (VideoBookmarkItem) -> Unit,
    onRemoveBookmark: (VideoBookmarkItem) -> Unit,
) {
    if (favoriteFolders.isEmpty() && favoriteVideos.isEmpty() && videoBookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "즐겨찾기한 폴더·동영상이나 장면 북마크가 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "목록의 ⋮ 메뉴 또는 재생 화면 상단 북마크 버튼으로 추가해 보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (favoriteFolders.isNotEmpty()) {
            item {
                Text(
                    text = "즐겨찾는 폴더 (${favoriteFolders.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(favoriteFolders, key = { "fav_folder_${it.path}" }) { folder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFolder(folder) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = folder.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onRemoveFavoriteFolder(folder) }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "즐겨찾기 해제",
                                tint = Color(0xFFFFB300),
                            )
                        }
                    }
                }
            }
        }

        if (favoriteVideos.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "즐겨찾는 동영상 (${favoriteVideos.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(favoriteVideos, key = { "fav_video_${it.id}" }) { fav ->
                val matchedVideo = allVideos.firstOrNull {
                    it.id == fav.id || (fav.path.isNotBlank() && it.path == fav.path)
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayFavoriteVideo(fav) },
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
                            uri = matchedVideo?.uri ?: fav.uri,
                            path = fav.path,
                            modifier = Modifier
                                .size(width = 96.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = fav.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val meta = buildString {
                                if (fav.folderName.isNotBlank()) append(fav.folderName)
                                if (fav.durationMs > 0L) {
                                    if (isNotEmpty()) append(" · ")
                                    append(Format.duration(fav.durationMs))
                                }
                            }
                            if (meta.isNotBlank()) {
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        IconButton(onClick = { onRemoveFavoriteVideo(fav) }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "즐겨찾기 해제",
                                tint = Color(0xFFFFB300),
                            )
                        }
                    }
                }
            }
        }

        if (videoBookmarks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "장면 북마크 (${videoBookmarks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(videoBookmarks, key = { "bm_${it.bookmarkId}" }) { bm ->
                val matchedVideo = allVideos.firstOrNull {
                    it.id == bm.videoId || (bm.path.isNotBlank() && it.path == bm.path)
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayBookmark(bm) },
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
                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            VideoThumbnail(
                                uri = matchedVideo?.uri ?: bm.uri,
                                path = bm.path,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Text(
                                text = Format.duration(bm.positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = bm.videoTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = if (bm.note.isNotBlank()) {
                                        "${Format.duration(bm.positionMs)} · ${bm.note}"
                                    } else {
                                        "${Format.duration(bm.positionMs)} 지점"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        IconButton(onClick = { onRemoveBookmark(bm) }) {
                            Icon(Icons.Default.Close, contentDescription = "북마크 삭제")
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
