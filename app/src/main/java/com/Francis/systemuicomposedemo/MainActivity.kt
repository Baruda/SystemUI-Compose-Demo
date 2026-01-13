package com.Francis.systemuicomposedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.Francis.systemuicomposedemo.ui.theme.SystemUIComposeDemoTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// --- Data class for a Quick Setting Tile State ---
data class QuickSettingTileState(
    val name: String,
    val icon: ImageVector,
    var isEnabled: Boolean = false
)

// --- Data class for a Notification ---
data class Notification(
    val id: Int,
    val appName: String,
    val title: String,
    val text: String,
)

// --- Data class for a Recent Task ---
data class Task(
    val id: Int,
    val name: String,
    val color: Color,
)

// --- ViewModel for the Notification Shade ---
class ShadeViewModel {
    private val _quickSettings = MutableStateFlow(
        listOf(
            QuickSettingTileState("Wi-Fi", Icons.Default.Wifi, isEnabled = true),
            QuickSettingTileState("Bluetooth", Icons.Default.Bluetooth, isEnabled = false),
            QuickSettingTileState("Flashlight", Icons.Default.FlashlightOn),
            QuickSettingTileState("Auto-rotate", Icons.Default.AutoAwesome),
            QuickSettingTileState("Do Not Disturb", Icons.Default.NotificationsOff),
            QuickSettingTileState("Airplane Mode", Icons.Default.AirplanemodeActive),
        )
    )
    val quickSettings = _quickSettings.asStateFlow()

    private val _notifications = MutableStateFlow(
        listOf(
            Notification(1, "Gmail", "New Message", "Check out the new features..."),
            Notification(2, "Slack", "Direct Message", "John Doe sent you a message"),
            Notification(3, "Android Studio", "Build Successful", "app:assembleDebug finished."),
        )
    )
    val notifications = _notifications.asStateFlow()

    fun toggleQuickSetting(settingName: String) {
        _quickSettings.value = _quickSettings.value.map {
            if (it.name == settingName) {
                it.copy(isEnabled = !it.isEnabled)
            } else {
                it
            }
        }
    }

    fun dismissNotification(notificationId: Int) {
        _notifications.value = _notifications.value.filterNot { it.id == notificationId }
    }
}

// --- ViewModel for the Recents Screen ---
class RecentsViewModel {
    private val _tasks = MutableStateFlow(
        listOf(
            Task(1, "Chrome", Color(0xFFF44336)),
            Task(2, "Spotify", Color(0xFF4CAF50)),
            Task(3, "Settings", Color(0xFF2196F3)),
            Task(4, "Calculator", Color(0xFFFFC107)),
            Task(5, "Camera", Color(0xFF9C27B0)),
        )
    )
    val tasks = _tasks.asStateFlow()

    fun dismissTask(taskId: Int) {
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
    }
}

// --- ViewModel for the Volume Dialog ---
class VolumeViewModel {
    private val _volumeLevel = MutableStateFlow(0.5f)
    val volumeLevel = _volumeLevel.asStateFlow()

    fun setVolume(level: Float) {
        _volumeLevel.value = level
    }
}

// --- ViewModel for the Keyguard ---
class KeyguardViewModel {
    private val _isLocked = MutableStateFlow(true)
    val isLocked = _isLocked.asStateFlow()
    fun lock() { _isLocked.value = true }
    fun unlock() { _isLocked.value = false }
}

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SystemUIComposeDemoTheme {
                SystemUIComposeDemo()
            }
        }
    }
}

