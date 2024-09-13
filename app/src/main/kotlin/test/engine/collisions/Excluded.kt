package test.engine.collisions

import org.lwjgl.opengl.GL11
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.PolygonDrawer
import sp.kx.lwjgl.opengl.GLUtil
import sp.kx.math.MutableOffset
import sp.kx.math.MutableVector
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Vector
import sp.kx.math.angle
import sp.kx.math.angleOf
import sp.kx.math.getShortestDistance
import sp.kx.math.length
import sp.kx.math.lt
import sp.kx.math.measure.Measure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.measure.Speed
import sp.kx.math.mut
import sp.kx.math.plus
import sp.kx.math.toString
import sp.kx.math.vectorOf
import sp.kx.physics.Acceleration
import sp.kx.physics.MutableAcceleration
import test.engine.collisions.entity.Moving
import test.engine.collisions.entity.MutableMoving
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

@Deprecated("sp.kx.physics.MutableSpeed.set")
internal fun MutableSpeed.set(acceleration: Acceleration, duration: Duration) {
    val v = per(TimeUnit.NANOSECONDS)
    val d = acceleration.speed(timeUnit = TimeUnit.NANOSECONDS, duration = duration)
    set(v + d, TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.physics.MutableSpeed.clear")
internal fun MutableSpeed.clear() {
    set(0.0, TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.math.drawCircle")
internal fun PolygonDrawer.drawCircle(
    color: Color,
    pointCenter: Point,
    radius: Double,
    edgeCount: Int,
    offset: Offset,
    measure: Measure<Double, Double>,
) {
    val points = (0..edgeCount).map {
        val radians = it * 2 * kotlin.math.PI / edgeCount
        pointCenter.plus(
            dX = kotlin.math.cos(radians) * radius,
            dY = kotlin.math.sin(radians) * radius,
        )
    }
    GL11.glLineWidth(1f)
    GLUtil.colorOf(color)
    GLUtil.transaction(GL11.GL_POLYGON) {
        points.forEach {
            GLUtil.vertexOf(it, offset = offset, measure = measure)
        }
    }
}

private fun vertexOf(
    start: Point,
    finish: Point,
    lineWidth: Double,
    offset: Offset,
    measure: Measure<Double, Double>,
) {
    val angle = angleOf(start, finish)
    GLUtil.vertexOfMoved(start, length = lineWidth / 2, angle = angle - kotlin.math.PI / 2, offset = offset, measure = measure)
    GLUtil.vertexOfMoved(start, length = lineWidth / 2, angle = angle + kotlin.math.PI / 2, offset = offset, measure = measure)
    GLUtil.vertexOfMoved(finish, length = lineWidth / 2, angle = angle - kotlin.math.PI / 2, offset = offset, measure = measure)
    GLUtil.vertexOfMoved(finish, length = lineWidth / 2, angle = angle + kotlin.math.PI / 2, offset = offset, measure = measure)
}

@Deprecated("sp.kx.math.drawCircle")
internal fun PolygonDrawer.drawCircle(
    color: Color,
    pointCenter: Point,
    radius: Double,
    edgeCount: Int,
    lineWidth: Double,
    offset: Offset,
    measure: Measure<Double, Double>,
) {
    val points = (0..edgeCount).map {
        val radians = it * 2 * kotlin.math.PI / edgeCount
        pointCenter.plus(
            dX = kotlin.math.cos(radians) * radius,
            dY = kotlin.math.sin(radians) * radius,
        )
    }
    GL11.glLineWidth(1f)
    GLUtil.colorOf(color)
    GLUtil.transaction(GL11.GL_TRIANGLE_STRIP) {
        val fStart = points[0]
        val fFinish = points[1]
        vertexOf(start = fStart, finish = fFinish, lineWidth = lineWidth, offset = offset, measure = measure)
        for (i in 2 until points.size) {
            vertexOf(start = points[i - 1], finish = points[i], lineWidth = lineWidth, offset = offset, measure = measure)
        }
        GLUtil.vertexOfMoved(fStart, length = lineWidth / 2, angle = angleOf(fStart, fFinish) - kotlin.math.PI / 2, offset = offset, measure = measure)
    }
}

@Deprecated("sp.kx.physics.mut")
internal fun Speed.mut(): MutableSpeed {
    return MutableSpeed(magnitude = per(TimeUnit.NANOSECONDS), timeUnit = TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.physics.MutableSpeed.set")
internal fun MutableSpeed.set(other: Speed) {
    set(magnitude = other.per(TimeUnit.NANOSECONDS), timeUnit = TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.math")
internal fun MutableMoving.set(other: Moving) {
    point.set(other.point)
    speed.set(other.speed)
    direction = other.direction
}

@Deprecated("sp.kx.physics.Speed.duration")
internal fun Speed.duration(length: Double): Duration {
    return (length / per(TimeUnit.NANOSECONDS)).nanoseconds
}

@Deprecated("sp.kx.math.measure.Momentum")
internal interface Momentum {
    fun scalar(timeUnit: TimeUnit): Double
    fun speed(timeUnit: TimeUnit, mass: Double): Double
    fun angle(): Double
}

@Deprecated("sp.kx.math.measure.MutableMomentum")
internal class MutableMomentum : Momentum {
    private val vector: MutableVector

    constructor(
        speed: Double,
        timeUnit: TimeUnit,
        mass: Double = 1.0, // todo
        angle: Double = 0.0, // todo
    ) {
        this.vector = vectorOf(
            Point.Center,
            length = mass * speed / timeUnit.toNanos(1),
            angle = angle,
        ).mut()
    }

    constructor(
        magnitude: Double,
        timeUnit: TimeUnit,
        angle: Double = 0.0, // todo
    ) {
        this.vector = vectorOf(
            Point.Center,
            length = magnitude / timeUnit.toNanos(1),
            angle = angle,
        ).mut()
    }

    override fun scalar(timeUnit: TimeUnit): Double {
        return vector.length() * timeUnit.toNanos(1)
    }

    override fun speed(timeUnit: TimeUnit, mass: Double): Double {
        return vector.length() * timeUnit.toNanos(1) / mass
    }

    override fun angle(): Double {
        return vector.angle()
    }

    fun set(
        magnitude: Double,
        timeUnit: TimeUnit,
        angle: Double = angle(),
    ) {
        vector.set(
            other = vectorOf(
                Point.Center,
                length = magnitude / timeUnit.toNanos(1),
                angle = angle,
            ),
        )
    }

    fun set(other: Momentum) {
        vector.set(
            other = vectorOf(
                Point.Center,
                length = other.scalar(TimeUnit.NANOSECONDS) / TimeUnit.NANOSECONDS.toNanos(1),
                angle = other.angle(),
            ),
        )
    }
}

@Deprecated(message = "sp.kx.math.closerThan")
internal fun Vector.closerThan(point: Point, minDistance: Double): Boolean {
    return getShortestDistance(point).lt(other = minDistance, points = 12)
}

@Deprecated(message = "sp.kx.math.closerThan")
internal fun Iterable<Vector>.anyCloserThan(point: Point, minDistance: Double): Boolean {
    for (vector in this) {
        if (vector.closerThan(point = point, minDistance = minDistance)) return true
    }
    return false
}

@Deprecated(message = "sp.kx.math.clear")
internal fun MutableOffset.clear() {
    dX = 0.0
    dY = 0.0
}

// todo angle vector x perpendicular
// todo angle vector x vector
