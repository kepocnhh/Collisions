package test.engine.collisions

import org.lwjgl.opengl.GL11
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.PolygonDrawer
import sp.kx.lwjgl.opengl.GLUtil
import sp.kx.math.MutableOffset
import sp.kx.math.MutablePoint
import sp.kx.math.MutableVector
import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Size
import sp.kx.math.Vector
import sp.kx.math.add
import sp.kx.math.angle
import sp.kx.math.angleOf
import sp.kx.math.distanceOf
import sp.kx.math.getShortestDistance
import sp.kx.math.isEmpty
import sp.kx.math.length
import sp.kx.math.lt
import sp.kx.math.measure.Measure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.measure.Speed
import sp.kx.math.measure.speedOf
import sp.kx.math.mut
import sp.kx.math.offsetOf
import sp.kx.math.plus
import sp.kx.math.plusAssign
import sp.kx.math.pointOf
import sp.kx.math.sizeOf
import sp.kx.math.toOffset
import sp.kx.math.toString
import sp.kx.math.toVector
import sp.kx.math.vectorOf
import test.engine.collisions.entity.Moving
import test.engine.collisions.entity.MutableMoving
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Deprecated("sp.kx.math.measure.Acceleration")
internal interface Acceleration {
    fun per(timeUnit: TimeUnit): Double
    fun speed(duration: Duration, timeUnit: TimeUnit): Double
}

@Deprecated("sp.kx.math.measure.MutableAcceleration")
internal class MutableAcceleration : Acceleration {
    private var raw: Double

    constructor(magnitude: Double, timeUnit: TimeUnit) {
        val nanos = timeUnit.toNanos(1)
        raw = magnitude / (nanos * nanos)
    }

    override fun per(timeUnit: TimeUnit): Double {
        val nanos = timeUnit.toNanos(1)
        return raw * nanos * nanos
    }

    override fun speed(duration: Duration, timeUnit: TimeUnit): Double {
        val magnitude = raw * duration.inWholeNanoseconds // units per nanosec
        return magnitude / timeUnit.toNanos(1)
    }

    override fun toString(): String {
        return toString(timeUnit = TimeUnit.SECONDS)
    }

    fun toString(timeUnit: TimeUnit): String {
        val magnitude = per(timeUnit = timeUnit)
        return "{${magnitude.toString(points = 12)}/${timeUnit}^2}"
    }

    fun set(magnitude: Double, timeUnit: TimeUnit) {
        val nanos = timeUnit.toNanos(1)
        raw = magnitude / (nanos * nanos)
    }
}

@Deprecated("sp.kx.math.measure.MutableSpeed.set")
internal fun MutableSpeed.set(acceleration: Acceleration, duration: Duration) {
    val v = per(TimeUnit.NANOSECONDS)
    val d = acceleration.speed(duration, TimeUnit.NANOSECONDS)
    set(v + d, TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.math.measure.MutableSpeed.clear")
internal fun MutableSpeed.clear() {
    set(0.0, TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.math.measure.MutableAcceleration.clear")
internal fun MutableAcceleration.clear() {
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

@Deprecated("sp.kx.math.measure.mut")
internal fun Speed.mut(): MutableSpeed {
    return MutableSpeed(magnitude = per(TimeUnit.NANOSECONDS), timeUnit = TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.math.measure.set")
internal fun MutableSpeed.set(other: Speed) {
    set(magnitude = other.per(TimeUnit.NANOSECONDS), timeUnit = TimeUnit.NANOSECONDS)
}

@Deprecated("sp.kx.math")
internal fun MutableMoving.set(other: Moving) {
    point.set(other.point)
    speed.set(other.speed)
    direction = other.direction
}

@Deprecated("sp.kx.math.measure.time")
internal fun Speed.duration(length: Double): Duration {
    return (length / per(TimeUnit.NANOSECONDS)).nanoseconds
}

@Deprecated("sp.kx.math.measure.set")
internal fun MutableAcceleration.set(other: Acceleration) {
    return set(magnitude = other.per(TimeUnit.NANOSECONDS), timeUnit = TimeUnit.NANOSECONDS)
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

@Deprecated("sp.kx.physics.Velocity")
internal interface Velocity {
    fun scalar(timeUnit: TimeUnit): Double
    fun angle(): Double
    fun isEmpty(): Boolean
}

@Deprecated("sp.kx.physics.MutableVelocity")
internal class MutableVelocity : Velocity {
    private val offset: MutableOffset

    constructor(
        magnitude: Double,
        timeUnit: TimeUnit,
        angle: Double = 0.0, // todo
    ) {
        val vector = vectorOf(
            Point.Center,
            length = magnitude / timeUnit.toNanos(1),
            angle = angle,
        )
        vector.length()
        offset = vector.toOffset().mut()
    }

    override fun scalar(timeUnit: TimeUnit): Double {
        return distanceOf(offset) * timeUnit.toNanos(1)
    }

    override fun angle(): Double {
        return angleOf(offset)
    }

    fun clear() {
        offset.set(dX = 0.0, dY = 0.0)
    }

    fun set(
        magnitude: Double,
        timeUnit: TimeUnit,
        angle: Double = angle(),
    ) {
        val vector = vectorOf(
            Point.Center,
            length = magnitude / timeUnit.toNanos(1),
            angle = angle,
        )
        offset.set(vector.toOffset())
    }

    fun add(
        magnitude: Double,
        timeUnit: TimeUnit,
        angle: Double = angle(),
    ) {
        val vector = vectorOf(
            Point.Center,
            length = magnitude / timeUnit.toNanos(1),
            angle = angle,
        )
        offset += vector.toOffset()
    }

    fun add(
        dX: Double,
        dY: Double,
        timeUnit: TimeUnit,
    ) {
        offset.add(dX = dX / timeUnit.toNanos(1), dY = dY / timeUnit.toNanos(1))
    }

    fun set(other: Velocity) {
        val vector = vectorOf(
            Point.Center,
            length = other.scalar(TimeUnit.NANOSECONDS),
            angle = other.angle(),
        )
        offset.set(vector.toOffset())
    }

    fun set(
        dX: Double,
        dY: Double,
        timeUnit: TimeUnit,
    ) {
        offset.set(dX = dX / timeUnit.toNanos(1), dY = dY / timeUnit.toNanos(1))
    }

    override fun isEmpty(): Boolean {
        return offset.isEmpty()
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

// todo angle vector x perpendicular
// todo angle vector x vector