// --- ROOT COMPOSABLE: THE CORE OF THE SYSTEM UI ---
@Composable
fun SystemUIComposeDemo() {
    // Centralized state management.
    val keyguardVM = remember { KeyguardViewModel() }
    val shadeVM = remember { ShadeViewModel() }
    val recentsVM = remember { RecentsViewModel() }
    val volumeVM = remember { VolumeViewModel() }
    val isLocked by keyguardVM.isLocked.collectAsState()
    val quickSettings by shadeVM.quickSettings.collectAsState()
    val notifications by shadeVM.notifications.collectAsState()
    val tasks by recentsVM.tasks.collectAsState()
    val volumeLevel by volumeVM.volumeLevel.collectAsState()

    // Transient UI state for gestures and animations.
    var expansion by remember { mutableFloatStateOf(0f) }
    val animatedExpansion by animateFloatAsState(
        targetValue = if (isLocked) 0f else expansion, // Shade must be closed when locked.
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 200f),
        label = "ShadeExpansion"
    )

    // State for various dialogs and overlays.
    var showVolume by remember { mutableStateOf(false) }
    var showPower by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var splitScreenTask by remember { mutableStateOf<Task?>(null) }
    val showSplit = splitScreenTask != null
    var headsUp by remember { mutableStateOf<String?>(null) }

    // A side-effect to manage the lifecycle of a heads-up notification.
    LaunchedEffect(headsUp) {
        if (headsUp != null) {
            delay(3000)
            headsUp = null
        }
    }

    // A System UI is a stack of layers. `BoxWithConstraints` is ideal for this.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (!isLocked) {
                    // When unlocked, downward drag expands the notification shade.
                    detectVerticalDragGestures { _, dragAmount ->
                        val delta = dragAmount / size.height
                        expansion = (expansion - delta).coerceIn(0f, 1f)
                    }
                } else {
                    // When locked, a significant swipe up unlocks the device.
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -200) { // Threshold to prevent accidental unlocks.
                            keyguardVM.unlock()
                        }
                    }
                }
            }
    ) {
        SystemUiLayers(
            isLocked = isLocked,
            expansion = expansion,
            animatedExpansion = animatedExpansion,
            maxHeight = this.maxHeight,
            quickSettings = quickSettings,
            notifications = notifications,
            tasks = tasks,
            volumeLevel = volumeLevel,
            showVolume = showVolume,
            showPower = showPower,
            showRecents = showRecents,
            showSplit = showSplit,
            splitScreenTask = splitScreenTask,
            headsUp = headsUp,
            keyguardVM = keyguardVM,
            shadeVM = shadeVM,
            recentsVM = recentsVM,
            volumeVM = volumeVM,
            onShowVolumeChange = { showVolume = it },
            onShowPowerChange = { showPower = it },
            onShowRecentsChange = { showRecents = it },
            onSplitScreenTaskChange = { splitScreenTask = it },
            onHeadsUpChange = { headsUp = it }
        )
    }
}

@Composable
private fun SystemUiLayers(
    isLocked: Boolean,
    expansion: Float,
    animatedExpansion: Float,
    maxHeight: Dp,
    quickSettings: List<QuickSettingTileState>,
    notifications: List<Notification>,
    tasks: List<Task>,
    volumeLevel: Float,
    showVolume: Boolean,
    showPower: Boolean,
    showRecents: Boolean,
    showSplit: Boolean,
    splitScreenTask: Task?,
    headsUp: String?,
    keyguardVM: KeyguardViewModel,
    shadeVM: ShadeViewModel,
    recentsVM: RecentsViewModel,
    volumeVM: VolumeViewModel,
    onShowVolumeChange: (Boolean) -> Unit,
    onShowPowerChange: (Boolean) -> Unit,
    onShowRecentsChange: (Boolean) -> Unit,
    onSplitScreenTaskChange: (Task?) -> Unit,
    onHeadsUpChange: (String?) -> Unit
) {
    // --- UI LAYERS (drawn from back to front) ---

    // Layer 0: Home Screen Content (conditionally rendered)
    if (!isLocked) {
        HomeScreenContent(onTriggerHeadsUp = { onHeadsUpChange("New Message from Jane!") })
    }

    // Layer 1: Status Bar (always visible)
    StatusBar()

    // Layer 2: Notification Shade Panel (slides down from the top)
    ShadePanel(
        expansion = animatedExpansion,
        maxHeight = maxHeight,
        quickSettings = quickSettings,
        notifications = notifications,
        onToggleQuickSetting = { shadeVM.toggleQuickSetting(it) },
        onDismissNotification = { shadeVM.dismissNotification(it) }
    )

    // Layer 3: Heads-Up Notification (transient, appears over other content)
    headsUp?.let { HeadsUpNotification(it) }

    // Layer 4: System Dialogs & Overlays
    if (showVolume) VolumeDialog(volumeLevel, { volumeVM.setVolume(it) }) { onShowVolumeChange(false) }
    if (showPower) PowerMenu { onShowPowerChange(false) }
    if (showRecents) RecentsScreen(
        tasks = tasks,
        onDismissTask = { recentsVM.dismissTask(it) },
        onDismiss = { onShowRecentsChange(false) },
        onSplit = { task ->
            onSplitScreenTaskChange(task)
            onShowRecentsChange(false)
        }
    )
    if (showSplit) SplitScreenOverlay(splitScreenTask) { onSplitScreenTaskChange(null) }

    // Layer 5: Navigation Bar (always at the bottom)
    NavigationBar(
        onBack = { /* Handled by SystemUIComposeDemo */ },
        onHome = { keyguardVM.lock() }, // Demo: Home button locks the device.
        onRecents = { onShowRecentsChange(true) },
        onShowPower = { onShowPowerChange(true) },
        onShowVolume = { onShowVolumeChange(true) }
    )

    // Layer 6: Keyguard (Lock Screen) - drawn on top of everything.
    if (isLocked) {
        KeyguardScreen(
            onBiometric = { keyguardVM.unlock() } // Simulate successful biometric unlock.
        )
    }
}


