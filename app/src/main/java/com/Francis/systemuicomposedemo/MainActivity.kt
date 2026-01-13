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

/**
 * Represents the state of a single Quick Settings tile.
 *
 * @param name The user-visible name of the tile (e.g., "Wi-Fi").
 * @param icon The vector icon to display for the tile.
 * @param isEnabled Whether the tile is currently in an 'on' or 'off' state.
 */
data class QuickSettingTileState(
    val name: String,
    val icon: ImageVector,
    var isEnabled: Boolean = false
)

/**
 * Represents a single notification item.
 *
 * @param id A unique identifier for the notification.
 * @param appName The name of the app that sent the notification.
 * @param title The title of the notification.
 * @param text The body text of the notification.
 */
data class Notification(
    val id: Int,
    val appName: String,
    val title: String,
    val text: String,
)

/**
 * Represents a single task (application) in the Recents screen.
 *
 * @param id A unique identifier for the task.
 * @param name The name of the application.
 * @param color A representative color for the app's theme, used for the card background.
 */
data class Task(
    val id: Int,
    val name: String,
    val color: Color,
)

/**
 * A state-holder (ViewModel) for the Notification Shade.
 *
 * This class holds the state for the Quick Settings tiles and the list of notifications.
 * It follows the Unidirectional Data Flow (UDF) pattern by exposing its state via a read-only
 * [StateFlow] and providing public functions to modify the state in response to UI events.
 */
class ShadeViewModel {
    // Private mutable state for the Quick Settings tiles.
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
    /** The publicly exposed, read-only state for the Quick Settings tiles. */
    val quickSettings = _quickSettings.asStateFlow()

    // Private mutable state for the notifications.
    private val _notifications = MutableStateFlow(
        listOf(
            Notification(1, "Gmail", "New Message", "Check out the new features..."),
            Notification(2, "Slack", "Direct Message", "John Doe sent you a message"),
            Notification(3, "Android Studio", "Build Successful", "app:assembleDebug finished."),
        )
    )
    /** The publicly exposed, read-only state for the notifications. */
    val notifications = _notifications.asStateFlow()

    /** Toggles the enabled state of a specific Quick Setting tile. */
    fun toggleQuickSetting(settingName: String) {
        _quickSettings.value = _quickSettings.value.map {
            if (it.name == settingName) {
                it.copy(isEnabled = !it.isEnabled)
            } else {
                it
            }
        }
    }

    /** Dismisses a notification by its ID. */
    fun dismissNotification(notificationId: Int) {
        _notifications.value = _notifications.value.filterNot { it.id == notificationId }
    }
}

/**
 * A state-holder (ViewModel) for the Recents (app switcher) screen.
 */
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

    /** Dismisses a task from the Recents screen by its ID. */
    fun dismissTask(taskId: Int) {
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
    }
}

/**
 * A state-holder (ViewModel) for the Volume Dialog.
 */
class VolumeViewModel {
    private val _volumeLevel = MutableStateFlow(0.5f)
    val volumeLevel = _volumeLevel.asStateFlow()

    fun setVolume(level: Float) {
        _volumeLevel.value = level
    }
}

/**
 * A state-holder (ViewModel) for the Keyguard (lock screen).
 */
class KeyguardViewModel {
    private val _isLocked = MutableStateFlow(true)
    val isLocked = _isLocked.asStateFlow()

    fun lock() { _isLocked.value = true }
    fun unlock() { _isLocked.value = false }
}

/**
 * The main and only Activity for this application.
 * It sets up the edge-to-edge display and hosts the root [SystemUIComposeDemo] composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This enables drawing under the system bars (status and navigation).
        enableEdgeToEdge()
        // This is the legacy way to do it, but still important for full control.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SystemUIComposeDemoTheme {
                SystemUIComposeDemo()
            }
        }
    }
}

/**
 * The root composable that orchestrates the entire System UI.
 *
 * This function is responsible for:
 * 1.  **State Ownership:** Creating and remembering all the ViewModels (state-holders).
 * 2.  **State Collection:** Collecting state from the ViewModels as Compose `State` objects.
 * 3.  **Layout:** Defining the root layout (`BoxWithConstraints`) and the layering of all UI components.
 * 4.  **Gesture Handling:** Managing the primary drag gestures for the shade and lock screen.
 */
