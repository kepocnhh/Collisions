package test.engine.collisions

import sp.kx.lwjgl.engine.Engine
import sp.kx.lwjgl.engine.EngineInputCallback
import sp.kx.lwjgl.engine.EngineLogic
import sp.kx.lwjgl.engine.input.Keyboard
import sp.kx.lwjgl.entity.Canvas
import sp.kx.lwjgl.entity.Color
import sp.kx.lwjgl.entity.input.KeyboardButton
import sp.kx.math.Point
import sp.kx.math.measure.frequency
import sp.kx.math.measure.measureOf
import sp.kx.math.pointOf
import sp.kx.math.sizeOf
import test.engine.collisions.util.FontInfoUtil

internal class InputEngineLogic(private val engine: Engine) : EngineLogic {
    private lateinit var shouldEngineStopUnit: Unit

    override val inputCallback = object : EngineInputCallback {
        override fun onKeyboardButton(button: KeyboardButton, isPressed: Boolean) {
            when (button) {
                KeyboardButton.ESCAPE -> {
                    if (!isPressed) {
                        shouldEngineStopUnit = Unit
                    }
                }
                else -> {
                    println("[InputEngineLogic]: on button: $button $isPressed")
                }
            }
        }
    }

    private val measure = measureOf(24.0)

    private fun onRenderKeyboard(canvas: Canvas, x: Double, y: Double, keyboard: Keyboard) {
        val width = 1.0
        setOf(
            setOf(KeyboardButton.Q, KeyboardButton.W, KeyboardButton.E, KeyboardButton.R, KeyboardButton.T, KeyboardButton.Y, KeyboardButton.U, KeyboardButton.I, KeyboardButton.O, KeyboardButton.P),
            setOf(KeyboardButton.A, KeyboardButton.S, KeyboardButton.D, KeyboardButton.F, KeyboardButton.G, KeyboardButton.H, KeyboardButton.J, KeyboardButton.K, KeyboardButton.L),
            setOf(KeyboardButton.Z, KeyboardButton.X, KeyboardButton.C, KeyboardButton.V, KeyboardButton.B, KeyboardButton.N, KeyboardButton.M)
        ).forEachIndexed { dY, row ->
            row.forEachIndexed { dX, button ->
                val isPressed = keyboard.isPressed(button)
                val pointTopLeft = pointOf(x + width * dX, y + width * dY)
                canvas.texts.draw(
                    info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
                    color = if (isPressed) Color.YELLOW else Color.GREEN,
                    pointTopLeft = pointTopLeft,
                    text = button.name,
                    measure = measure,
                )
                if (isPressed) {
                    canvas.polygons.drawRectangle(
                        color = Color.YELLOW,
                        pointTopLeft = pointOf(x + width * dX - width * 0.25, y + width * dY),
                        size = sizeOf(width = width, height = width),
                        lineWidth = 0.1,
                        measure = measure,
                    )
                }
            }
        }
    }

    override fun onRender(canvas: Canvas) {
        val fps = engine.property.time.frequency()
        canvas.texts.draw(
            info = FontInfoUtil.getFontInfo(height = 1.0, measure = measure),
            pointTopLeft = Point.Center,
            color = Color.GREEN,
            text = String.format("%.2f", fps),
            measure = measure,
        )
        onRenderKeyboard(canvas = canvas, x = 1.0, y = 1.0, engine.input.keyboard)
    }

    override fun shouldEngineStop(): Boolean {
        return ::shouldEngineStopUnit.isInitialized
    }
}
