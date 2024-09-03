package test.engine.collisions.entity

import test.engine.collisions.MutableAcceleration

internal class Body(
    val moving: MutableMoving,
    val acceleration: MutableAcceleration,
    val mass: Double,
)
