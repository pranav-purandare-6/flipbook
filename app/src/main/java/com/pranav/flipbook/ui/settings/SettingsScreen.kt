package com.pranav.flipbook.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.data.backup.BackupManager
import com.pranav.flipbook.utils.CacheManager
import com.pranav.flipbook.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }

    val transitionStyle by viewModel.transitionStyle.collectAsState()
    val animationSpeed by viewModel.animationSpeed.collectAsState()
    val pageSoundEnabled by viewModel.pageSoundEnabled.collectAsState()
    val pageSoundVolume by viewModel.pageSoundVolume.collectAsState()
    val ambientSound by viewModel.ambientSound.collectAsState()
    val ambientVolume by viewModel.ambientVolume.collectAsState()
    val readerTheme by viewModel.readerTheme.collectAsState()
    val readerBrightness by viewModel.readerBrightness.collectAsState()
    val marginSize by viewModel.marginSize.collectAsState()
    val autoHideControls by viewModel.autoHideControls.collectAsState()

    var cacheMessage by remember { mutableStateOf<String?>(null) }
    var backupMessage by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val ok = backupManager.createBackup(it)
                backupMessage = if (ok) "Backup saved successfully" else "Backup failed"
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val ok = backupManager.restoreBackup(it)
                backupMessage = if (ok) "Restore completed" else "Restore failed"
            }
        }
    }

    LaunchedEffect(cacheMessage, backupMessage) {
        cacheMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); cacheMessage = null }
        backupMessage?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); backupMessage = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { SettingsSectionHeader("Reader") }

            item {
                SettingsDropdown(
                    title = "Reading Mode",
                    subtitle = readerTheme,
                    options = listOf("light", "dark", "sepia", "warm", "bw"),
                    labels = listOf("Light", "Dark", "Sepia", "Warm", "Black & White"),
                    selected = readerTheme,
                    onSelect = { viewModel.setReaderTheme(it) }
                )
            }

            item {
                SettingsSlider(
                    title = "Brightness",
                    value = readerBrightness,
                    range = 0.3f..1f,
                    steps = 6,
                    label = "${(readerBrightness * 100).toInt()}%",
                    onValueChange = { viewModel.setReaderBrightness(it) }
                )
            }

            item {
                SettingsDropdown(
                    title = "Page Transition",
                    subtitle = transitionStyle,
                    options = listOf("CURL", "SLIDE", "FADE", "NONE"),
                    labels = listOf("Realistic Page Curl", "Slide", "Fade", "None"),
                    selected = transitionStyle,
                    onSelect = { viewModel.setTransitionStyle(it) }
                )
            }

            item {
                SettingsSlider(
                    title = "Animation Speed",
                    value = animationSpeed.toFloat(),
                    range = 150f..1200f,
                    steps = 4,
                    label = when {
                        animationSpeed <= 250 -> "Very Fast"
                        animationSpeed <= 400 -> "Fast"
                        animationSpeed <= 550 -> "Normal"
                        animationSpeed <= 750 -> "Slow"
                        else -> "Very Slow"
                    },
                    onValueChange = { viewModel.setAnimationSpeed(it.toInt()) }
                )
            }

            item {
                SettingsDropdown(
                    title = "Page Margins",
                    subtitle = marginSize,
                    options = listOf("small", "medium", "large"),
                    labels = listOf("Small", "Medium", "Large"),
                    selected = marginSize,
                    onSelect = { viewModel.setMarginSize(it) }
                )
            }

            item {
                SettingsSwitch(
                    title = "Auto-hide Controls",
                    subtitle = "Hide reader controls after inactivity",
                    checked = autoHideControls,
                    onCheckedChange = { viewModel.setAutoHideControls(it) }
                )
            }

            item { SettingsSectionHeader("Audio") }

            item {
                SettingsSwitch(
                    title = "Page Turn Sound",
                    subtitle = "Play a subtle rustle when turning pages",
                    checked = pageSoundEnabled,
                    onCheckedChange = { viewModel.setPageSound(it) }
                )
            }

            item {
                SettingsSlider(
                    title = "Page Turn Volume",
                    value = pageSoundVolume,
                    range = 0f..1f,
                    steps = 9,
                    label = "${(pageSoundVolume * 100).toInt()}%",
                    onValueChange = { viewModel.setPageSoundVolume(it) }
                )
            }

            item {
                SettingsDropdown(
                    title = "Ambient Sound",
                    subtitle = ambientSound,
                    options = listOf("none", "rain", "fireplace", "cafe", "forest", "whitenoise"),
                    labels = listOf("None", "Rain", "Fireplace", "Cafe", "Forest", "White Noise"),
                    selected = ambientSound,
                    onSelect = { viewModel.setAmbientSound(it) }
                )
            }

            item {
                SettingsSlider(
                    title = "Ambient Volume",
                    value = ambientVolume,
                    range = 0f..1f,
                    steps = 9,
                    label = "${(ambientVolume * 100).toInt()}%",
                    onValueChange = { viewModel.setAmbientVolume(it) }
                )
            }

            item { SettingsSectionHeader("Storage") }

            item {
                SettingsItem(
                    title = "Clear Thumbnail Cache",
                    subtitle = "Covers: ${CacheManager.formatSize(CacheManager.getThumbnailCacheSize(context))}",
                    onClick = {
                        CacheManager.clearThumbnailCache(context)
                        cacheMessage = "Thumbnail cache cleared"
                    }
                )
            }

            item {
                SettingsItem(
                    title = "Clear Temporary Files",
                    subtitle = "Temp: ${CacheManager.formatSize(CacheManager.getTempCacheSize(context))}",
                    onClick = {
                        CacheManager.clearTempCache(context)
                        cacheMessage = "Temporary files cleared"
                    }
                )
            }

            item { SettingsSectionHeader("Data") }

            item {
                SettingsItem(
                    title = "Backup Data",
                    subtitle = "Export progress, bookmarks, notes, and settings",
                    onClick = {
                        backupLauncher.launch("flipbook_backup_${System.currentTimeMillis()}.json")
                    }
                )
            }

            item {
                SettingsItem(
                    title = "Restore Data",
                    subtitle = "Import a previously exported backup file",
                    onClick = { restoreLauncher.launch(arrayOf("application/json")) }
                )
            }

            item { SettingsSectionHeader("About") }

            item {
                SettingsInfoItem(
                    title = "Flip Book",
                    subtitle = "Version 1.0 - All data stays on your device"
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDropdown(
    title: String,
    subtitle: String,
    options: List<String>,
    labels: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = labels.getOrElse(options.indexOf(selected)) { subtitle }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(selectedLabel, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(labels[index]) },
                    onClick = { onSelect(option); expanded = false },
                    leadingIcon = {
                        if (option == selected) Icon(Icons.Default.Check, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    label: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsInfoItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
