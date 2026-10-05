package tv.quven.glass

import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reads how bright a backdrop is under an area: a renderer of its own draws the area into a few pixels of a buffer the
 * processor reads, kept for the probe's life, so a sample allocates nothing and the window's frames wait for nothing.
 */
@RequiresApi(33)
internal class GlassBrightnessProbe {

    private val reader = ImageReader.newInstance(Side, Side, PixelFormat.RGBA_8888, MaxImages, ReadableBuffer)
    private val node = RenderNode(Name).apply { setPosition(0, 0, Side, Side) }
    private val renderer = HardwareRenderer().apply {
        setContentRoot(node)
        setSurface(reader.surface)
    }
    private val scope = CanvasDrawScope()
    private val pixels = IntArray(Side * Side)

    @Volatile
    private var drawn: CompletableDeferred<Unit>? = null

    init {
        // Every image is taken off the queue as it arrives, waited for or not: a full queue would stall the
        // render thread the whole window shares.
        reader.setOnImageAvailableListener({ ready ->
            ready.acquireLatestImage()?.use { image -> synchronized(pixels) { read(image) } }
            drawn?.complete(Unit)
        }, Consumer)
    }

    /**
     * Returns what [source] shows under [area]: the mean of its channels and its mean luminance.
     *
     * @param source The layer the backdrop records into.
     * @param area The area, in the source's coordinates.
     * @param density The density the area is drawn at.
     * @return The reading; the previous one while the renderer draws nothing.
     */
    suspend fun sample(source: GraphicsLayer, area: Rect, density: Density): GlassBackdropReading {
        if (area.width <= 0f || area.height <= 0f) return GlassBackdropReading(0f, 0f)
        record(source, area, density)
        val frame = CompletableDeferred<Unit>().also { drawn = it }
        if (renderer.createRenderRequest().syncAndDraw() and NoFrame == 0) withTimeoutOrNull(ImageWaitMillis) { frame.await() }
        drawn = null
        return synchronized(pixels) { GlassBackdropReading(meanChannels(pixels), meanLuminance(pixels)) }
    }

    /** Releases the renderer and its buffer; the probe reads nothing afterwards. */
    fun release() {
        renderer.destroy()
        node.discardDisplayList()
        Consumer.post {
            reader.setOnImageAvailableListener(null, null)
            reader.close()
        }
    }

    private fun record(source: GraphicsLayer, area: Rect, density: Density) {
        val recording = node.beginRecording()
        try {
            scope.draw(density, LayoutDirection.Ltr, Canvas(recording), Size(Side.toFloat(), Side.toFloat())) {
                scale(Side / area.width, Side / area.height, pivot = Offset.Zero) {
                    translate(-area.left, -area.top) { drawLayer(source) }
                }
            }
        } finally {
            node.endRecording()
        }
    }

    private fun read(image: Image) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        for (y in 0 until Side) {
            for (x in 0 until Side) {
                val at = y * plane.rowStride + x * plane.pixelStride
                val red = buffer.get(at).toInt() and 0xFF
                val green = buffer.get(at + 1).toInt() and 0xFF
                val blue = buffer.get(at + 2).toInt() and 0xFF
                pixels[y * Side + x] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }
    }

    private companion object {
        const val Side = 16
        const val MaxImages = 2
        const val ImageWaitMillis = 500L
        const val Name = "QuvenGlassBrightness"
        const val ReadableBuffer = HardwareBuffer.USAGE_CPU_READ_OFTEN or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        const val NoFrame = HardwareRenderer.SYNC_LOST_SURFACE_REWARD_IF_FOUND or
            HardwareRenderer.SYNC_CONTEXT_IS_STOPPED or HardwareRenderer.SYNC_FRAME_DROPPED

        // One thread for every probe of the process takes the images off their queues.
        val Consumer: Handler by lazy { Handler(HandlerThread(Name).apply { start() }.looper) }
    }
}
