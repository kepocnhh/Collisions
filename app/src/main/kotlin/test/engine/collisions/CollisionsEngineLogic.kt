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
import sp.kx.math.Vector
import sp.kx.math.angle
import sp.kx.math.angleOf
import sp.kx.math.center
import sp.kx.math.centerPoint
import sp.kx.math.distanceOf
import sp.kx.math.getPerpendicular
import sp.kx.math.getShortestPoint
import sp.kx.math.isEmpty
import sp.kx.math.measure.Measure
import sp.kx.math.measure.MutableDoubleMeasure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.measure.diff
import sp.kx.math.minus
import sp.kx.math.moved
import sp.kx.math.offsetOf
import sp.kx.math.plus
import sp.kx.math.pointOf
import sp.kx.math.radians
import sp.kx.math.sizeOf
import sp.kx.math.vectorOf
import test.engine.collisions.entity.Body
import test.engine.collisions.entity.Circle
import test.engine.collisions.entity.Dot
import test.engine.collisions.entity.Line
import test.engine.collisions.entity.MutableMoving
import test.engine.collisions.util.FontInfoUtil
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.nanoseconds

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
                KeyboardButton.B -> if (!env.paused) {
                    val body = env.bodies.firstOrNull() ?: TODO()
                    body.velocity.add(
                        magnitude = 4.0,
                        timeUnit = TimeUnit.SECONDS,
                        angle = angleOf(env.camera.point, body.point),
                    )
                }
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

    private fun getFinalPoint(
        current: Point,
        target: Point,
        minDistance: Double,
        vectors: List<Vector>,
        angle: Double,
    ): Pair<Point, Double>? {
        val targetDistance = distanceOf(current, target)
        val nearest = vectors.filter { vector ->
            vector.closerThan(point = current, minDistance = targetDistance + minDistance)
        }
        val anyCloser = nearest.anyCloserThan(point = target, minDistance = minDistance)
        if (!anyCloser) return target to angle
        val correctedPoints = nearest.map { vector ->
            val tp = vector.getPerpendicular(target = target)
            val cp = vector.getPerpendicular(target = current)
//            val ft = tp.moved(length = minDistance, angle = angleOf(tp, target))
//            val ct = tp.moved(length = distanceOf(cp, current), angle = angleOf(tp, target))
//            val fc = cp.moved(length = minDistance, angle = angleOf(cp, current))
//            val ct_t = distanceOf(ct, target)
//            val ct_c = distanceOf(ct, current)
//            val c_t = distanceOf(current, target)
//            val ft_t = distanceOf(ft, target)
            val ft_t = minDistance - distanceOf(tp, target)
            // ct_c / ft_f = ct_t / ft_t
            // ft_f = ft_t * ct_c / ct_t
//            val ft_f = ft_t * ct_c / ct_t
//            val f = ft.moved(length = ft_f, angleOf(tp, cp))
            val at = angleOf(tp, target)
//            val f = target.moved(length = ft_t * 2, angle = at)
            val tf = tp.moved(length = minDistance, angle = angleOf(cp, current))
            val f = tf.moved(length = distanceOf(target, tf), angle = angleOf(cp, current))
            val av = vector.angle()
            val ac = angleOf(current, target)
//            val ad = ac - av
            val af = 2 * av - ac
            val message = """
                vector: %.1f/%.1f -> %.1f/%.1f
                target: %.1f/%.1f
                final: %.1f/%.1f
                angle:transform: %.4f
                angle:current: %.4f
                angle:final: %.4f
                ft_t: %.12f
            """.trimIndent()
            println(String.format(message, vector.start.x, vector.start.y, vector.finish.x, vector.finish.y, target.x, target.y, f.x, f.y, at, angle, af, ft_t)) // todo
            vector to f
//            vector to getCorrectedPoint(
//                minDistance = minDistance,
//                target = target,
//                point = vector.getShortestPoint(target = target),
//            )
        }
        val (vector, point) = correctedPoints.filter { (_, point) ->
            !nearest.anyCloserThan(point = point, minDistance = minDistance)
        }.maxByOrNull { (_, point) ->
            distanceOf(current, point)
        } ?: return null
        val av = vector.angle()
        val ac = angleOf(current, target)
        val af = 2 * av - ac
        return point to af
    }

    private fun moveBodies(bodies: List<Body>) {
        val timeDiff = engine.property.time.diff()
        if (bodies.size != 1) TODO("moveBodies($bodies)")
        val (b1) = bodies
        // https://en.wikipedia.org/wiki/Gram is a unit of mass in the International System of Units (SI) equal to one thousandth of a kilogram
        // todo Force Friction 1 Newton https://en.wikipedia.org/wiki/Newton_(unit)
        val Ff = 1.0 / (TimeUnit.SECONDS.toNanos(1) * TimeUnit.SECONDS.toNanos(1))
        val vs = b1.velocity.scalar(TimeUnit.NANOSECONDS)
//        val ps = vs * b1.mass
//        val tf = (ps / Ff).nanoseconds
        val tf = (vs * b1.mass / Ff).nanoseconds
        val tx = kotlin.math.min(timeDiff.inWholeNanoseconds, tf.inWholeNanoseconds).nanoseconds
        //
//        val px = ps - Ff * tx.inWholeNanoseconds
//        val px = vs * b1.mass - Ff * tx.inWholeNanoseconds
//        val vx = px / b1.mass
        val vx = vs - Ff * tx.inWholeNanoseconds / b1.mass
        val s = (vs + vx) * tx.inWholeNanoseconds / 2
//        b1.point.move(length = s, angle = b1.velocity.angle())
        val finalPoint = getFinalPoint(
            current = b1.point,
            target = b1.point.moved(length = s, angle = b1.velocity.angle()),
            minDistance = 1.0,
            vectors = env.walls,
            angle = b1.velocity.angle(),
        )
        if (finalPoint == null) {
            b1.velocity.clear()
        } else {
            b1.point.set(finalPoint.first)
            b1.velocity.set(magnitude = vx, TimeUnit.NANOSECONDS, angle = finalPoint.second)
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
                pointCenter = body.point,
                radius = 1.0,
                lineWidth = 0.1,
                edgeCount = 16,
                offset = offset,
                measure = measure,
            )
            canvas.vectors.draw(
                color = Color.GRAY.copy(alpha = 0.5f),
                vector = body.point + body.point.moved(body.velocity.scalar(TimeUnit.SECONDS), body.velocity.angle()),
                lineWidth = 0.05,
                offset = offset,
                measure = measure,
            )
            canvas.vectors.draw(
                color = Color.YELLOW,
                vector = body.point + body.point.moved(length = 1.0, angle = angleOf(env.camera.point, body.point)),
                lineWidth = 0.1,
                offset = offset,
                measure = measure,
            )
            canvas.texts.draw(
                color = Color.BLUE,
                info = info,
                pointTopLeft = body.point,
                offset = offset + offsetOf(dX = -0.5, dY = -0.5),
                measure = measure,
                text = String.format("%02d", index),
            )
        }
    }

    private fun onRenderWalls(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
        walls: List<Vector>,
    ) {
        for (wall in walls) {
            canvas.vectors.draw(
                color = Color.GRAY,
                vector = wall,
                lineWidth = 0.1,
                offset = offset,
                measure = measure,
            )
        }
    }

    private fun onRenderCamera(
        canvas: Canvas,
        offset: Offset,
        measure: Measure<Double, Double>,
    ) {
        canvas.vectors.draw(
            color = Color.WHITE.copy(alpha = 0.5f),
            vector = vectorOf(-0.25, 0.0, 0.25, 0.0),
            offset = offset,
            measure = measure,
            lineWidth = 0.05,
        )
        canvas.vectors.draw(
            color = Color.WHITE.copy(alpha = 0.5f),
            vector = vectorOf(0.0, -0.25, 0.0, 0.25),
            offset = offset,
            measure = measure,
            lineWidth = 0.05,
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
                    body.point.x,
                    body.point.y,
                ),
                measure = measure,
            )
            // todo Force Friction 1 Newton https://en.wikipedia.org/wiki/Newton_(unit)
            val Ff = 1.0 / (TimeUnit.SECONDS.toNanos(1) * TimeUnit.SECONDS.toNanos(1))
            listOf(
                body.velocity.scalar(TimeUnit.SECONDS) to "speed: %.1f per seconds",
                body.velocity.scalar(TimeUnit.NANOSECONDS) to "speed: %.12f per ns",
                body.velocity.angle() to "direction: %.1f",
                Ff to "Ff: %.32f",
            ).forEach { (value: Double, format: String) ->
                canvas.texts.draw(
                    color = Color.GREEN,
                    info = info,
                    pointTopLeft = pointOf(
                        x = 4.0,
                        y = 2.0 + y++,
                    ),
                    text = String.format("    $format", value),
                    measure = measure,
                )
            }
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
        onRenderWalls(
            canvas = canvas,
            offset = offset,
            measure = env.measure,
            walls = env.walls,
        )
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
            val measure = MutableDoubleMeasure(16.0)
