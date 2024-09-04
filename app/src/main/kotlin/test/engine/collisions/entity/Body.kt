package test.engine.collisions.entity

import sp.kx.math.MutablePoint
import sp.kx.math.Vector
import test.engine.collisions.MutableMomentum
import test.engine.collisions.MutableVelocity

internal class Body(
    val point: MutablePoint,
    val velocity: MutableVelocity,
    // https://en.wikipedia.org/wiki/Gram is a unit of mass in the International System of Units (SI) equal to one thousandth of a kilogram
    val mass: Double, // in kilograms
)
