package test.engine.collisions

import sp.kx.math.measure.MutableDoubleMeasure
import test.engine.collisions.entity.Body
import test.engine.collisions.entity.Circle
import test.engine.collisions.entity.Dot
import test.engine.collisions.entity.Line
import test.engine.collisions.entity.MutableMoving

internal class Environment(
    val measure: MutableDoubleMeasure,
    val camera: MutableMoving,
    val bodies: List<Body>,
    var paused: Boolean,
    var debug: Boolean,
    val lines: List<Line>,
    val circles: List<Circle>,
    val dots: List<Dot>,
)