@Composable
fun SystemUIComposeDemo() {
    // 1. STATE OWNERSHIP: Instantiate all ViewModels. `remember` ensures they survive recomposition.
    val keyguardVM = remember { KeyguardViewModel() }
    val shadeVM = remember { ShadeViewModel() }
    val recentsVM = remember { RecentsViewModel() }
    val volumeVM = remember { VolumeViewModel() }

    // 2. STATE COLLECTION: Collect state from Flows and convert to Compose State.
    // The `by` keyword delegates to the State's value, so we can use `isLocked` directly.
    val isLocked by keyguardVM.isLocked.collectAsState()
    val quickSettings by shadeVM.quickSettings.collectAsState()
    val notifications by shadeVM.notifications.collectAsState()
    val tasks by recentsVM.tasks.collectAsState()
    val volumeLevel by volumeVM.volumeLevel.collectAsState()

    // Transient UI state that is owned by this composable.
    // This state is not complex enough to require a full ViewModel.
    var expansion by remember { mutableFloatStateOf(0f) }
    val animatedExpansion by animateFloatAsState(
        targetValue = if (isLocked) 0f else expansion, // Shade must be closed when locked.
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 200f),
        label = "ShadeExpansion"
    )

    // Simple boolean flags to control the visibility of various dialogs and overlays.
    var showVolume by remember { mutableStateOf(false) }
    var showPower by remember { mutableStateOf(false) }
    var showRecents by remember { mutableStateOf(false) }
    var splitScreenTask by remember { mutableStateOf<Task?>(null) }
    val showSplit = splitScreenTask != null
    var headsUp by remember { mutableStateOf<String?>(null) }

    // A side-effect to automatically dismiss the heads-up notification after a delay.
    LaunchedEffect(headsUp) {
        if (headsUp != null) {
            delay(3000)
            headsUp = null
        }
    }

    // 3. LAYOUT: `BoxWithConstraints` is the root layout, perfect for a stack of UI layers.
    // It also provides the screen constraints, which are used for the shade expansion calculation.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 4. GESTURE HANDLING: A single, centralized gesture detector.
            .pointerInput(isLocked) { // The key `isLocked` restarts the gesture detector when the lock state changes.
                if (!isLocked) {
                    // When unlocked, downward drag expands the notification shade.
                    detectVerticalDragGestures { _, dragAmount ->
                        // Calculate the drag delta as a percentage of the screen height.
                        val delta = dragAmount / size.height
                        // Update the expansion state, coercing it between 0% and 100%.
                        expansion = (expansion - delta).coerceIn(0f, 1f)
                    }
                } else {
                    // When locked, a significant swipe up unlocks the device.
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -200) { // Use a threshold to prevent accidental unlocks.
                            keyguardVM.unlock()
                        }
                    }
                }
            }
    ) {
        // The UI layers are drawn here, from back to front.

        // Layer 0: Home Screen Content (conditionally rendered)
        if (!isLocked) {
            HomeScreenContent(onTriggerHeadsUp = { headsUp = "New Message from Jane!" })
        }

        // Layer 1: Status Bar (always visible)
        StatusBar()

        // Layer 2: Notification Shade Panel (slides down from the top)
        ShadePanel(
            expansion = animatedExpansion,
            maxHeight = this.maxHeight,
            quickSettings = quickSettings,
            notifications = notifications,
            onToggleQuickSetting = { shadeVM.toggleQuickSetting(it) },
            onDismissNotification = { shadeVM.dismissNotification(it) }
        )

        // Layer 3: Heads-Up Notification (transient, appears over other content)
        headsUp?.let { HeadsUpNotification(it) }

        // Layer 4: System Dialogs & Overlays
        if (showVolume) VolumeDialog(volumeLevel, { volumeVM.setVolume(it) }) { showVolume = false }
        if (showPower) PowerMenu { showPower = false }
        if (showRecents) RecentsScreen(
            tasks = tasks,
            onDismissTask = { recentsVM.dismissTask(it) },
            onDismiss = { showRecents = false },
            onSplit = { task ->
                splitScreenTask = task
                showRecents = false
            }
        )
        if (showSplit) SplitScreenOverlay(splitScreenTask) { splitScreenTask = null }

        // Layer 5: Navigation Bar (always at the bottom)
        NavigationBar(
            onBack = { /* Back press logic would be handled here */ },
            onHome = { keyguardVM.lock() }, // Demo: Home button locks the device.
            onRecents = { showRecents = true },
            onShowPower = { showPower = true },
            onShowVolume = { showVolume = true }
        )

        // Layer 6: Keyguard (Lock Screen) - drawn on top of everything.
        if (isLocked) {
            KeyguardScreen(
                onBiometric = { keyguardVM.unlock() } // Simulate successful biometric unlock.
            )
        }
    }
}


// --- UI COMPONENTS ---

/** A placeholder for the Home Screen content. */
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

/** The status bar, which displays the time and system icons. */
@Composable
fun StatusBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding() // Automatically adds padding to avoid the system status bar.
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text("12:00 PM", color = Color.White, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text("LTE", color = Color.White, fontWeight = FontWeight.Medium)
    }
}

