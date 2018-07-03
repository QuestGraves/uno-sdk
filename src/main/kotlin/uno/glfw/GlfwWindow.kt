package uno.glfw

import ab.appBuffer
import glm_.bool
import glm_.f
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import glm_.vec2.Vec2i
import glm_.vec4.Vec4i
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VkInstance
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * Created by GBarbieri on 24.04.2017.
 */


open class GlfwWindow(var handle: Long) {

    constructor(windowSize: Vec2i,
                title: String,
                monitor: Long = NULL,
                position: Vec2i = Vec2i(Int.MIN_VALUE),
                installCallbacks: Boolean = true) : this(windowSize.x, windowSize.y, title, monitor, position, installCallbacks)

    constructor(x: Int,
                title: String,
                monitor: Long = NULL,
                position: Vec2i = Vec2i(Int.MIN_VALUE),
                installCallbacks: Boolean = true) : this(x, x, title, monitor, position, installCallbacks)

    constructor(width: Int, height: Int,
                title: String,
                monitor: Long = NULL,
                position: Vec2i = Vec2i(Int.MIN_VALUE),
                installCallbacks: Boolean = true) : this(glfwCreateWindow(width, height, title, monitor, NULL)) {

        this.title = title

        if (position != Vec2i(Int.MIN_VALUE))
            glfwSetWindowPos(handle, position.x, position.y)

        if (installCallbacks) {
            glfwSetCharCallback(handle, nCharCallback)
            glfwSetCursorPosCallback(handle, nCursorPosCallback)
            glfwSetFramebufferSizeCallback(handle, nFramebufferSizeCallback)
            glfwSetKeyCallback(handle, nKeyCallback)
            glfwSetMouseButtonCallback(handle, nMouseButtonCallback)
            glfwSetScrollCallback(handle, nScrollCallback)
            glfwSetWindowCloseCallback(handle, nWindowCloseCallback)
            cursorPosCallback = defaultCursorPosCallback
            framebufferSizeCallback = defaultFramebufferSizeCallback
            keyCallback = defaultKeyCallback
            mouseButtonCallback = defaultMouseButtonCallback
            scrollCallback = defaultScrollCallback
            windowCloseCallback = defaultWindowCloseCallback
        }
    }

    init {
        if (handle == MemoryUtil.NULL) {
            glfw.terminate()
            throw RuntimeException("Failed to create the GLFW window")
        }
    }

    fun init() {
        makeContextCurrent()
        /*  This line is critical for LWJGL's interoperation with GLFW's OpenGL context,
            or any context that is managed externally.
            LWJGL detects the context that is current in the current thread, creates the GLCapabilities instance and
            makes the OpenGL bindings available for use. */
        GL.createCapabilities()
        show()
    }

    val isOpen get() = !shouldClose

    var shouldClose
        get() = glfwWindowShouldClose(handle)
        set(value) = glfwSetWindowShouldClose(handle, value)

    var title: String = ""
        set(value) {
            glfwSetWindowTitle(handle, value)
            field = value
        }

    fun setSizeLimit(width: IntRange, height: IntRange) = glfwSetWindowSizeLimits(handle, width.start, height.start, width.endInclusive, height.endInclusive)

    // TODO icon

    var pos = Vec2i()
        get() {
            val x = appBuffer.int
            val y = appBuffer.int
            nglfwGetWindowPos(handle, x, y)
            return field(memGetInt(x), memGetInt(y))
        }
        set(value) = glfwSetWindowPos(handle, value.x, value.y)

    var size = Vec2i()
        get() {
            val x = appBuffer.int
            val y = appBuffer.int
            nglfwGetWindowSize(handle, x, y)
            return field(memGetInt(x), memGetInt(y))
        }
        set(value) = glfwSetWindowSize(handle, value.x, value.y)

    val aspect get() = size.aspect
//        set(value) = glfwSetWindowAspectRatio(handle, (value * 1_000).i, 1_000)

    var aspectRatio = Vec2i()
        get() = field(size.x, size.y)
        set(value) = glfwSetWindowAspectRatio(handle, value.x, value.y)

    val framebufferSize = Vec2i()
        get() {
            val x = appBuffer.int
            val y = appBuffer.int
            nglfwGetFramebufferSize(handle, x, y)
            return field(memGetInt(x), memGetInt(y))
        }

    val frameSize = Vec4i()
        get() {
            val x = appBuffer.int
            val y = appBuffer.int
            val z = appBuffer.int
            val w = appBuffer.int
            nglfwGetWindowFrameSize(handle, x, y, z, w)
            return field(memGetInt(x), memGetInt(y), memGetInt(z), memGetInt(w))
        }

    fun iconify() = glfwIconifyWindow(handle)
    fun restore() = glfwRestoreWindow(handle)
    fun maximize() = glfwMaximizeWindow(handle)
    fun show(show: Boolean = true) = if (show) glfwShowWindow(handle) else glfwHideWindow(handle)
    fun hide() = glfwHideWindow(handle)
    fun focus() = glfwFocusWindow(handle)

