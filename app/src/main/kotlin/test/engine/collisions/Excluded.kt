package test.engine.collisions

import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Size
import sp.kx.math.measure.Measure
import sp.kx.math.measure.MutableSpeed
import sp.kx.math.measure.Speed
import sp.kx.math.measure.speedOf
import sp.kx.math.offsetOf
import sp.kx.math.pointOf
import sp.kx.math.sizeOf
import sp.kx.math.toString
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Deprecated(message = "sp.kx.math.minus")
internal operator fun Size.minus(
    measure: Measure<Double, Double>,
): Size {
    return sizeOf(
        width = measure.units(width),
        height = measure.units(height),
    )
}

@Deprecated(message = "sp.kx.math.minus")
internal operator fun Point.minus(measure: Measure<Double, Double>): Point {
    return pointOf(
        x = measure.units(x),
        y = measure.units(y),
    )
}

@Deprecated(message = "sp.kx.math.minus")
internal operator fun Offset.minus(measure: Measure<Double, Double>): Offset {
    return offsetOf(
        dX = measure.units(dX),
        dY = measure.units(dY),
    )
}

@Deprecated("sp.kx.math.plus")
internal operator fun Offset.plus(other: Offset): Offset {
    return offsetOf(
        dX = dX + other.dX,
        dY = dY + other.dY,
    )
}

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
