package com.pranav.flipbook.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.utils.formatDuration
import com.pranav.flipbook.viewmodel.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    onGoalsClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    viewModel: StatisticsViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (stats.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overview cards
                item {
                    Text("Overview", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Books", "${stats.totalBooksOpened}", Icons.Outlined.MenuBook, Modifier.weight(1f))
                        StatCard("Completed", "${stats.booksCompleted}", Icons.Outlined.CheckCircle, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Pages Read", "${stats.totalPagesRead}", Icons.Outlined.Description, Modifier.weight(1f))
                        StatCard("Reading Time", stats.totalReadingTime.formatDuration(), Icons.Outlined.Schedule, Modifier.weight(1f))
                    }
                }

                // Today
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Text("Today", style = MaterialTheme.typography.titleMedium) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Pages Today", "${stats.pagesReadToday}", Icons.Outlined.TrendingUp, Modifier.weight(1f))
                        StatCard("Time Today", stats.readingTimeToday.formatDuration(), Icons.Outlined.Timer, Modifier.weight(1f))
                    }
                }

                // This Period
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("This Week", "${stats.pagesReadThisWeek} pages", Icons.Outlined.DateRange, Modifier.weight(1f))
                        StatCard("This Month", "${stats.pagesReadThisMonth} pages", Icons.Outlined.CalendarMonth, Modifier.weight(1f))
                    }
                }

                // Streaks & Activity
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Text("Streaks & Activity", style = MaterialTheme.typography.titleMedium) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Current Streak", "${stats.currentStreak} days", Icons.Outlined.LocalFireDepartment, Modifier.weight(1f))
                        StatCard("Best Streak", "${stats.longestStreak} days", Icons.Outlined.EmojiEvents, Modifier.weight(1f))
                    }
                }
                item {
                    ReadingCalendarView(readingDays = emptyList())
                }

                // Quick actions
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Card(
                        onClick = onGoalsClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Reading Goals", modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
                item {
                    Card(
                        onClick = onAchievementsClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Achievements", modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(onBack: () -> Unit, viewModel: StatisticsViewModel = viewModel()) {
    val goals by viewModel.activeGoals.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Goals") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Add Goal")
            }
        }
    ) { padding ->
        if (goals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No reading goals yet", style = MaterialTheme.typography.titleMedium)
                    Text("Set a goal to stay motivated", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(goals.size) { index ->
                    val goal = goals[index]
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = when (goal.type) {
                                    "DAILY_PAGES" -> "Daily Pages Goal"
                                    "DAILY_TIME" -> "Daily Reading Time"
                                    "WEEKLY_PAGES" -> "Weekly Pages Goal"
                                    "MONTHLY_BOOKS" -> "Monthly Books Goal"
                                    else -> goal.type
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text("Target: ${goal.target}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            var goalType by remember { mutableStateOf("DAILY_PAGES") }
            var targetText by remember { mutableStateOf("30") }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Goal") },
                text = {
                    Column {
                        listOf("DAILY_PAGES" to "Daily Pages", "DAILY_TIME" to "Daily Minutes", "WEEKLY_PAGES" to "Weekly Pages").forEach { (type, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = goalType == type, onClick = { goalType = type })
                                Text(label)
                            }
                        }
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                            label = { Text("Target") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        targetText.toIntOrNull()?.let { target ->
                            viewModel.createGoal(goalType, target)
                        }
                        showCreateDialog = false
                    }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit, viewModel: StatisticsViewModel = viewModel()) {
    val achievements by viewModel.achievements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (achievements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(achievements.size) { index ->
                    val achievement = achievements[index]
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (achievement.isUnlocked)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (achievement.isUnlocked) Icons.Filled.EmojiEvents else Icons.Outlined.Lock,
                                null,
                                tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(achievement.title, style = MaterialTheme.typography.titleSmall)
                                Text(achievement.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!achievement.isUnlocked && achievement.target > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (achievement.progress.toFloat() / achievement.target).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp)
                                    )
                                    Text("${achievement.progress}/${achievement.target}",
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
