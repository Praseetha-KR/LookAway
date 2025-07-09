package `in`.imagineer.lookaway.ui.screens

import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.core.view.WindowCompat
import `in`.imagineer.lookaway.R
import `in`.imagineer.lookaway.ui.components.PetalsAnimation


@Composable
fun EyeBreakScreen(
    isActive: Boolean,
    timeUntilNext: Long,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    intervalMinutes: Int,
    hasNotificationPermission: Boolean,
    onToggle: () -> Unit,
    onTimeChange: (Int, Int, Int, Int) -> Unit,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    // StatusBar theme
    val statusBarColor = Color(0xFFD0C9C7)
    val navBarColor = Color(0xFF000000)
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        DisposableEffect(Unit) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.isAppearanceLightNavigationBars = false
            onDispose {}
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(statusBarColor)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(navBarColor)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {}
    }

    // Local state for input fields
    var startHourText by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d", startHour)) }
    var startMinuteText by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d", startMinute)) }
    var endHourText by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d", endHour)) }
    var endMinuteText by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d", endMinute)) }
    var intervalText by remember { mutableStateOf(intervalMinutes.toString()) }

    LaunchedEffect(startHour, startMinute, endHour, endMinute, intervalMinutes) {
        startHourText = String.format(Locale.getDefault(), "%02d", startHour)
        startMinuteText = String.format(Locale.getDefault(), "%02d", startMinute)
        endHourText = String.format(Locale.getDefault(), "%02d", endHour)
        endMinuteText = String.format(Locale.getDefault(), "%02d", endMinute)
        intervalText = intervalMinutes.toString()
    }

    fun validateAndUpdateTime() {
        val sH = startHourText.toIntOrNull()?.coerceIn(0, 23) ?: startHour
        val sM = startMinuteText.toIntOrNull()?.coerceIn(0, 59) ?: startMinute
        val eH = endHourText.toIntOrNull()?.coerceIn(0, 23) ?: endHour
        val eM = endMinuteText.toIntOrNull()?.coerceIn(0, 59) ?: endMinute

        startHourText = String.format(Locale.getDefault(), "%02d", sH)
        startMinuteText = String.format(Locale.getDefault(), "%02d", sM)
        endHourText = String.format(Locale.getDefault(), "%02d", eH)
        endMinuteText = String.format(Locale.getDefault(), "%02d", eM)

        onTimeChange(sH, sM, eH, eM)
    }

    fun validateAndUpdateInterval() {
        val interval = intervalText.toIntOrNull()?.coerceIn(1, 120) ?: intervalMinutes
        intervalText = interval.toString()
        onIntervalChange(interval)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "🌿 LookAway",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Eye Break Reminder",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!hasNotificationPermission) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(16.dp),
                        ) {
                            Column {
                                Text(
                                    text = "Grant notification permissions for this app from settings.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    if (isActive) {
                        Text(
                            text = "Look at an object\n20 feet away\nfor 20 seconds\nevery $intervalMinutes minutes",
                            fontSize = 2.5.em,
                            lineHeight = 1.4.em,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Box(
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = "Enable notifications from this app \nin your smart watch for the best experience",
                                fontSize = 2.6.em,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.64F)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth,
            )

            if (isActive) {
                PetalsAnimation(isActive = isActive, petalCount = 7, spawnInterval = 3000L)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Reminders OFF state
                if (hasNotificationPermission && !isActive) {
                    Spacer(modifier = Modifier.weight(1.0f))

                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("from ", color = MaterialTheme.colorScheme.onSurface)
                                Card(
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextField(
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor= Color.White,
                                                unfocusedContainerColor = Color.White,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                            ),
                                            value = startHourText,
                                            onValueChange = { startHourText = it },
                                            modifier = Modifier
                                                .height(52.dp)
                                                .width(60.dp)
                                                .onFocusChanged { focusState ->
                                                    if (!focusState.isFocused) {
                                                        validateAndUpdateTime()
                                                    }
                                                },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        Text(":", color = MaterialTheme.colorScheme.inverseOnSurface)
                                        TextField(
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor= Color.White,
                                                unfocusedContainerColor = Color.White,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                            ),
                                            value = startMinuteText,
                                            onValueChange = { startMinuteText = it },
                                            modifier = Modifier
                                                .height(52.dp)
                                                .width(60.dp)
                                                .onFocusChanged { focusState ->
                                                    if (!focusState.isFocused) {
                                                        validateAndUpdateTime()
                                                    }
                                                },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        )
                                    }
                                }
                                Text(" to ", color = MaterialTheme.colorScheme.onSurface)
                                Card(
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TextField(
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor= Color.White,
                                                unfocusedContainerColor = Color.White,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                            ),
                                            value = endHourText,
                                            onValueChange = { endHourText = it },
                                            modifier = Modifier
                                                .height(52.dp)
                                                .width(60.dp)
                                                .onFocusChanged { focusState ->
                                                    if (!focusState.isFocused) {
                                                        validateAndUpdateTime()
                                                    }
                                                },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        Text(":", color = MaterialTheme.colorScheme.inverseOnSurface)
                                        TextField(
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor= Color.White,
                                                unfocusedContainerColor = Color.White,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedBorderColor = Color.Transparent,
                                            ),
                                            value = endMinuteText,
                                            onValueChange = { endMinuteText = it },
                                            modifier = Modifier
                                                .height(52.dp)
                                                .width(60.dp)
                                                .onFocusChanged { focusState ->
                                                    if (!focusState.isFocused) {
                                                        validateAndUpdateTime()
                                                    }
                                                },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("repeat every", color = MaterialTheme.colorScheme.onSurface)
                                OutlinedTextField(
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor= Color.White,
                                        unfocusedContainerColor = Color.White,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    ),
                                    value = intervalText,
                                    onValueChange = { intervalText = it },
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(52.dp)
                                        .onFocusChanged { focusState ->
                                            if (!focusState.isFocused) {
                                                validateAndUpdateInterval()
                                            }
                                        },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Text("min", color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(modifier = Modifier.height(64.dp))

                            Button(
                                onClick = onToggle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play"
                                    )
                                    Text(
                                        text = "Start Reminders",
                                        fontSize = 2.6.em,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }

                // Reminders ON state
                if (isActive) {  // && timeUntilNext > 0
                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp
                        val circleSize = screenWidth * 0.91f

                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier.size(circleSize)
                        ) {
                            val totalIntervalMillis = intervalMinutes * 60 * 1000L
                            val progress = timeUntilNext.toFloat() / totalIntervalMillis.toFloat()

                            CircularProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.size(circleSize),
                                strokeWidth = 8.dp,
                                trackColor = Color.Transparent,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top,
                            ) {
                                val hours = TimeUnit.MILLISECONDS.toHours(timeUntilNext)
                                val minutes = TimeUnit.MILLISECONDS.toMinutes(timeUntilNext) % 60
                                val seconds = TimeUnit.MILLISECONDS.toSeconds(timeUntilNext) % 60

                                Spacer(modifier = Modifier.height(120.dp))

                                Text(
                                    text = "next reminder in",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m ${seconds}s",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(64.dp))

                        Button(
                            onClick = onToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Stop",
                                    tint = Color.White,
                                )
                                Text(
                                    text = "Stop Reminders",
                                    fontSize = 2.5.em,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1.0f))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                        ) {
                            Column {
                                Text(
                                    text = "STARTS AT",
                                    fontSize = 1.8.em,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute),
                                    fontSize = 3.em,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                            Spacer(modifier = Modifier.weight(1.0f))
                            Column {
                                Text(
                                    text = "ENDS AT",
                                    fontSize = 1.8.em,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute),
                                    fontSize = 3.em,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}