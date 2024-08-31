package test.engine.collisions.entity

import sp.kx.math.measure.MutableSpeed

internal class Body(
    val moving: MutableMoving,
    val acceleration: MutableSpeed,
    var direction: Double,
    val mass: Double,
)
