package test.engine.collisions.entity

import sp.kx.math.MutablePoint
import sp.kx.math.measure.MutableSpeed

internal class MutableMoving(
    override val point: MutablePoint,
    override val speed: MutableSpeed,
) : Moving