// --- UI COMPONENTS ---

@Composable
fun HomeScreenContent(onTriggerHeadsUp: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Home Screen")
            Button(onClick = onTriggerHeadsUp) {
                Text("Trigger Heads-Up Notification")
            }
        }
    }
}

@Composable
fun StatusBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding() // Handles insets automatically.
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("12:00 PM", color = Color.White, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text("LTE", color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun KeyguardScreen(onBiometric: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.8f)) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Locked", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(80.dp))
                Button(onClick = onBiometric) { Text("Simulate Biometric Unlock") }
                Text("Swipe up to unlock", color = Color.White, modifier = Modifier.padding(top = 20.dp))
            }
        }
    }
}

@Composable
fun ShadePanel(
    expansion: Float,
    maxHeight: Dp,
    quickSettings: List<QuickSettingTileState>,
    notifications: List<Notification>,
    onToggleQuickSetting: (String) -> Unit,
    onDismissNotification: (Int) -> Unit
) {
    if (expansion > 0f) { // Performance optimization.
        Column(
            Modifier
                .fillMaxWidth()
                .height(maxHeight * expansion) // Height is dynamically controlled by the gesture.
                .background(Color.DarkGray.copy(alpha = 0.9f))
        ) {
            // Animate the content to fade/slide in
            AnimatedVisibility(
                visible = expansion > 0.5, // Only show when expanded enough
                enter = fadeIn(animationSpec = spring(stiffness = 300f)) + slideInVertically(animationSpec = spring(stiffness = 300f)),
                exit = fadeOut(animationSpec = spring(stiffness = 300f)) + slideOutVertically(animationSpec = spring(stiffness = 300f))
            ) {
                Column(modifier = Modifier.padding(top = 40.dp)) { // Padding for status bar
                    QuickSettingsGrid(quickSettings, onToggle = onToggleQuickSetting)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Notifications",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    NotificationList(notifications, onDismissNotification)
                }
            }
        }
    }
}

@Composable
fun QuickSettingsGrid(
    settings: List<QuickSettingTileState>,
    onToggle: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(settings) { setting ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { onToggle(setting.name) }
                    .padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = setting.icon,
                    contentDescription = setting.name,
                    tint = if (setting.isEnabled) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(setting.name, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun NotificationList(
    notifications: List<Notification>,
    onDismissNotification: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp) // Avoid overlap with nav bar
    ) {
        items(notifications, key = { it.id }) { notification ->
            NotificationCard(notification, onDismiss = { onDismissNotification(notification.id) })
        }
    }
}

@Composable
fun NotificationCard(notification: Notification, onDismiss: () -> Unit) {
    val offsetX = remember { Animatable(0f) }

    Box(
        Modifier
            .pointerInput(Unit) {
                coroutineScope {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            launch {
                                if (abs(offsetX.value) > 300) { // arbitrary threshold
                                    onDismiss()
                                } else {
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        }
                    )
                }
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color.Gray.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "App Icon", tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(notification.appName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(notification.title, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(notification.text, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RecentsScreen(
    tasks: List<Task>,
    onDismissTask: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSplit: (Task) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f))) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = 40.dp), // To have cards centered
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onDismiss = { onDismissTask(task.id) },
                    onSplit = { onSplit(task) },
                    modifier = Modifier
                        .fillParentMaxHeight(0.7f) // Make cards tall
                        .aspectRatio(9f / 16f) // Typical phone aspect ratio
                )
            }
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .navigationBarsPadding()
        ) {
            Text("Close")
        }
    }
}

@Composable
fun TaskCard(task: Task, onDismiss: () -> Unit, onSplit: () -> Unit, modifier: Modifier = Modifier) {
    val offsetY = remember { Animatable(0f) }

    Surface(
        modifier = modifier
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                coroutineScope {
                    detectVerticalDragGestures {
                        change, dragAmount ->
                        change.consume()
                        launch {
                            offsetY.snapTo(offsetY.value + dragAmount)
                        }
                        // Swipe up to dismiss
                        if (dragAmount < -150) { // Swipe threshold
                            onDismiss()
                        }
                    }
                }
            },
        shape = MaterialTheme.shapes.large,
        color = task.color
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Adb, contentDescription = "App Icon", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(task.name, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSplit) {
                    Icon(Icons.Default.VerticalSplit, contentDescription = "Split Screen", tint = Color.White)
                }
            }
            // Fake app content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Fake App Content", color = Color.White, fontSize = 20.sp)
            }
        }
    }
}



