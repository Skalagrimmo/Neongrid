package com.example.render

import android.opengl.GLES30
import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

class GlBatchRenderer {

    private val vertexShaderCode = """
        #version 300 es
        layout(location = 0) in vec2 aPosition;
        layout(location = 1) in vec4 aColor; // normalized GL_UNSIGNED_BYTE -> vec4 [0.0..1.0]
        
        uniform mat4 uProjection;
        out vec4 vColor;
        
        void main() {
            vColor = aColor;
            gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #version 300 es
        precision lowp float;
        
        in vec4 vColor;
        out vec4 fragColor;
        
        uniform bool uEnableScanlines;
        uniform bool uEnableCelShading;
        uniform float uCelBands;
        
        void main() {
            vec4 col = vColor;
            
            // Branchless cel-shading calculation
            if (uEnableCelShading && col.a > 0.05) {
                float luma = dot(col.rgb, vec3(0.299, 0.587, 0.114));
                if (luma > 0.02) {
                    float bands = max(uCelBands, 2.0);
                    float quantized = max(floor(luma * bands) / (bands - 1.0), 0.30);
                    col.rgb = clamp(col.rgb * (quantized / luma) * 1.08, 0.0, 1.0);
                }
            }
            
            if (uEnableScanlines) {
                float lineY = mod(gl_FragCoord.y, 6.0);
                if (lineY < 2.0) {
                    col.rgb *= 0.82;
                }
            }
            fragColor = col;
        }
    """.trimIndent()

    private var program: Int = 0
    private var uProjectionLoc: Int = -1
    private var uEnableScanlinesLoc: Int = -1
    private var uEnableCelShadingLoc: Int = -1
    private var uCelBandsLoc: Int = -1

    // VBO / EBO / VAO Configuration
    // 32768 vertices max per batch (fits safely in 16-bit short indices)
    private val maxVertices = 32768
    private val maxIndices = 65536
    private val floatsPerPosition = 2
    private val bytesPerColor = 4 // R, G, B, A normalized unsigned bytes
    private val vertexStrideBytes = floatsPerPosition * 4 + bytesPerColor // 12 bytes per vertex!

    private val vertexByteBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(maxVertices * vertexStrideBytes)
        .order(ByteOrder.nativeOrder())

    private val indexShortBuffer: ShortBuffer = ByteBuffer
        .allocateDirect(maxIndices * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()

    private var vaoId: Int = 0
    private var vboId: Int = 0
    private var eboId: Int = 0

    private var currentVertexCount = 0
    private var currentIndexCount = 0

    private val projectionMatrix = FloatArray(16)

    fun initGL() {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        uProjectionLoc = GLES30.glGetUniformLocation(program, "uProjection")
        uEnableScanlinesLoc = GLES30.glGetUniformLocation(program, "uEnableScanlines")
        uEnableCelShadingLoc = GLES30.glGetUniformLocation(program, "uEnableCelShading")
        uCelBandsLoc = GLES30.glGetUniformLocation(program, "uCelBands")

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vaoId = vaos[0]

        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        vboId = vbos[0]

        val ebos = IntArray(1)
        GLES30.glGenBuffers(1, ebos, 0)
        eboId = ebos[0]

        GLES30.glBindVertexArray(vaoId)

        // Bind VBO (Array Buffer) with orphan dynamic draw initialization
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, maxVertices * vertexStrideBytes, null, GLES30.GL_DYNAMIC_DRAW)

        // Bind EBO (Element Array Buffer)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, maxIndices * 2, null, GLES30.GL_DYNAMIC_DRAW)

        // Attribute 0: aPosition (2 floats, offset 0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, vertexStrideBytes, 0)
        GLES30.glEnableVertexAttribArray(0)

        // Attribute 1: aColor (4 normalized unsigned bytes, offset 8)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_UNSIGNED_BYTE, true, vertexStrideBytes, 8)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glBindVertexArray(0)

        // Default OpenGL state optimizations
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
    }

