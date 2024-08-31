package test.engine.collisions

import sp.kx.math.measure.MutableDoubleMeasure
import test.engine.collisions.entity.MutableMoving

internal class Environment(
    val measure: MutableDoubleMeasure,
    val camera: MutableMoving,
)
