package com.pranav.flipbook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val transitionStyle by viewModel.transitionStyle.collectAsState()
    val animationSpeed by viewModel.animationSpeed.collectAsState()
    val pageSoundEnabled by viewModel.pageSoundEnabled.collectAsState()
    val ambientSound by viewModel.ambientSound.collectAsState()
    val readerTheme by viewModel.readerTheme.collectAsState()
    val marginSize by viewModel.marginSize.collectAsState()
    val autoHideControls by viewModel.autoHideControls.collectAsState()
    val showBookOpening by viewModel.showBookOpening.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Reader Section
            item { SettingsSectionHeader("Reader") }

            item {
                SettingsDropdown(
                    title = "Page Transition",
                    subtitle = transitionStyle,
                    options = listOf("CURL", "SLIDE", "FADE", "NONE"),
                    labels = listOf("Realistic Page Curl", "Simple Slide", "Fade", "None"),
                    selected = transitionStyle,
                    onSelect = { viewModel.setTransitionStyle(it) }
                )
            }

            item {
                SettingsSlider(
                    title = "Animation Speed",
                    value = animationSpeed.toFloat(),
                    range = 100f..2000f,
                    steps = 4,
                    label = when {
                        animationSpeed <= 200 -> "Very Fast"
                        animationSpeed <= 350 -> "Fast"
                        animationSpeed <= 500 -> "Normal"
                        animationSpeed <= 800 -> "Slow"
                        else -> "Very Slow"
                    },
                    onValueChange = { viewModel.setAnimationSpeed(it.toInt()) }
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

            // Appearance
            item { SettingsSectionHeader("Appearance") }

            item {
                SettingsDropdown(
                    title = "Reader Theme",
                    subtitle = readerTheme,
                    options = listOf("light", "dark", "sepia", "warm", "bw"),
                    labels = listOf("Light", "Dark", "Sepia", "Warm", "Black & White"),
                    selected = readerTheme,
                    onSelect = { viewModel.setReaderTheme(it) }
                )
            }

            item {
                SettingsSwitch(
                    title = "Book Opening Animation",
                    subtitle = "Show animation when opening a book",
                    checked = showBookOpening,
                    onCheckedChange = { viewModel.setShowBookOpening(it) }
                )
            }

            // Audio
            item { SettingsSectionHeader("Audio") }

            item {
                SettingsSwitch(
                    title = "Page Turn Sound",
                    subtitle = "Play subtle sound on page turn",
                    checked = pageSoundEnabled,
                    onCheckedChange = { viewModel.setPageSound(it) }
                )
            }

            item {
                SettingsDropdown(
                    title = "Ambient Sound",
                    subtitle = ambientSound,
                    options = listOf("none", "rain", "fireplace", "cafe", "forest", "whitenoise"),
                    labels = listOf("None", "Rain", "Fireplace", "Café", "Forest", "White Noise"),
                    selected = ambientSound,
                    onSelect = { viewModel.setAmbientSound(it) }
                )
            }

            // Storage
            item { SettingsSectionHeader("Storage") }

            item {
                SettingsItem(
                    title = "Clear Page Cache",
                    subtitle = "Free up space used by rendered pages",
                    onClick = { /* TODO clear cache */ }
                )
            }

            item {
                SettingsItem(
                    title = "Clear Cover Cache",
                    subtitle = "Covers will be regenerated when needed",
                    onClick = { /* TODO clear covers */ }
                )
            }

            // About
            item { SettingsSectionHeader("About") }

            item {
                SettingsItem(
                    title = "Flip Book",
                    subtitle = "Version 1.0 · All data stays on your device",
                    onClick = { }
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
