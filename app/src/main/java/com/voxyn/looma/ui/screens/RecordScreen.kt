package com.voxyn.looma.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxyn.looma.RecordPrefs
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = {
                    Text("Looma", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRecordings() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                Text(
                    "Recent Recordings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // ─── Recording List ────────────────
            if (recordState.lastFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.VideocamOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No recordings yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
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
        ResolutionSheet(
            current = prefs.resolution,
            detected = detectedRes,
            onSelect = { viewModel.updateResolution(it); showResSheet = false },
            onDismiss = { showResSheet = false },
        )
    }
    if (showFpsSheet) {
        ValuePickerSheet(
            title = "Frame Rate",
            current = prefs.fps,
            values = listOf(15, 24, 30, 60, 90, 120),
            suffix = " fps",
            onSelect = { viewModel.updateFps(it); showFpsSheet = false },
            onDismiss = { showFpsSheet = false },
        )
    }
    if (showBitrateSheet) {
        ValuePickerSheet(
            title = "Bitrate",
            current = prefs.bitRateMbps,
            values = listOf(4, 6, 8, 10, 12, 16, 20, 24),
            suffix = " Mbps",
            onSelect = { viewModel.updateBitRate(it); showBitrateSheet = false },
            onDismiss = { showBitrateSheet = false },
        )
    }
}

// ═══════════════════════════════════════════════
//  Root Status
// ═══════════════════════════════════════════════

@Composable
private fun RootStatusCard(hasRoot: Boolean?) {
    val containerColor = when (hasRoot) {
        true -> MaterialTheme.colorScheme.primaryContainer
        false -> MaterialTheme.colorScheme.errorContainer
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (hasRoot) {
        true -> MaterialTheme.colorScheme.onPrimaryContainer
        false -> MaterialTheme.colorScheme.onErrorContainer
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (hasRoot) {
                true -> Icons.Filled.CheckCircle
                false -> Icons.Filled.Cancel
                null -> Icons.Filled.HourglassEmpty
            }
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Text(
                when (hasRoot) {
                    true -> "Root granted"
                    false -> "Root access denied"
                    null -> "Checking root…"
                },
                color = contentColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ═══════════════════════════════════════════════
//  Record Button
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
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    val containerColor by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary,
        label = "btnBg",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick,
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) Color.White.copy(alpha = pulseAlpha)
                        else Color.White.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (isRecording) MaterialTheme.colorScheme.error else Color.White,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                if (isRecording) "Tap to Stop" else "Start Recording",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            if (isRecording) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recording in progress…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  Settings Card
// ═══════════════════════════════════════════════

@Composable
private fun SettingsCard(
    prefs: RecordPrefs,
    detectedRes: String,
    onResClick: () -> Unit,
    onFpsClick: () -> Unit,
    onBitrateClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingsRow(
                icon = Icons.Outlined.AspectRatio,
                label = "Resolution",
                value = prefs.resolution.ifEmpty { "Auto ($detectedRes)" },
                onClick = onResClick,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                icon = Icons.Outlined.Speed,
                label = "Frame Rate",
                value = "${prefs.fps} fps",
                onClick = onFpsClick,
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                icon = Icons.Outlined.HighQuality,
                label = "Bitrate",
                value = "${prefs.bitRateMbps} Mbps",
                onClick = onBitrateClick,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.primary)
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

// ═══════════════════════════════════════════════
//  Bottom Sheets
// ═══════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionSheet(
    current: String,
    detected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        "" to "Auto — $detected",
        "720x1280" to "720p (720×1280)",
        "1080x1920" to "1080p (1080×1920)",
        "1440x2560" to "1440p (1440×2560)",
        "2160x3840" to "4K (2160×3840)",
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Resolution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            options.forEach { (key, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(key) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = current == key,
                        onClick = { onSelect(key) },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValuePickerSheet(
    title: String,
    current: Int,
    values: List<Int>,
    suffix: String,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            values.forEach { value ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(value) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = current == value,
                        onClick = { onSelect(value) },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("$value$suffix", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  Recording Item
// ═══════════════════════════════════════════════

@Composable
private fun RecordingItem(file: File) {
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dateFormat.format(Date(file.lastModified())),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
