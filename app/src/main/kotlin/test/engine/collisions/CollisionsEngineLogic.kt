package test.engine.collisions

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.engine.EngineInputCallback
import sp.kx.lwjgl.engine.EngineLogic
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.MutablePoint
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.center
import sp.kx.math.centerPoint
import sp.kx.math.measure.Measure
import sp.kx.math.measure.MutableDoubleMeasure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.minus
import sp.kx.math.moved
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.vectorOf
import test.engine.collisions.entity.MutableMoving
import test.engine.collisions.util.FontInfoUtil
import java.util.concurrent.TimeUnit

internal class CollisionsEngineLogic(private val engine: Engine) : EngineLogic {
    private val env = getEnvironment()
    private lateinit var shouldEngineStopUnit: Unit
    override val inputCallback = object : EngineInputCallback {
        override fun onKeyboardButton(button: KeyboardButton, isPressed: Boolean) {
            if (isPressed) when (button) {
                KeyboardButton.ESCAPE -> shouldEngineStopUnit = Unit
                KeyboardButton.P -> env.measure.magnitude = when (env.measure.magnitude) {
                    16.0 -> 24.0
                    24.0 -> 32.0
                    32.0 -> 40.0
                    else -> 16.0
                }
                else -> Unit
            }
        }
    }

    private fun onRenderCamera(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(-1.0, 0.0, 1.0, 0.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(0.0, -1.0, 0.0, 1.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
//        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
//        canvas.texts.draw(
//            color = Color.GREEN,
//            info = info,
//            pointTopLeft = Point.Center.moved(0.5),
//            text = String.format("x: %.2f y: %.2f", env.camera.point.x, env.camera.point.y),
//            offset = offset,
//            measure = measure,
//        )
    }

    private fun onRenderGrid(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        point: Point,
    ) {
        val pictureSize = engine.property.pictureSize - measure
        canvas.vectors.draw(
            color = Color.WHITE,
            vector = vectorOf(
                startX = pictureSize.width / 2,
                startY = 0.0,
                finishX = pictureSize.width / 2,
                finishY = 2.0,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(
                startX = 0.0,
                startY = 1.0,
                finishX = pictureSize.width,
                finishY = 1.0,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        canvas.vectors.draw(
            color = Color.WHITE,
            vector = vectorOf(
                startX = 0.0,
                startY = pictureSize.height / 2,
                finishX = 2.0,
                finishY = pictureSize.height / 2,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        canvas.vectors.draw(
            color = Color.GREEN,
            vector = vectorOf(
                startX = 1.0,
                startY = 0.0,
                finishX = 1.0,
                finishY = pictureSize.height,
            ),
            lineWidth = 0.1,
            measure = measure,
        )
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        val xHalf = pictureSize.width.toInt() / 2
        val xNumber = kotlin.math.ceil(point.x).toInt()
        val xNumbers = (xNumber - xHalf - 2)..(xNumber + xHalf)
        for (x in xNumbers) {
            val textY = if (x % 2 == 0) 1.0 else 0.25
            canvas.texts.draw(
                color = Color.GREEN,
                info = info,
                pointTopLeft = pointOf(x = x + offset.dX + 0.25, y = textY),
                measure = measure,
                text = String.format("%d", x),
            )
            val lineY = if (x % 2 == 0) 1.5 else 0.5
            canvas.vectors.draw(
                color = Color.GREEN,
                vector = pointOf(x = x + offset.dX, y = 1.0) + pointOf(x = x + offset.dX, y = lineY),
                lineWidth = 0.1,
                measure = measure,
            )
        }
        val yHalf = pictureSize.height.toInt() / 2
        val yNumber = kotlin.math.ceil(point.y).toInt()
        val yNumbers = (yNumber - yHalf + 2)..(yNumber + yHalf)
        for (y in yNumbers) {
            val textX = if (y % 2 == 0) 1.25 else 1.75
            canvas.texts.draw(
                color = Color.GREEN,
                info = info,
                pointTopLeft = pointOf(x = textX, y = y + offset.dY),
                measure = measure,
                text = String.format("%d", y),
            )
            val lineX = if (y % 2 == 0) 0.5 else 1.5
            canvas.vectors.draw(
                color = Color.GREEN,
                vector = pointOf(x = 1.0, y = y + offset.dY) + pointOf(x = lineX, y = y + offset.dY),
                lineWidth = 0.1,
                measure = measure,
            )
        }
    }

    private fun onRenderDebug(
        canvas: Canvas,
        measure: Measure<Double, Double>,
    ) {
        val info = FontInfoUtil.getFontInfo(height = 0.75, measure = measure)
        val pictureSize = engine.property.pictureSize - measure
        var y = 0
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = pointOf(
                x = 4.0,
                y = 2.0 + y++,
            ),
            text = String.format(
                "Picture: %.1fx%.1f (%.2fx%.2f)",
                engine.property.pictureSize.width,
                engine.property.pictureSize.height,
                pictureSize.width,
                pictureSize.height,
            ),
            measure = measure,
        )
        canvas.texts.draw(
            color = Color.GREEN,
            info = info,
            pointTopLeft = pointOf(
                x = 4.0,
                y = 2.0 + y++,
            ),
            text = String.format(
                "Camera: x %.1f y %.1f",
                env.camera.point.x,
                env.camera.point.y,
            ),
            measure = measure,
        )
    }

    override fun onRender(canvas: Canvas) {
        val centerOffset = engine.property.pictureSize.center() - env.measure
        onRenderCamera(
            canvas = canvas,
            offset = centerOffset,
            measure = env.measure,
        )
        val point = env.camera.point
        val centerPoint = engine.property.pictureSize.centerPoint() - env.measure
        val offset = centerPoint - point
        if (engine.input.keyboard.isPressed(KeyboardButton.I)) {
            onRenderGrid(
                canvas = canvas,
                offset = offset,
                measure = env.measure,
                point = point,
            )
            onRenderDebug(
                canvas = canvas,
                measure = env.measure,
            )
        }
    }

    override fun shouldEngineStop(): Boolean {
        return ::shouldEngineStopUnit.isInitialized
    }

    companion object {
        private fun getEnvironment(): Environment {
            val measure = MutableDoubleMeasure(24.0)
            val camera = MutableMoving(
                point = MutablePoint(x = 0.0, y = 0.0),
                speed = MutableSpeed(magnitude = 12.0, timeUnit = TimeUnit.SECONDS),
            )
            return Environment(
                measure = measure,
                camera = camera,
            )
        }
    }
}
