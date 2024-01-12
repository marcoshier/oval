import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.HashBlur
import org.openrndr.extra.olive.OliveScriptHost
import org.openrndr.extra.olive.Once
import org.openrndr.extra.olive.oliveProgram
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 800
        height = 800

    }
    oliveProgram(scriptHost = OliveScriptHost.JSR223) {

        val o by Once { Oval() }

        val g = o.midi["what"]

        extend(o) {

            layer {

                bar {
                    println("bar")
                    draw {
                        drawer.clear(ColorRGBa.GRAY)
                        drawer.fill = ColorRGBa.WHITE
                        for (i in 0 until 100) {
                            drawer.circle(
                                width / 2.0 + cos(seconds + i) * 320.0,
                                i * 7.2,
                                cos(i + seconds * 0.5) * 20.0 + 20.0
                            )
                        }
                    }

                }
                post(HashBlur())
            }



            step(8) {

            }

            step(
                { steps -> sin(steps * 1.0).toInt() }
            ) {

            }
        }
        extend {



        }
    }
}