    fun setScreenSize(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1000f, 1000f)
    }

    fun beginBatch(
        enableScanlines: Boolean = false,
        enableCelShading: Boolean = true,
        celBands: Float = 3.0f
    ) {
        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uProjectionLoc, 1, false, projectionMatrix, 0)
        GLES30.glUniform1i(uEnableScanlinesLoc, if (enableScanlines) 1 else 0)
        GLES30.glUniform1i(uEnableCelShadingLoc, if (enableCelShading) 1 else 0)
        GLES30.glUniform1f(uCelBandsLoc, celBands)

        vertexByteBuffer.clear()
        indexShortBuffer.clear()
        currentVertexCount = 0
        currentIndexCount = 0
    }

    fun flush() {
        if (currentIndexCount == 0) return

        vertexByteBuffer.flip()
        indexShortBuffer.flip()

        GLES30.glBindVertexArray(vaoId)

        // VBO Orphan streaming to prevent CPU-GPU pipeline stalls
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, maxVertices * vertexStrideBytes, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, currentVertexCount * vertexStrideBytes, vertexByteBuffer)

        // EBO Orphan streaming
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, maxIndices * 2, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBufferSubData(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0, currentIndexCount * 2, indexShortBuffer)

        // Draw indexed primitives
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, currentIndexCount, GLES30.GL_UNSIGNED_SHORT, 0)

        GLES30.glBindVertexArray(0)

        vertexByteBuffer.clear()
        indexShortBuffer.clear()
        currentVertexCount = 0
        currentIndexCount = 0
    }

    private fun addVertexPacked(x: Float, y: Float, r: Byte, g: Byte, b: Byte, a: Byte): Short {
        val index = currentVertexCount
        vertexByteBuffer.putFloat(x)
        vertexByteBuffer.putFloat(y)
        vertexByteBuffer.put(r)
        vertexByteBuffer.put(g)
        vertexByteBuffer.put(b)
        vertexByteBuffer.put(a)
        currentVertexCount++
        return index.toShort()
    }

    private fun checkCapacity(neededVertices: Int, neededIndices: Int) {
        if (currentVertexCount + neededVertices > maxVertices || currentIndexCount + neededIndices > maxIndices) {
            flush()
        }
    }

    fun drawTriangle(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        color: Color
    ) {
        checkCapacity(3, 3)

        val r = (color.red.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val g = (color.green.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val b = (color.blue.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val a = (color.alpha.coerceIn(0f, 1f) * 255f).toInt().toByte()

        val i1 = addVertexPacked(x1, y1, r, g, b, a)
        val i2 = addVertexPacked(x2, y2, r, g, b, a)
        val i3 = addVertexPacked(x3, y3, r, g, b, a)

        indexShortBuffer.put(i1)
        indexShortBuffer.put(i2)
        indexShortBuffer.put(i3)
        currentIndexCount += 3
    }

    fun drawQuad(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float,
        color: Color
    ) {
        checkCapacity(4, 6)

        val r = (color.red.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val g = (color.green.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val b = (color.blue.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val a = (color.alpha.coerceIn(0f, 1f) * 255f).toInt().toByte()

        val i1 = addVertexPacked(x1, y1, r, g, b, a)
        val i2 = addVertexPacked(x2, y2, r, g, b, a)
        val i3 = addVertexPacked(x3, y3, r, g, b, a)
        val i4 = addVertexPacked(x4, y4, r, g, b, a)

        // Triangle 1: 1 -> 2 -> 3
        indexShortBuffer.put(i1)
        indexShortBuffer.put(i2)
        indexShortBuffer.put(i3)
        // Triangle 2: 1 -> 3 -> 4
        indexShortBuffer.put(i1)
        indexShortBuffer.put(i3)
        indexShortBuffer.put(i4)
        currentIndexCount += 6
    }

    fun drawLine(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        color: Color,
        width: Float = 2f
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        if (len < 0.001f) return

        val nx = -dy / len * (width / 2f)
        val ny = dx / len * (width / 2f)

        drawQuad(
            x1 + nx, y1 + ny,
            x2 + nx, y2 + ny,
            x2 - nx, y2 - ny,
            x1 - nx, y1 - ny,
            color
        )
    }

    fun drawIsoDiamond(
        cx: Float, cy: Float,
        halfW: Float, halfH: Float,
        fillColor: Color,
        strokeColor: Color? = null,
        strokeWidth: Float = 1.5f
    ) {
        val topX = cx
        val topY = cy - halfH

        val rightX = cx + halfW
        val rightY = cy

        val botX = cx
        val botY = cy + halfH

        val leftX = cx - halfW
        val leftY = cy

        drawQuad(
            topX, topY,
            rightX, rightY,
            botX, botY,
            leftX, leftY,
            fillColor
        )

        if (strokeColor != null) {
            drawLine(topX, topY, rightX, rightY, strokeColor, strokeWidth)
            drawLine(rightX, rightY, botX, botY, strokeColor, strokeWidth)
            drawLine(botX, botY, leftX, leftY, strokeColor, strokeWidth)
            drawLine(leftX, leftY, topX, topY, strokeColor, strokeWidth)
        }
    }

    fun drawIsoCube(
        cx: Float, cy: Float,
        halfW: Float, halfH: Float,
        wallHeight: Float,
        topColor: Color,
        leftColor: Color,
        rightColor: Color,
        strokeColor: Color? = null,
        strokeWidth: Float = 1.5f
    ) {
        val topCx = cx
        val topCy = cy - wallHeight

        // Top Face
        drawIsoDiamond(topCx, topCy, halfW, halfH, topColor, strokeColor, strokeWidth)

        // Left Face
        drawQuad(
            topCx - halfW, topCy + halfH,
            topCx, topCy + halfH * 2f,
            cx, cy + halfH * 2f,
            cx - halfW, cy + halfH,
            leftColor
        )

        // Right Face
        drawQuad(
            topCx, topCy + halfH * 2f,
            topCx + halfW, topCy + halfH,
            cx + halfW, cy + halfH,
            cx, cy + halfH * 2f,
            rightColor
        )

        if (strokeColor != null) {
            drawLine(topCx - halfW, topCy + halfH, cx - halfW, cy + halfH, strokeColor, strokeWidth)
            drawLine(topCx, topCy + halfH * 2f, cx, cy + halfH * 2f, strokeColor, strokeWidth)
            drawLine(topCx + halfW, topCy + halfH, cx + halfW, cy + halfH, strokeColor, strokeWidth)
        }
    }

    fun drawCircle(
        cx: Float, cy: Float,
        radius: Float,
        color: Color,
        segments: Int = 16,
        isFilled: Boolean = true,
        strokeWidth: Float = 2f
    ) {
        if (isFilled) {
            for (i in 0 until segments) {
                val a1 = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val a2 = ((i + 1).toFloat() / segments) * 2f * Math.PI.toFloat()

                val x1 = cx + cos(a1) * radius
                val y1 = cy + sin(a1) * radius

                val x2 = cx + cos(a2) * radius
                val y2 = cy + sin(a2) * radius

                drawTriangle(cx, cy, x1, y1, x2, y2, color)
            }
        } else {
            for (i in 0 until segments) {
                val a1 = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val a2 = ((i + 1).toFloat() / segments) * 2f * Math.PI.toFloat()

                val x1 = cx + cos(a1) * radius
                val y1 = cy + sin(a1) * radius

                val x2 = cx + cos(a2) * radius
                val y2 = cy + sin(a2) * radius

                drawLine(x1, y1, x2, y2, color, strokeWidth)
            }
        }
    }

    fun release() {
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
            vaoId = 0
        }
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
        if (eboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(eboId), 0)
            eboId = 0
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)
        return shader
    }
}

