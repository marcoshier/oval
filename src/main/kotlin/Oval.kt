import ddf.minim.AudioInput
import ddf.minim.Minim
import org.openrndr.Extension
import org.openrndr.KEY_LEFT_SHIFT
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.extra.color.presets.*
import org.openrndr.extra.compositor.Composite
import org.openrndr.extra.compositor.Compositor
import org.openrndr.extra.compositor.Layer
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.midi.*
import org.openrndr.extra.minim.minim
import org.openrndr.extra.shapes.grid
import org.openrndr.math.Vector2


// Openrndr Visual Audio Loop-station

class Oval: Extension {

    override var enabled = true
    var debug = true
    private var fps = 60

    var recording = false
        private set


    private lateinit var minim: Minim

    private val comp = Composite()

    val lineIn: AudioInput
        get() = minim.lineIn

    var midi = mutableListOf<Pair<String, MidiTransceiver>>()


    var bpm = 120

    private val excludedMidiDevices = listOf(
        "Gervill",
        null
    )
    private fun excludeDevice(d: MidiDeviceDescription): Boolean {
        return !excludedMidiDevices.contains(d.name) &&
                !excludedMidiDevices.contains(d.vendor) && d.transmit && d.receive
    }

    override fun setup(program: Program) {
        stepFunctions.clear()
        barFunctions.clear()

        program.run {
            minim = minim()

            val devices = mutableMapOf<String, MidiTransceiver>()

            listMidiDevices().filter { excludeDevice(it) }.forEach {
                val d = openMidiDeviceOrNull(it.name)
                if (d != null) devices[it.name] = d
            }

            minim.lineIn.enableMonitoring()
        }

        setupListeners(program)

    }


    val barPulse = Event<Int>("bar-pulse")
    val stepPulse = Event<Int>("step-pulse")

    var playhead: Double = 0.0
        set(value) {
            field = value.mod(1.0)
        }

    var currentBar = 0
        private set (value) {
            if (field != value) {
                field = value
                barPulse.trigger(value)
            }
        }
    var currentStep = 0
        private set (value) {
            if (field != value) {
                field = value
                if (value % steps == 0) currentBar = (currentBar + 1).mod(bars)
                stepPulse.trigger(value)
            }
        }

    var bars = 4
    var steps = 4

    private fun tick(program: Program) {
        // bpm!!!!!
        val unmapped = program.frameCount * (bpm / fps.toDouble())
        playhead = (unmapped / (bpm.toDouble() * bars * steps)).mod(1.0)


        currentStep = (playhead * bars * steps).toInt().mod(bars * steps)

    }


    data class OvalFunction(var enabled: Boolean = false, val function: () -> Unit)

    private var stepFunctions = mutableListOf<Unit>()

    fun step(function: () -> Unit): Layer {
        stepFunctions.add(function())
        return comp.layer {
            function()
        }
    }
    fun step(divider: Int = 1, function: () -> Unit): Layer {
        stepFunctions.add(function())
        return comp.layer {
            function()
        }
    }
    fun step(divider: (steps: Int) -> Int = { 1 }, function: () -> Unit): Layer {
        stepFunctions.add(function())
        return comp.layer {
            function()
        }
    }


    private var barFunctions = mutableListOf<Unit>()

    fun bar(function: () -> Unit) {
        barFunctions.add(function())
    }
    fun bar(divider: Int = 1, function: () -> Unit) {
        barFunctions.add(function())
    }
    fun bar(divider: (steps: Int) -> Int = { 1 }, function: () -> Unit) {
        barFunctions.add(function())
    }

    fun setupListeners(program: Program) {

        barPulse.listen {

        }

        stepPulse.listen {

        }



        program.keyboard.keyRepeat.listen {
            if (it.key == KEY_LEFT_SHIFT) {
                recording = true
            }
        }

        program.keyboard.keyUp.listen {
            if (it.key == KEY_LEFT_SHIFT) {
                recording = false
            }
        }

    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        // all the ticker and
        // audio-related logic

        tick(program)
    }


    override fun afterDraw(drawer: Drawer, program: Program) {

        drawer.isolated {
            comp.draw(drawer)
        }

        if (debug) {
            val r = drawer.bounds.offsetEdges(-10.0).scaledBy(1.0, 0.3, 0.5, 1.0)

            drawer.isolated {
                drawer.stroke = ColorRGBa.WHITE
                drawer.fill = ColorRGBa.TRANSPARENT
                drawer.strokeWeight = 1.0

                drawer.rectangles{
                    for ((i, re) in r.grid(bars, 1).flatten().withIndex()) {
                        this.fill = if (i == currentBar) ColorRGBa.LIGHT_SLATE_GRAY else ColorRGBa.DIM_GRAY.shade(0.4)
                        this.rectangle(re)
                    }
                }

                drawer.strokeWeight = 0.1
                drawer.fill = null
                drawer.rectangles{
                    for ((i, re) in r.grid(bars * steps, 1).flatten().withIndex()) {
                        this.fill = if (i == currentStep) ColorRGBa.MEDIUM_SPRING_GREEN else ColorRGBa.TRANSPARENT
                        this.rectangle(re)
                    }
                }

                drawer.strokeWeight = 2.0
                drawer.stroke = ColorRGBa.MEDIUM_PURPLE
                val x = r.x + (r.width * playhead)
                drawer.lineSegment(x, r.y, x, r.y + r.height)

                drawer.fill = ColorRGBa.MEDIUM_SPRING_GREEN
                drawer.stroke = null
                drawer.text("" +
                        "B: $currentBar, " +
                        "S: $currentStep ", r.corner + Vector2(0.0, -5.0))
            }
        }
    }


    // compositor functions

    fun layer(function: Layer.() -> Unit): Layer {
        comp.function()
        return comp
    }

}

operator fun List<Pair<String, MidiTransceiver>>.get(str: String): MidiTransceiver? {
    val candidate = firstOrNull { it.first.contains(str) }
    if (candidate == null) println("W : no device found for $str") else println("device found! ${candidate.first}")
    return candidate?.second
}