/** The Keyguard (lock screen) UI. */
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

/**
 * The main panel for the notification shade.
 * Its height is dynamically controlled by the [expansion] parameter.
 * The content inside is animated to appear as the shade expands.
 */
@Composable
fun ShadePanel(
    expansion: Float,
    maxHeight: Dp,
    quickSettings: List<QuickSettingTileState>,
    notifications: List<Notification>,
    onToggleQuickSetting: (String) -> Unit,
    onDismissNotification: (Int) -> Unit
) {
    if (expansion > 0f) { // Performance optimization: Only compose if visible.
        Column(
            Modifier
                .fillMaxWidth()
                .height(maxHeight * expansion) // The height is a direct function of the gesture expansion state.
                .background(Color.DarkGray.copy(alpha = 0.9f))
        ) {
            // Animate the content to fade and slide in as the shade is pulled down.
            AnimatedVisibility(
                visible = expansion > 0.5, // Only show when expanded enough to be useful.
                enter = fadeIn(animationSpec = spring(stiffness = 300f)) + slideInVertically(animationSpec = spring(stiffness = 300f)),
                exit = fadeOut(animationSpec = spring(stiffness = 300f)) + slideOutVertically(animationSpec = spring(stiffness = 300f))
            ) {
                Column(modifier = Modifier.padding(top = 40.dp)) { // Padding for the status bar
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

/** A grid of Quick Settings tiles. */
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

/** A scrollable list of notifications. */
@Composable
fun NotificationList(
    notifications: List<Notification>,
    onDismissNotification: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp) // Avoids overlap with the navigation bar.
    ) {
        items(notifications, key = { it.id }) { notification ->
            NotificationCard(notification, onDismiss = { onDismissNotification(notification.id) })
        }
    }
}

/**
 * A single notification card with a swipe-to-dismiss gesture.
 * This demonstrates an advanced, physics-based animation for the swipe gesture.
 */
@Composable
fun NotificationCard(notification: Notification, onDismiss: () -> Unit) {
    // `Animatable` is a low-level animation API that provides more control than `animate*AsState`.
    val offsetX = remember { Animatable(0f) }

    Box(
        Modifier
            .pointerInput(Unit) {
                coroutineScope {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume() // Consume the event to prevent other gestures from firing.
                            // `snapTo` provides a 1-to-1 mapping of finger movement to the UI element.
                            launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        },
                        onDragEnd = {
                            launch {
                                // If the swipe distance exceeds the threshold, dismiss the card.
                                if (abs(offsetX.value) > 300) {
                                    onDismiss()
                                } else {
                                    // Otherwise, animate the card back to its original position with a springy effect.
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

/**
 * The Recents screen, which displays a horizontal, pager-like list of recent tasks.
 */
@Composable
fun RecentsScreen(
    tasks: List<Task>,
    onDismissTask: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSplit: (Task) -> Unit
) {
    val lazyListState = rememberLazyListState()
    // This is the key to the pager-like effect. It makes the list snap to the nearest item.
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f))) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = 40.dp), // Allows the first and last items to be centered.
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onDismiss = { onDismissTask(task.id) },
                    onSplit = { onSplit(task) },
                    modifier = Modifier
                        .fillParentMaxHeight(0.7f) // Make cards tall.
                        .aspectRatio(9f / 16f) // Typical phone aspect ratio.
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

/** A single card representing a recent task, with a swipe-up-to-dismiss gesture. */
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
                        launch { offsetY.snapTo(offsetY.value + dragAmount) }
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
            // Header with app icon and split screen button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Adb, contentDescription = "App Icon", tint = Color.White) // Placeholder
                Spacer(Modifier.width(8.dp))
                Text(task.name, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSplit) {
                    Icon(Icons.Default.VerticalSplit, contentDescription = "Split Screen", tint = Color.White)
                }
            }
            // Placeholder for the app's content
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

/** The navigation bar, with buttons for Back, Home, and Recents. */
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
                .navigationBarsPadding() // Automatically adds padding to avoid the system navigation bar.
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

/** A transient, heads-up notification that appears at the top of the screen. */
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

/** A custom volume dialog with a vertical slider. */
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
                // This is a classic technique for creating a vertical slider in Compose.
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

/** A custom power menu with a long-press "easter egg". */
@Composable
fun PowerMenu(onDismiss: () -> Unit) {
    var showSafeMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // No ripple on the background click.
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The Box wrapper is used to apply the long-press listener to the Button.
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

            // This hidden option is revealed on long-press.
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

/** A placeholder for the split-screen UI. */
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