    data class Monitor(val monitor: Long, val xPos: Int, val yPos: Int, val width: Int, val height: Int, val refreshRate: Int = GLFW_DONT_CARE)

    var monitor: Monitor
        get() {
            val monitor = glfwGetWindowMonitor(handle)
            return Monitor(monitor, pos.x, pos.y, size.x, size.y)
        }
        set(value) = glfwSetWindowMonitor(handle, value.monitor, value.xPos, value.yPos, value.width, value.height, value.refreshRate)

    val focused get() = glfwGetWindowAttrib(handle, GLFW_FOCUSED).bool
    val iconified get() = glfwGetWindowAttrib(handle, GLFW_ICONIFIED).bool
    val maximized get() = glfwGetWindowAttrib(handle, GLFW_MAXIMIZED).bool
    val visible get() = glfwGetWindowAttrib(handle, GLFW_VISIBLE).bool
    val resizable get() = glfwGetWindowAttrib(handle, GLFW_RESIZABLE).bool
    val decorated get() = glfwGetWindowAttrib(handle, GLFW_DECORATED).bool
    val floating get() = glfwGetWindowAttrib(handle, GLFW_FLOATING).bool

    fun makeContextCurrent() = glfwMakeContextCurrent(handle)

    fun destroy() {
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(handle)
        glfwDestroyWindow(handle)
        handle = NULL
    }

    var cursorPos = Vec2d()
        get() {
            val x = appBuffer.double
            val y = appBuffer.double
            nglfwGetCursorPos(handle, x, y)
            return field(memGetDouble(x), memGetDouble(y))
        }
        set(value) = glfwSetCursorPos(handle, value.x, value.y)


    var charCallback: CharCallbackT? = null
        set(value) {
            charCallbacks["0 - default"] = value
            field = value
        }

    val charCallbacks = sortedMapOf<String, CharCallbackT>()
    val nCharCallback = GLFWCharCallbackI { _, codePoint -> charCallbacks.values.forEach { it(codePoint) } }


    var cursorPosCallback: CursorPosCallbackT? = null
        set(value) {
            cursorPosCallbacks["0 - default"] = value
            field = value
        }

    val cursorPosCallbacks = sortedMapOf<String, CursorPosCallbackT>()
    val nCursorPosCallback = GLFWCursorPosCallbackI { _, xPos, yPos -> cursorPosCallbacks.values.forEach { it(Vec2(xPos, yPos)) } }


    var framebufferSizeCallback: FramebufferSizeCallbackT? = null
        set(value) {
            framebufferSizeCallbacks["0 - default"] = value
            field = value
        }
    val framebufferSizeCallbacks = sortedMapOf<String, FramebufferSizeCallbackT>()
    val nFramebufferSizeCallback = GLFWFramebufferSizeCallbackI { _, width, height -> framebufferSizeCallbacks.values.forEach { it(Vec2i(width, height)) } }


    var keyCallback: KeyCallbackT? = null
        set(value) {
            keyCallbacks["0 - default"] = value
            field = value
        }
    val keyCallbacks = sortedMapOf<String, KeyCallbackT>()
    val nKeyCallback = GLFWKeyCallbackI { _, key, scanCode, action, mods -> keyCallbacks.values.forEach { it(key, scanCode, action, mods) } }


    var mouseButtonCallback: MouseButtonCallbackT? = null
        set(value) {
            mouseButtonCallbacks["0 - default"] = value
            field = value
        }
    val mouseButtonCallbacks = sortedMapOf<String, MouseButtonCallbackT>()
    val nMouseButtonCallback = GLFWMouseButtonCallbackI { _, button, action, mods -> mouseButtonCallbacks.values.forEach { it(button, action, mods) } }


    var scrollCallback: ScrollCallbackT? = null
        set(value) {
            scrollCallbacks["0 - default"] = value
            field = value
        }
    val scrollCallbacks = sortedMapOf<String, ScrollCallbackT>()
    val nScrollCallback = GLFWScrollCallbackI { _, xOffset, yOffset -> scrollCallbacks.values.forEach { it(Vec2d(xOffset, yOffset)) } }


    var windowCloseCallback: WindowCloseCallbackT? = null
        set(value) {
            windowCloseCallbacks["0 - default"] = value
            field = value
        }
    val windowCloseCallbacks = sortedMapOf<String, WindowCloseCallbackT>()
    val nWindowCloseCallback = GLFWWindowCloseCallbackI { windowCloseCallbacks.values.forEach { it() } }


    val defaultKeyCallback: KeyCallbackT = { key, _, _, mods -> onKeyPressed(key, mods) }
    val defaultMouseButtonCallback: MouseButtonCallbackT = { button, action, mods -> onMouseButtonEvent(button, action, mods) }
    val defaultCursorPosCallback: CursorPosCallbackT = { pos -> onMouseMoved(pos) }
    val defaultScrollCallback: ScrollCallbackT = { scroll -> onMouseScrolled(scroll.y.f) }
    val defaultWindowCloseCallback: WindowCloseCallbackT = ::onWindowClosed
    val defaultFramebufferSizeCallback: FramebufferSizeCallbackT = { size -> onWindowResized(size) }

