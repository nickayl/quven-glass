package tv.quven.glass.sample

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import android.view.PixelCopy
import java.io.File
import java.nio.ByteBuffer

/**
 * Copies a region of the window on every frame for a while and writes the frames as raw RGBA files beside a list of
 * their times, as the iOS reference's window recording does, so an animation can be measured frame by frame.
 *
 * @param activity The activity whose window is copied.
 * @param region The region to copy, in window pixels.
 * @param scale The share of the region's size each frame is kept at.
 * @param seconds How long to record for.
 * @param folder The folder the frames, their times and their size are written to; it is emptied first.
 */
internal class WindowRecorder(
    private val activity: Activity,
    private val region: Rect,
    private val scale: Float,
    private val seconds: Float,
    private val folder: File,
) {
    private val width = (region.width() * scale).toInt()
    private val height = (region.height() * scale).toInt()
    private val times = mutableListOf<Long>()
    private val thread = HandlerThread("WindowRecorder").apply { start() }
    private val handler = Handler(thread.looper)
    private var startNanos = 0L

    /** Starts recording on the next frame. */
    fun start() {
        folder.deleteRecursively()
        folder.mkdirs()
        Choreographer.getInstance().postFrameCallback(::onFrame)
    }

    private fun onFrame(frameNanos: Long) {
        if (startNanos == 0L) startNanos = frameNanos
        val elapsed = frameNanos - startNanos
        if (elapsed > (seconds * NanosPerSecond).toLong()) {
            handler.post(::finish)
            return
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(activity.window, region, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                // Each frame goes to its file at once, so a long recording of a large region never fills the memory.
                val pixels = ByteBuffer.allocate(bitmap.byteCount)
                bitmap.copyPixelsToBuffer(pixels)
                File(folder, "frame-%03d.rgba".format(times.size)).writeBytes(pixels.array())
                times += elapsed
            }
            bitmap.recycle()
        }, handler)
        Choreographer.getInstance().postFrameCallback(::onFrame)
    }

    private fun finish() {
        File(folder, "times.txt").writeText(times.mapIndexed { index, nanos -> "$index ${nanos / NanosPerMilli}" }.joinToString("\n"))
        File(folder, "size.txt").writeText("${width}x$height")
        thread.quitSafely()
    }

    private companion object {
        const val NanosPerSecond = 1_000_000_000f
        const val NanosPerMilli = 1_000_000L
    }
}

/**
 * Records [region] of this activity's window, given in density-independent pixels, at [pixelsPerDp], for [seconds],
 * from a second after launch, into `files/window/` of the app's external storage, so a capture starts once the screen
 * has settled.
 *
 * @param region The region to copy, in density-independent pixels from the window's top-left corner.
 * @param pixelsPerDp The pixels each frame keeps per density-independent pixel.
 * @param seconds How long to record for.
 */
internal fun Activity.recordWindowAfterLaunch(region: RectF, pixelsPerDp: Float, seconds: Float) {
    val density = resources.displayMetrics.density
    val pixels = Rect((region.left * density).toInt(), (region.top * density).toInt(), (region.right * density).toInt(), (region.bottom * density).toInt())
    val folder = File(getExternalFilesDir(null), "window")
    Handler(mainLooper).postDelayed({ WindowRecorder(this, pixels, pixelsPerDp / density, seconds, folder).start() }, LaunchSettleMillis)
}

private const val LaunchSettleMillis = 1000L