//            val measure = MutableDoubleMeasure(24.0)
            val camera = MutableMoving(
                point = MutablePoint(x = 0.0, y = 0.0),
                speed = MutableSpeed(magnitude = 12.0, timeUnit = TimeUnit.SECONDS),
                direction = 0.0,
            )
            val bodies = listOf(
                Body(
                    point = MutablePoint(x = 0.0, y = 0.0),
//                    velocity = MutableVelocity(
//                        magnitude = 8.0,
//                        timeUnit = TimeUnit.SECONDS,
//                        angle = kotlin.math.PI / 4,
//                    ),
                    velocity = MutableVelocity(
                        magnitude = 0.0,
                        timeUnit = TimeUnit.SECONDS,
                    ),
                    mass = 1.0,
                ),
//                Body(
//                    point = MutablePoint(x = 4.0, y = 0.0),
//                    velocity = MutableVelocity(
//                        magnitude = 0.0,
//                        timeUnit = TimeUnit.SECONDS,
//                    ),
//                    mass = 1.0,
//                ),
            )
            val dc = Dot(
//                point = pointOf(2, 4),
                point = pointOf(9.0, 2.5),
                color = Color.WHITE,
            )
            val line = Line(
//                vector = vectorOf(startX = -2, startY = -4, finishX = -2, finishY = 4),
//                vector = vectorOf(startX = 10, startY = -10, finishX = 10, finishY = 10),
                vector = vectorOf(startX = 10, startY = 10, finishX = 10, finishY = -10),
                color = Color.WHITE,
                width = 0.1,
            )
            val dcp = Dot(
                point = line.vector.getPerpendicular(dc.point),
                color = Color.GRAY,
            )
            val minDistance = 1.0
            val dcf = Dot(
                point = dcp.point.moved(length = minDistance, angle = angleOf(dcp.point, dc.point)),
                color = Color.GRAY,
            )
            val ac = -0.7854
            val dt = Dot(
                point = dc.point.moved(length = 0.5, angle = ac),
                color = Color.RED,
            )
            val dtp = Dot(
                point = line.vector.getPerpendicular(dt.point),
                color = Color.GRAY,
            )
            val dtf = Dot(
                point = dtp.point.moved(length = minDistance, angle = angleOf(dcp.point, dc.point)),
                color = Color.WHITE,
            )
            val df = Dot(
                point = dtf.point.moved(length = distanceOf(dt.point, dtf.point), angle = angleOf(dcp.point, dc.point)),
                color = Color.GREEN,
            )
            val av = line.vector.angle()
