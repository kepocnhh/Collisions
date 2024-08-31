package test.engine.collisions.util

import sp.kx.lwjgl.entity.font.FontInfo
import sp.kx.math.measure.Measure
import java.io.InputStream

internal object FontInfoUtil {
    private val map = mutableMapOf<String, FontInfo>()

    fun getFontInfo(name: String = "JetBrainsMono.ttf", height: Double, measure: Measure<Double, Double>): FontInfo {
        return getFontInfo(name = name, height = measure.transform(height).toFloat())
    }

    fun getFontInfo(name: String = "JetBrainsMono.ttf", height: Float): FontInfo {
        val id = "${name}_${height}"
        return map.getOrPut(id) {
            object : FontInfo {
                override val id: String = id
                override val height: Float = height.toInt().toFloat() // todo

                override fun getInputStream(): InputStream {
                    return Thread.currentThread().contextClassLoader.getResourceAsStream(name)
                        ?: error("Resource by path \"$name\" does not exist!")
                }
            }
        }
    }
}
