package com.myxoz.mandelbrot

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

const val UHIGH_RES = 2
const val CHIGH_RES = 3
const val VHIGH_RES = 4
const val PHIGH_RES = 6
const val HIGH_RES = 8
const val LOW_RES = 10
class MainActivity : ComponentActivity() {
    @SuppressLint("ReturnFromAwaitPointerEventScope")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(0xFFFFFF))
        setContent {
            MandelbrotBitmapView()
        }
    }
}
fun isPartOfSet(c0real: Double, c0imaginary: Double): Boolean {
    var znreal = c0real
    var znimaginary = c0imaginary
    for (i in 0 until 100) {
        val zn = znreal
        znreal = zn * zn - znimaginary * znimaginary + c0real
        znimaginary = 2 * zn * znimaginary + c0imaginary
        if (znreal * znreal + znimaginary * znimaginary > 4) {
            return false
        }
    }
    return true
}
fun calculateMandelbrotBitmap(
    width: Int,
    height: Int,
    centerX: Double,
    centerY: Double,
    zoom: Double,
    resolution: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint()
    val widthToHeightRatio = width / height.toDouble()
    val totalWidth = zoom * widthToHeightRatio
    val left = centerX - totalWidth / 2
    val top = centerY - zoom / 2
    val gridWidth = width / resolution
    val gridHeight = height / resolution
    val stepX = (totalWidth / width) * resolution
    val stepY = (zoom / height) * resolution
    for (gy in 0 until gridHeight) {
        for (gx in 0 until gridWidth) {
            val cx = left + gx * stepX
            val cy = top + gy * stepY
            val inside = isPartOfSet(cx, cy)
            paint.color = if (inside) Color.White.toArgb() else Color.Black.toArgb()
            val pixelX = gx * resolution
            val pixelY = gy * resolution
            canvas.drawRect(
                pixelX.toFloat(),
                pixelY.toFloat(),
                (pixelX + resolution).toFloat(),
                (pixelY + resolution).toFloat(),
                paint
            )
        }
    }
    return bitmap
}
@Composable
fun MandelbrotBitmapView() {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val width = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val height = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    var resolution by remember { mutableIntStateOf(LOW_RES) }
    var centerX by remember { mutableDoubleStateOf(-0.75) }
    var centerY by remember { mutableDoubleStateOf(0.0) }
    var zoom by remember { mutableDoubleStateOf(2.0) }
    val bitmapState  = calculateMandelbrotBitmap(width, height, centerX, centerY, zoom, resolution)
    val lastTouchTs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(resolution, zoom) {
        while(System.currentTimeMillis()-lastTouchTs<500){
            delay(20)
        }
        println("Resolution Increase!")
        resolution = when(resolution){
            LOW_RES -> HIGH_RES
            HIGH_RES -> PHIGH_RES
            PHIGH_RES -> VHIGH_RES
            VHIGH_RES -> CHIGH_RES
            CHIGH_RES -> UHIGH_RES
            UHIGH_RES -> UHIGH_RES
            else -> LOW_RES
        }
    }

    Image(bitmap = bitmapState.asImageBitmap(), contentDescription = "Mandelbrot Set", modifier = Modifier.fillMaxSize()
        .pointerInput(Unit) {
            detectTransformGestures(
                onGesture = { _, pan, zoomChange, _ ->
                    resolution = HIGH_RES
                    zoom /= zoomChange
                    centerX -= pan.x * zoom / height.toDouble()
                    centerY -= pan.y * zoom / height.toDouble()
                }
            )
        }.background(Color.Black),
        contentScale = ContentScale.Crop
    )
}