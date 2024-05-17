package com.roboticamedellin.esp32ble

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.roboticamedellin.esp32ble.ui.theme.ESP32BleTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            var isLoading by remember { mutableStateOf(false) }
            var connectedState by remember { mutableStateOf(false) }
            var uiState by remember { mutableStateOf(UIState.DISCONNECTED) }

            LaunchedEffect(isLoading) {
                if (isLoading) {
                    delay(3000)
                    isLoading = false


                    connectedState = true
                    uiState = UIState.CONNECTED
                }
            }

            ESP32BleTheme {
                BleManagerScreen(
                    connectedState = connectedState,
                    onScanClicked = {
                        uiState = UIState.SCANNING
                    },
                    onDisconnectClicked = {
                        connectedState = false
                        uiState = UIState.DISCONNECTED
                    },
                ) {
                    when (uiState) {
                        UIState.SCANNING -> DeviceListSection(
                            devices = listOf(
                                "Device 1",
                                "Device 2",
                                "Device 3"
                            )
                        ) {
                            isLoading = true
                        }

                        UIState.CONNECTED -> DeviceInteractionSection()
                        UIState.DISCONNECTED -> Box {}
                    }
                }

                if (isLoading) LoadingCover(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
            }
        }
    }
}

enum class UIState {
    SCANNING,
    CONNECTED,
    DISCONNECTED
}

@Composable
fun BleManagerScreen(
    connectedState: Boolean,
    onScanClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    composeSection: @Composable BoxScope.() -> Unit = {}
) {
    Scaffold { _ ->
        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.weight(1f)) {
                composeSection()
            }

            if (connectedState) Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = onDisconnectClicked
            ) {
                Text(text = "Disconnect")
            }
            else Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = onScanClicked
            ) {
                Text(text = "Start Scan")
            }

            AnimatedVisibility(visible = connectedState) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Green),
                    text = "Connected",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Find devices
@Composable
fun DeviceListSection(devices: List<String>, onDeviceSelected: (String) -> Unit = {}) {
    LazyColumn {
        items(devices) { device ->
            DeviceItem(device, onDeviceSelected = onDeviceSelected)
        }
    }
}

@Composable
fun DeviceItem(device: String, onDeviceSelected: (String) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { onDeviceSelected(device) }
    ) {
        Text(modifier = Modifier.padding(vertical = 8.dp), text = device)
    }
}

// Interact with BLE device
@Composable
fun DeviceInteractionSection() {
    Box(modifier = Modifier.fillMaxSize()) {
        Button(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            onClick = { /*TODO*/ }
        ) {
            Text(text = "Send Data")
        }
    }
}

@Composable
fun LoadingCover(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                // do nothing
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
