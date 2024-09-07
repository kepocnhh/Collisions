package test.engine.collisions.entity

import sp.kx.math.MutablePoint
import sp.kx.physics.MutableVelocity

internal class Body(
    val point: MutablePoint,
    val velocity: MutableVelocity,
    // https://en.wikipedia.org/wiki/Gram is a unit of mass in the International System of Units (SI) equal to one thousandth of a kilogram
    val mass: Double, // in kilograms
)