//            val ac = angleOf(dc.point, dt.point)
            val ad = ac - av
//            val af = av - ad
            val af = 2 * av - ac
            val lines = listOf(
                line,
                Line(
                    vector = dc.point + dt.point,
                    color = Color.GRAY,
                    width = 0.05,
                ),
                Line(
                    vector = dt.point + dt.point.moved(length = 8.0, angle = af),
                    color = Color.YELLOW.copy(alpha = 0.5f),
                    width = 0.05,
                ),
                Line(
                    vector = df.point + df.point.moved(length = 8.0, angle = af + kotlin.math.PI),
                    color = Color.YELLOW.copy(alpha = 0.5f),
                    width = 0.05,
                ),
                Line(
                    vector = dtf.point + dcf.point,
                    color = Color.GRAY.copy(alpha = 0.5f),
                    width = 0.05,
                ),
            )
            val dots = listOf(
                dc,
                dcp,
                dcf,
                dt,
                dtp,
                dtf,
                df,
                Dot(
                    point = line.vector.start,
                    color = Color.WHITE,
                ),
                Dot(
                    point = line.vector.finish,
                    color = Color.WHITE,
                ),
            )
            val message = """
                av: %.4f
                ac: %.4f
                ad: %.4f
                af: %.4f
            """.trimIndent()
            println(String.format(message, av, ac, ad, af)) // todo
            val walls = listOf(
                pointOf(-10, -10) + pointOf(-10, 10),
                pointOf(-10, 10) + pointOf(10, 10),
                pointOf(10, 10) + pointOf(10, -10),
                pointOf(10, -10) + pointOf(-10, -10),
            )
            return Environment(
                measure = measure,
                camera = camera,
//                bodies = emptyList(),
                bodies = bodies,
                paused = true,
                debug = false,
                lines = emptyList(),
//                lines = lines,
                circles = emptyList(),
//                circles = circles,
                dots = emptyList(),
//                dots = dots,
//                walls = emptyList(),
                walls = walls,
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
