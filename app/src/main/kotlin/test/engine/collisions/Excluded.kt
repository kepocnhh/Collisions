package test.engine.collisions

import sp.kx.math.Offset
import sp.kx.math.Point
import sp.kx.math.Size
import sp.kx.math.measure.Measure
import sp.kx.math.offsetOf
import sp.kx.math.pointOf
import sp.kx.math.sizeOf

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