@Composable
fun NavigationBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecents: () -> Unit,
    onShowVolume: () -> Unit,
    onShowPower: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .navigationBarsPadding() // Handles insets automatically.
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onShowVolume) { Text("Vol") }
            Button(onClick = onBack) { Text("Back") }
            Button(onClick = onHome) { Text("Home") }
            Button(onClick = onRecents) { Text("Recents") }
            Button(onClick = onShowPower) { Text("Pwr") }
        }
    }
}

// --- Implemented Dialogs & Overlays ---

@Composable
fun HeadsUpNotification(text: String) {
    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .background(Color(0xFF333333), shape = MaterialTheme.shapes.medium)
        ) {
            Text(text, color = Color.White, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun VolumeDialog(volumeLevel: Float, onVolumeChange: (Float) -> Unit, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }, // Dismiss on background click
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .padding(end = 24.dp)
                .height(300.dp)
                .width(100.dp),
            shape = MaterialTheme.shapes.large,
            color = Color(0xFF333333)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Slider(
                    value = volumeLevel,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = -90f
                        }
                        .width(250.dp)
                )
            }
        }
    }
}

@Composable
fun PowerMenu(onDismiss: () -> Unit) {
    var showSafeMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // No ripple on the background
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showSafeMode = true }
                    )
                }
            ) {
                Button(onClick = { /* TODO: Implement Power Off */ }) {
                    Text("Power Off", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(onClick = { /* TODO: Implement Restart */ }) {
                Text("Restart", fontSize = 18.sp)
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(visible = showSafeMode) {
                Button(onClick = { /* TODO: Implement Safe Mode Reboot */ }) {
                    Text("Reboot to Safe Mode", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(if (showSafeMode) 16.dp else 48.dp))

            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 16.sp)
            }
        }
    }
}


@Composable
fun SplitScreenOverlay(task: Task?, onDismiss: () -> Unit) {
    if (task == null) return

    Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
        // Top app
        Box(modifier = Modifier.weight(1f)) {
            TaskCard(task = task, onDismiss = {}, onSplit = {}, modifier = Modifier.fillMaxSize())
        }
        // Bottom app (placeholder)
        Box(modifier = Modifier.weight(1f).background(Color.DarkGray)) {
            Text("Select another app", color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        Button(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)) {
            Text("Exit Split Screen")
        }
    }
}

// --- PREVIEWS ---

@Preview(showSystemUi = true, name = "Locked State")
@Composable
fun LockedPreview() {
    SystemUIComposeDemoTheme {
        KeyguardScreen(onBiometric = {})
    }
}

@Preview(showSystemUi = true, name = "Unlocked State")
@Composable
fun UnlockedPreview() {
    SystemUIComposeDemoTheme {
        Box(Modifier.fillMaxSize()) {
            HomeScreenContent(onTriggerHeadsUp = {})
            StatusBar()
            NavigationBar({}, {}, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, name = "Shade Panel Preview", heightDp = 800)
@Composable
fun ShadePanelPreview() {
    val shadeVM = remember { ShadeViewModel() }
    val quickSettings by shadeVM.quickSettings.collectAsState()
    val notifications by shadeVM.notifications.collectAsState()
    SystemUIComposeDemoTheme {
        Box(modifier = Modifier.background(Color.Black)) {
            ShadePanel(
                expansion = 1f,
                maxHeight = 800.dp,
                quickSettings = quickSettings,
                notifications = notifications,
                onToggleQuickSetting = { shadeVM.toggleQuickSetting(it) },
                onDismissNotification = { shadeVM.dismissNotification(it) }
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Recents Screen Preview")
@Composable
fun RecentsScreenPreview() {
    val recentsVM = remember { RecentsViewModel() }
    val tasks by recentsVM.tasks.collectAsState()
    SystemUIComposeDemoTheme {
        RecentsScreen(
            tasks = tasks,
            onDismissTask = {},
            onDismiss = {},
            onSplit = {}
        )
    }
}

@Preview(showBackground = true, name = "Volume Dialog Preview")
@Composable
fun VolumeDialogPreview() {
    SystemUIComposeDemoTheme {
        VolumeDialog(volumeLevel = 0.5f, onVolumeChange = {}, onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Power Menu Preview")
@Composable
fun PowerMenuPreview() {
    SystemUIComposeDemoTheme {
        PowerMenu(onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Split Screen Preview")
@Composable
fun SplitScreenPreview() {
    val task = Task(1, "Chrome", Color(0xFFF44336))
    SystemUIComposeDemoTheme {
        SplitScreenOverlay(task = task, onDismiss = {})
    }
}
