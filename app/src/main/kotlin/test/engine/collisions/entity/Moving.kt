package test.engine.collisions.entity

import sp.kx.math.Point
import sp.kx.math.measure.Speed

internal interface Moving {
    val point: Point
    val speed: Speed
    val direction: Double
}
