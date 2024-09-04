package test.engine.collisions.entity

import sp.kx.math.MutablePoint
import sp.kx.math.Vector
import test.engine.collisions.MutableMomentum

internal class Body(
    val point: MutablePoint,
    val momentum: MutableMomentum,
    val mass: Double,
)
