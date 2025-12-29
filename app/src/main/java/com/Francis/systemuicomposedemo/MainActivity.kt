package com.Francis.systemuicomposedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.Francis.systemuicomposedemo.ui.theme.SystemUIComposeDemoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- ARCHITECTURAL SUGGESTION: STATE HOLDER / VIEWMODEL ---
// A dedicated state holder class (like a ViewModel) is used to manage and expose UI state.
class KeyguardViewModel {
    // State is exposed as a StateFlow, the recommended way to handle observable state.
    private val _isLocked = MutableStateFlow(true)
    val isLocked = _isLocked.asStateFlow()

    fun lock() { _isLocked.value = true }
    fun unlock() { _isLocked.value = false }
}

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- ENABLING EDGE-TO-EDGE DISPLAY ---
        // This gives our app full control over the entire screen real estate.
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
    val isLocked by keyguardVM.isLocked.collectAsState()

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
    var showSplit by remember { mutableStateOf(false) }
    var headsUp by remember { mutableStateOf<String?>(null) }

    // A side-effect to manage the lifecycle of a heads-up notification.
    LaunchedEffect(headsUp) {
        if (headsUp != null) {
            delay(3000)
            headsUp = null
        }
    }

    // A System UI is a stack of layers. `BoxWithConstraints` is ideal for this.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val maxHeight = this.maxHeight

        // Layer 0: Home Screen Content (conditionally rendered)
        if (!isLocked) {
            HomeScreenContent(onTriggerHeadsUp = { headsUp = "New Message from Jane!" })
        }

        // Centralized Gesture Input dispatcher.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        // When unlocked, downward drag expands the notification shade.
                        detectVerticalDragGestures { _, dragAmount ->
                            val delta = dragAmount / maxHeight.value
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
            // --- UI LAYERS (drawn from back to front) ---

            // Layer 1: Status Bar (always visible)
            StatusBar()

            // Layer 2: Notification Shade Panel (slides down from the top)
            ShadePanel(expansion = animatedExpansion, maxHeight = maxHeight)

            // Layer 3: Heads-Up Notification (transient, appears over other content)
            headsUp?.let { HeadsUpNotification(it) }

            // Layer 4: System Dialogs & Overlays
            if (showVolume) VolumeDialog { showVolume = false }
            if (showPower) PowerMenu { showPower = false }
            if (showRecents) RecentsScreen(onDismiss = { showRecents = false }, onSplit = { showSplit = true })
            if (showSplit) SplitScreenOverlay { showSplit = false }

            // Layer 5: Navigation Bar (always at the bottom)
            NavigationBar(
                onBack = { if (expansion > 0f) expansion = 0f }, // Back gesture closes shade first.
                onHome = { keyguardVM.lock() }, // Demo: Home button locks the device.
                onRecents = { showRecents = true },
                onShowPower = { showPower = true },
                onShowVolume = { showVolume = true }
            )

            // Layer 6: Keyguard (Lock Screen) - drawn on top of everything.
            if (isLocked) {
                KeyguardScreen(
                    onUnlock = { keyguardVM.unlock() },
                    onBiometric = { keyguardVM.unlock() } // Simulate successful biometric unlock.
                )
            }
        }
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
fun KeyguardScreen(onUnlock: () -> Unit, onBiometric: () -> Unit) {
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
fun ShadePanel(expansion: Float, maxHeight: Dp) {
    if (expansion > 0f) { // Performance optimization.
        Box(
            Modifier
                .fillMaxWidth()
                .height(maxHeight * expansion) // Height is dynamically controlled by the gesture.
                .background(Color.DarkGray.copy(alpha = 0.9f))
        ) {
            Text("Notification Shade", color = Color.White, modifier = Modifier.align(Alignment.Center))
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
            // Added buttons for demoing volume and power menus
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
    // Positioned at the top using a Box wrapper
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
fun VolumeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Volume") },
        text = { Text("Volume controls would appear here.") },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun PowerMenu(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Power Menu") },
        text = { Text("Power Off / Restart options would appear here.") },
        confirmButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun RecentsScreen(onDismiss: () -> Unit, onSplit: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
        Text("Recents Screen", color = Color.White, modifier = Modifier.align(Alignment.Center))
        // Added button to dismiss for now
        Button(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Text("Close")
        }
    }
}

@Composable
fun SplitScreenOverlay(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f))) {
        Text("Split Screen", color = Color.White, modifier = Modifier.align(Alignment.Center))
        Button(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Text("Exit Split")
        }
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, showSystemUi = true, name = "Locked State")
@Composable
fun LockedPreview() {
    SystemUIComposeDemoTheme {
        KeyguardScreen(onUnlock = {}, onBiometric = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Unlocked State")
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

