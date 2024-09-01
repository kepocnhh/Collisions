package test.engine.collisions

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.engine.EngineInputCallback
import sp.kx.lwjgl.engine.EngineLogic
import sp.kx.lwjgl.engine.input.Keyboard
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.MutableOffset
import sp.kx.math.MutablePoint
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.angleOf
import sp.kx.math.center
import sp.kx.math.centerPoint
import sp.kx.math.distanceOf
import sp.kx.math.getPerpendicular
import sp.kx.math.getShortestPoint
import sp.kx.math.gt
import sp.kx.math.isEmpty
import sp.kx.math.measure.Measure
import sp.kx.math.measure.MutableDoubleMeasure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.measure.diff
import sp.kx.math.measure.isEmpty
import sp.kx.math.minus
import sp.kx.math.moved
import sp.kx.math.offsetOf
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.radians
import sp.kx.math.sizeOf
import sp.kx.math.toOffset
import sp.kx.math.toString
import sp.kx.math.vectorOf
import test.engine.collisions.entity.Body
import test.engine.collisions.entity.Circle
import test.engine.collisions.entity.Dot
import test.engine.collisions.entity.Line
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
                KeyboardButton.Q -> env.paused = !env.paused
                KeyboardButton.I -> env.debug = !env.debug
                else -> Unit
            }
        }
    }

    private fun moveCamera(camera: MutableMoving) {
        val offset = engine.input.keyboard.getOffset(
            upKey = KeyboardButton.W,
            downKey = KeyboardButton.S,
            leftKey = KeyboardButton.A,
            rightKey = KeyboardButton.D,
        )
        if (offset.isEmpty()) return
        val timeDiff = engine.property.time.diff()
        val length = camera.speed.length(timeDiff)
        val multiplier = kotlin.math.min(1.0, distanceOf(offset))
        camera.point.move(
            length = length * multiplier,
            angle = angleOf(offset).radians(),
        )
    }

    private fun moveBodies(bodies: List<Body>) {
        val timeDiff = engine.property.time.diff()
        for (index in bodies.indices) {
            val body = bodies[index]
            // a = (v - v0) / t
            // v = a * t + v0
            val d = body.acceleration.speed(timeDiff, TimeUnit.NANOSECONDS)
            val v0 = body.moving.speed.per(TimeUnit.NANOSECONDS)
//            println("$index] v0: " + v0.toString(points = 6))
//            println("$index] d: " + d.toString(points = 6))
            val v = kotlin.math.max(v0 + d, 0.0)
            if (v == 0.0) {
                body.moving.speed.clear()
                body.acceleration.clear() // todo
                continue
            }
            body.moving.speed.set(v, TimeUnit.NANOSECONDS)
            val length = body.moving.speed.length(timeDiff)
            println("$index] length: $length")
            println("$index] before: ${body.moving.point}")
            val target = body.moving.point.moved(
                length = length,
                angle = body.direction,
            )
            println("$index] after: $target")
            body.moving.point.set(target)
        }
    }

    private fun onRenderBodies(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        bodies: List<Body>,
    ) {
        val info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure)
        for (index in bodies.indices) {
            val body = bodies[index]
            canvas.polygons.drawCircle(
                borderColor = Color.BLUE,
                fillColor = Color.BLUE.copy(alpha = 0.5f),
                pointCenter = body.moving.point,
                radius = 1.0,
                lineWidth = 0.1,
                edgeCount = 16,
                offset = offset,
                measure = measure,
            )
            canvas.texts.draw(
                color = Color.BLUE,
                info = info,
                pointTopLeft = body.moving.point,
                offset = offset + offsetOf(dX = -0.5, dY = -0.5),
                measure = measure,
                text = String.format("%02d", index),
            )
        }
    }

    private fun onRenderCamera(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.vectors.draw(
            color = Color.WHITE,
            vector = vectorOf(-0.5, 0.0, 0.5, 0.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.1,
        )
        canvas.vectors.draw(
            color = Color.WHITE,
            vector = vectorOf(0.0, -0.5, 0.0, 0.5),
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
        env.bodies.forEachIndexed { index, body ->
            canvas.texts.draw(
                color = Color.GREEN,
                info = info,
                pointTopLeft = pointOf(
                    x = 4.0,
                    y = 2.0 + y++,
                ),
                text = String.format(
                    "Body #$index: x %.1f y %.1f",
                    body.moving.point.x,
                    body.moving.point.y,
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
                    "    speed: ${body.moving.speed}",
                    body.moving.point.x,
                    body.moving.point.y,
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
                    "    acceleration: ${body.acceleration}",
                    body.moving.point.x,
                    body.moving.point.y,
                ),
                measure = measure,
            )
        }
    }

    override fun onRender(canvas: Canvas) {
        // todo time diff
        moveCamera(camera = env.camera)
        if (!env.paused) {
            moveBodies(bodies = env.bodies)
        }
        val point = env.camera.point
        val centerPoint = engine.property.pictureSize.centerPoint() - env.measure
        val offset = centerPoint - point
        onRenderBodies(
            canvas = canvas,
            offset = offset,
            measure = env.measure,
            bodies = env.bodies,
        )
        for (line in env.lines) {
            canvas.vectors.draw(
                color = line.color,
                vector = line.vector,
                lineWidth = line.width,
                offset = offset,
                measure = env.measure,
            )
        }
        for (circle in env.circles) {
            canvas.polygons.drawCircle(
                color = circle.color,
                pointCenter = circle.pointCenter,
                radius = circle.radius,
                edgeCount = 16,
                lineWidth = 0.1,
                offset = offset,
                measure = env.measure,
            )
        }
        val dotSize = sizeOf(0.2, 0.2)
        for (dot in env.dots) {
            canvas.polygons.drawRectangle(
                color = dot.color,
                pointTopLeft = dot.point,
                size = dotSize,
                offset = offset - dotSize.center(),
                measure = env.measure,
            )
        }
        val centerOffset = engine.property.pictureSize.center() - env.measure
        onRenderCamera(
            canvas = canvas,
            offset = centerOffset,
            measure = env.measure,
        )
        if (env.debug) {
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
            val bodies = listOf(
                Body(
                    moving = MutableMoving(
                        point = MutablePoint(x = -4.0, y = 0.0),
                        speed = MutableSpeed(magnitude = 1.0, timeUnit = TimeUnit.SECONDS),
                    ),
                    acceleration = MutableAcceleration(magnitude = -0.1, timeUnit = TimeUnit.SECONDS),
                    direction = 0.0,
                    mass = 1.0,
                ),
                Body(
                    moving = MutableMoving(
                        point = MutablePoint(x = 4.0, y = 0.0),
                        speed = MutableSpeed(magnitude = 0.0, timeUnit = TimeUnit.SECONDS),
                    ),
                    acceleration = MutableAcceleration(magnitude = 0.0, timeUnit = TimeUnit.SECONDS),
                    direction = 0.0,
                    mass = 1.0,
                ),
            )
            val cs = Circle(
                pointCenter = pointOf(0, 0),
                radius = 2.0,
                color = Color.WHITE,
            )
            val ct = Circle(
                pointCenter = pointOf(8, 4),
                radius = 2.0,
                color = Color.GRAY,
            )
            val c1 = Circle(
                pointCenter = pointOf(11, 1),
                radius = 3.0,
                color = Color.YELLOW,
            )
            val d1 = Dot(
                point = (cs.pointCenter + ct.pointCenter).getPerpendicular(c1.pointCenter),
                color = Color.YELLOW,
            )
            val c1d1 = distanceOf(c1.pointCenter, d1.point)
            val c1cf = c1.radius + cs.radius
            val d1cf = kotlin.math.sqrt(c1cf * c1cf - c1d1 * c1d1)
            val cf = Circle(
                pointCenter = d1.point.moved(length = d1cf, angle = angleOf(d1.point, cs.pointCenter)),
                color = Color.WHITE,
                radius = cs.radius,
            )
            val lines = listOf(
                Line(
                    vector = cs.pointCenter + ct.pointCenter,
                    color = Color.GRAY.copy(alpha = 0.5f),
                    width = 0.05,
                ),
                Line(
                    vector = c1.pointCenter + d1.point,
                    color = Color.YELLOW.copy(alpha = 0.5f),
                    width = 0.05,
                ),
            )
            val circles = listOf(cs, ct, c1, cf)
            val dots = listOf(
                Dot(
                    point = cs.pointCenter,
                    color = Color.WHITE,
                ),
                Dot(
                    point = c1.pointCenter,
                    color = Color.YELLOW,
                ),
                Dot(
                    point = ct.pointCenter,
                    color = Color.GRAY.copy(alpha = 0.5f),
                ),
                d1,
                Dot(
                    point = cf.pointCenter,
                    color = Color.WHITE,
                ),
            )
            return Environment(
                measure = measure,
                camera = camera,
                bodies = emptyList(),
//                bodies = bodies,
                paused = true,
                debug = false,
                lines = lines,
                circles = circles,
                dots = dots,
            )
        }

        private fun Keyboard.getOffset(
            upKey: KeyboardButton,
            downKey: KeyboardButton,
            leftKey: KeyboardButton,
            rightKey: KeyboardButton,
        ): Offset {
            val result = MutableOffset(dX = 0.0, dY = 0.0)
            val down = isPressed(downKey)
            if (isPressed(upKey)) {
                if (!down) result.dY = -1.0
            } else if (down) {
                result.dY = 1.0
            }
            val right = isPressed(rightKey)
            if (isPressed(leftKey)) {
                if (!right) result.dX = -1.0
            } else if (right) {
                result.dX = 1.0
            }
            return result
        }
    }
}
