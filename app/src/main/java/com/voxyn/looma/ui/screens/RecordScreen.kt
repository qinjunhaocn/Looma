package com.voxyn.looma.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxyn.looma.RecordPrefs
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordScreen(
    viewModel: RecordViewModel = viewModel()
) {
    val recordState by viewModel.recordState.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val hasRoot by viewModel.hasRoot.collectAsStateWithLifecycle()
    val detectedRes by viewModel.detectedResolution.collectAsStateWithLifecycle()

    var showResSheet by remember { mutableStateOf(false) }
    var showFpsSheet by remember { mutableStateOf(false) }
    var showBitrateSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Looma",
                subtitle = if (recordState.isRecording) "Recording…" else "Screen Recorder",
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshRecordings()
                        }
                    ) {
                        Image(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "Refresh",
                            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                        )
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // ─── Root Status ───────────────────
            item {
                RootStatusCard(hasRoot = hasRoot)
            }

            // ─── Record Button ─────────────────
            item {
                RecordButtonCard(
                    isRecording = recordState.isRecording,
                    onClick = { viewModel.toggleRecording() },
                    enabled = hasRoot == true,
                )
            }

            // ─── Settings ──────────────────────
            item {
                SettingsCard(
                    prefs = prefs,
                    detectedRes = detectedRes,
                    onResClick = { showResSheet = true },
                    onFpsClick = { showFpsSheet = true },
                    onBitrateClick = { showBitrateSheet = true },
                )
            }

            // ─── Section Header ────────────────
            item {
                Spacer(Modifier.height(4.dp))
                SmallTitle(text = "Recent Recordings")
            }

            // ─── Recording List ────────────────
            if (recordState.lastFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No recordings yet",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                items(recordState.lastFiles.take(20), key = { it.absolutePath }) { file ->
                    RecordingItem(file = file)
                }
            }
        }
    }

    // ─── Bottom Sheets ─────────────────────
    if (showResSheet) {
        SettingsOptionSheet(
            title = "Resolution",
            options = listOf(
                "" to "Auto — $detectedRes",
                "720x1280" to "720p  (720×1280)",
                "1080x1920" to "1080p  (1080×1920)",
                "1440x2560" to "1440p  (1440×2560)",
                "2160x3840" to "4K  (2160×3840)",
            ),
            selectedKey = prefs.resolution,
            onSelect = { viewModel.updateResolution(it); showResSheet = false },
            onDismiss = { showResSheet = false },
        )
    }
    if (showFpsSheet) {
        SettingsOptionSheet(
            title = "Frame Rate",
            options = listOf(15, 24, 30, 60, 90, 120).map { "$it" to "${it} fps" },
            selectedKey = prefs.fps.toString(),
            onSelect = { viewModel.updateFps(it.toIntOrNull() ?: 30); showFpsSheet = false },
            onDismiss = { showFpsSheet = false },
        )
    }
    if (showBitrateSheet) {
        SettingsOptionSheet(
            title = "Bitrate",
            options = listOf(4, 6, 8, 10, 12, 16, 20, 24).map { "$it" to "${it} Mbps" },
            selectedKey = prefs.bitRateMbps.toString(),
            onSelect = { viewModel.updateBitRate(it.toIntOrNull() ?: 8); showBitrateSheet = false },
            onDismiss = { showBitrateSheet = false },
        )
    }
}

// ═══════════════════════════════════════════════
//  Root Status Card
// ═══════════════════════════════════════════════

@Composable
private fun RootStatusCard(hasRoot: Boolean?) {
    val bg = when (hasRoot) {
        true -> MiuixTheme.colorScheme.tertiaryContainer
        false -> MiuixTheme.colorScheme.errorContainer
        null -> MiuixTheme.colorScheme.surfaceContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon: ImageVector
            val tint: Color
            val text: String
            when (hasRoot) {
                true -> {
                    icon = MiuixIcons.Basic.Check
                    tint = MiuixTheme.colorScheme.primary
                    text = "Root granted"
                }
                false -> {
                    icon = MiuixIcons.Basic.Close
                    tint = MiuixTheme.colorScheme.error
                    text = "Root access denied"
                }
                null -> {
                    icon = MiuixIcons.Basic.Close
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                    text = "Checking root…"
                }
            }
            Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(tint),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

// ═══════════════════════════════════════════════
//  Record Button Card
// ═══════════════════════════════════════════════

@Composable
private fun RecordButtonCard(
    isRecording: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    val containerColor by animateColorAsState(
        targetValue = if (isRecording) MiuixTheme.colorScheme.error
        else MiuixTheme.colorScheme.primary,
        label = "btnBg",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = containerColor),
        onClick = if (enabled) onClick else null,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Record icon circle
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Pulse ring when recording
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .padding(0.dp)
                    )
                }
                Image(
                    imageVector = if (isRecording) MiuixIcons.Basic.Close
                    else MiuixIcons.Recording,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    colorFilter = ColorFilter.tint(
                        if (isRecording) Color(0xFFFF4444) else Color.White
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isRecording) "Tap to Stop" else "Start Recording",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )

            if (isRecording) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Recording in progress…",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  Settings Card (3-row preference list)
// ═══════════════════════════════════════════════

@Composable
private fun SettingsCard(
    prefs: RecordPrefs,
    detectedRes: String,
    onResClick: () -> Unit,
    onFpsClick: () -> Unit,
    onBitrateClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ArrowPreference(
                title = "Resolution",
                summary = prefs.resolution.ifEmpty { "Auto ($detectedRes)" },
                startAction = {
                    Image(
                        imageVector = MiuixIcons.ScreenCapture,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                    )
                },
                onClick = onResClick,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ArrowPreference(
                title = "Frame Rate",
                summary = "${prefs.fps} fps",
                startAction = {
                    Image(
                        imageVector = MiuixIcons.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                    )
                },
                onClick = onFpsClick,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ArrowPreference(
                title = "Bitrate",
                summary = "${prefs.bitRateMbps} Mbps",
                startAction = {
                    Image(
                        imageVector = MiuixIcons.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                    )
                },
                onClick = onBitrateClick,
            )
        }
    }
}

// ═══════════════════════════════════════════════
//  Settings Option Bottom Sheet
// ═══════════════════════════════════════════════

@Composable
private fun SettingsOptionSheet(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            options.forEach { (key, label) ->
                val selected = key == selectedKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { onSelect(key) },
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.body1,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  Recording Item Row
// ═══════════════════════════════════════════════

@Composable
private fun RecordingItem(file: File) {
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                imageVector = MiuixIcons.ScreenCapture,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurfaceContainerVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dateFormat.format(Date(file.lastModified())),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
    }
}
