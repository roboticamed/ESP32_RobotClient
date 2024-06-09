package com.roboticamedellin.esp32ble.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.roboticamedellin.esp32ble.framework.BleScanner
import com.roboticamedellin.esp32ble.presentation.ui.theme.ESP32BleTheme
import com.roboticamedellin.esp32ble.repository.BLEDevicesRepositoryImpl
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val bleScanner = BleScanner(this)
    private val bleDevicesRepository = BLEDevicesRepositoryImpl(bleScanner)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initDependencies(bleDevicesRepository)

        setContent {

            var isLoading by remember { mutableStateOf(false) }
            var connectedState by remember { mutableStateOf(false) }
            var uiState by remember { mutableStateOf(UIState.DISCONNECTED) }

            val devicesListState by viewModel.deviceListState.collectAsState()

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
                        viewModel.startScan()
                    },
                    onDisconnectClicked = {
                        connectedState = false
                        uiState = UIState.DISCONNECTED
                    },
                ) {
                    when (uiState) {
                        UIState.SCANNING -> DeviceListSection(
                            devices = devicesListState
                        ) { itemSelected ->
                            isLoading = true
                            viewModel.connectToDevice(itemSelected.address)
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

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
    fun DeviceListSection(devices: List<BLEItemUI>, onDeviceSelected: (BLEItemUI) -> Unit = {}) {
        LazyColumn {
            items(devices) { device ->
                DeviceItem(device, onDeviceSelected = onDeviceSelected)
            }
        }
    }

    @Composable
    fun DeviceItem(bleItem: BLEItemUI, onDeviceSelected: (BLEItemUI) -> Unit = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clickable { onDeviceSelected(bleItem) }
        ) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = "${bleItem.name} -> ${bleItem.address}"
            )
        }
    }

    fun String.isFloat(): Boolean {
        return this.toFloatOrNull() != null
    }

    // Interact with BLE device
    @Composable
    fun DeviceInteractionSection() {
        val dataFlowState by viewModel.dataFlowState.collectAsState("---")
        var flagState by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    flagState = !flagState
                    viewModel.sendCommand(if (flagState) "A" else "B")
                }
            ) {
                Text(text = "Send Data")
            }
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = dataFlowState,
                textAlign = TextAlign.Center
            )
//            if (dataFlowState.isFloat()) {
//                Log.i("MainActivity", "Data: $dataFlowState")
//                XYGraph(value = dataFlowState.toFloat())
//            }
            Chart(modifier = Modifier.fillMaxWidth())
        }
    }
//    @Composable
//    fun XYGraph(value: Float) {
//        val data = remember { mutableStateListOf<Float>() }
//        data.add(value)
//
//        if (data.size > 100) data.removeAt(0)
//
//        val maxY = data.maxOrNull() ?: 0f
//        val maxX = data.size.toFloat()
//
//        var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
//
//        Canvas(modifier = Modifier
//            .fillMaxWidth()
//            .height(200.dp) // Set an appropriate height for the graph
//            .onSizeChanged {
//                canvasSize = it.toSize()
//            }
//        ) {
//            val width = canvasSize.width
//            val height = canvasSize.height
//
//            if (width > 0 && height > 0) {
//                val stepX = width / maxX
//                val stepY = height / maxY
//
//                val paint = Paint().apply { color = Color.Blue }
//
//                for (i in 0 until data.size - 1) {
//                    val startX = i * stepX
//                    val startY = height - data[i] * stepY
//                    val endX = (i + 1) * stepX
//                    val endY = height - data[i + 1] * stepY
//
//                    drawLine(
//                        start = Offset(startX, startY),
//                        end = Offset(endX, endY),
//                        color = Color.Blue,
//                        strokeWidth = 4.dp.toPx()
//                    )
//                }
//            }
//        }
//    }

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

}