    //
    // Event handlers are called by the GLFW callback mechanism and should not be called directly
    //

    open fun onWindowResized(newSize: Vec2i) = appBuffer.reset()
    open fun onWindowClosed() {}

    // Keyboard handling
    open fun onKeyEvent(key: Int, scanCode: Int, action: Int, mods: Int) {
        when (action) {
            GLFW_PRESS -> onKeyPressed(key, mods)
            GLFW_RELEASE -> onKeyReleased(key, mods)
        }
    }

    open fun onKeyPressed(key: Int, mods: Int) {}
    open fun onKeyReleased(key: Int, mods: Int) {}

    // Mouse handling
    open fun onMouseButtonEvent(button: Int, action: Int, mods: Int) {
        when (action) {
            GLFW_PRESS -> onMousePressed(button, mods)
            GLFW_RELEASE -> onMouseReleased(button, mods)
        }
    }

    open fun onMousePressed(button: Int, mods: Int) {}
    open fun onMouseReleased(button: Int, mods: Int) {}
    open fun onMouseMoved(newPos: Vec2) {}
    open fun onMouseScrolled(delta: Float) {}


    var cursorStatus: CursorStatus
        get() = when (glfwGetInputMode(handle, GLFW_CURSOR)) {
            GLFW_CURSOR_NORMAL -> CursorStatus.Normal
            GLFW_CURSOR_HIDDEN -> CursorStatus.Hidden
            GLFW_CURSOR_DISABLED -> CursorStatus.Disabled
            else -> throw Error()
        }
        set(value) = glfwSetInputMode(handle, GLFW_CURSOR, when (value) {
            CursorStatus.Normal -> GLFW_CURSOR_NORMAL
            CursorStatus.Hidden -> GLFW_CURSOR_HIDDEN
            CursorStatus.Disabled -> GLFW_CURSOR_DISABLED
        })
    var cursor: Long
        get() = NULL
        set(value) = glfwSetCursor(handle, value)

    enum class CursorStatus { Normal, Hidden, Disabled }

    fun pressed(key: Int) = glfwGetKey(handle, key) == GLFW_PRESS
    fun released(key: Int) = glfwGetKey(handle, key) == GLFW_PRESS

    fun mouseButton(button: Int) = glfwGetMouseButton(handle, button)

    val joystick1Buttons: ByteBuffer?
        get() = getJoystickButtons(GLFW_JOYSTICK_1)
    val joystick2Buttons: ByteBuffer?
        get() = getJoystickButtons(GLFW_JOYSTICK_2)
    val joystick3Buttons: ByteBuffer?
        get() = getJoystickButtons(GLFW_JOYSTICK_3)

    fun getJoystickButtons(joystickId: Int): ByteBuffer? {
        val count = appBuffer.int
        val result = nglfwGetJoystickButtons(joystickId, count)
        return memByteBufferSafe(result, memGetInt(count))
    }

    val joystick1Axes: FloatBuffer?
        get() = getJoystickAxes(GLFW_JOYSTICK_1)
    val joystick2Axes: FloatBuffer?
        get() = getJoystickAxes(GLFW_JOYSTICK_2)
    val joystick3Axes: FloatBuffer?
        get() = getJoystickAxes(GLFW_JOYSTICK_3)

    fun getJoystickAxes(joystickId: Int): FloatBuffer? {
        val count = appBuffer.int
        val result = nglfwGetJoystickAxes(joystickId, count)
        return memFloatBufferSafe(result, memGetInt(count))
    }

    var autoSwap = true

    inline fun loop(block: () -> Unit) = loop( { isOpen }, block)

    inline fun loop(condition: () -> Boolean, block: () -> Unit) {
        while (condition()) {
            glfwPollEvents()
            block()
            if (autoSwap)
                glfwSwapBuffers(handle)
            appBuffer.reset()
        }
    }

    infix fun createSurface(instance: VkInstance) = glfw.createWindowSurface(handle, instance)

    fun present() = glfwSwapBuffers(handle)
    fun swapBuffers() = glfwSwapBuffers(handle)
}

typealias CharCallbackT = (codePoint: Int) -> Unit
typealias CursorPosCallbackT = (pos: Vec2) -> Unit
typealias FramebufferSizeCallbackT = (size: Vec2i) -> Unit
typealias KeyCallbackT = (key: Int, scanCode: Int, action: Int, mods: Int) -> Unit
typealias MouseButtonCallbackT = (button: Int, action: Int, mods: Int) -> Unit
typealias ScrollCallbackT = (scroll: Vec2d) -> Unit
typealias WindowCloseCallbackT = () -> Unit
