package com.roboticamedellin.esp32ble.presentation

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisTickComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberFadingEdges
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.core.common.shape.MarkerCorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
internal fun Chart(modifier: Modifier, listData: List<Float>) {
    val modelProducer = remember { CartesianChartModelProducer.build() }

    LaunchedEffect(listData) {
        withContext(Dispatchers.Default) {
            while (isActive) {
                val dataCopy = listData.toList() // Create a copy of the list
                modelProducer.tryRunTransaction {
                    lineSeries {
                        series(dataCopy)
                    }
                }
                delay(500L)
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lines = listOf(rememberLineSpec(shader = DynamicShader.color(lineColor))),
            ),
            startAxis = rememberStartAxis(
                guideline = null,
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
                titleComponent = rememberTextComponent(
                    color = Color.Black,
                    background = rememberShapeComponent(Shape.Pill, lineColor),
                    padding = Dimensions.of(horizontal = 8.dp, vertical = 2.dp),
                    margins = Dimensions.of(end = 4.dp),
                    typeface = Typeface.MONOSPACE,
                )
            ),
            bottomAxis = rememberBottomAxis(
                sizeConstraint = BaseAxis.SizeConstraint.Exact(20f),
                titleComponent = rememberTextComponent(
                    background = rememberShapeComponent(Shape.Pill, bottomAxisLabelBackgroundColor),
                    color = Color.White,
                    padding = Dimensions.of(horizontal = 2.dp, vertical = 1.dp),
                    margins = Dimensions.of(top = 2.dp),
                    typeface = Typeface.MONOSPACE,
                ),
                itemPlacer = remember {
                    AxisItemPlacer.Horizontal.default(
                        spacing = 1,
                        shiftExtremeTicks = false,
                        addExtremeLabelPadding = false
                    )
                },
                tick = rememberAxisTickComponent(
                    color = Color.White,
                    thickness = 0.5f.dp,
                    shape = RectangleShape
                ),
                tickLength = 0.5f.dp
            ),
            fadingEdges = rememberFadingEdges(),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        marker = rememberMarker(DefaultCartesianMarker.LabelPosition.AroundPoint),
        runInitialAnimation = false,
        horizontalLayout = HorizontalLayout.fullWidth(),
        zoomState = rememberVicoZoomState(zoomEnabled = false),
    )
}

private val lineColor = Color(0xffffbb00)
private val bottomAxisLabelBackgroundColor = Color(0xff9db591)
private val axisValueOverrider = AxisValueOverrider.adaptiveYValues(yFraction = 1.2f, round = true)
