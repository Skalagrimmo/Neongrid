package com.example.render

import android.opengl.GLES30
import android.opengl.Matrix
import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class GlBatchRenderer {

    private val vertexShaderCode = """
        #version 300 es
        layout(location = 0) in vec2 aPosition;
        layout(location = 1) in vec4 aColor;
        
        uniform mat4 uProjection;
        out vec4 vColor;
        
        void main() {
            vColor = aColor;
            gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #version 300 es
        precision mediump float;
        
        in vec4 vColor;
        out vec4 fragColor;
        
        uniform bool uEnableScanlines;
        uniform bool uEnableCelShading;
        uniform float uCelBands;
        
        void main() {
            vec4 col = vColor;
            
            if (uEnableCelShading && col.a > 0.05) {
                float luma = dot(col.rgb, vec3(0.299, 0.587, 0.114));
                if (luma > 0.02) {
                    float bands = max(uCelBands, 2.0);
                    float quantized = floor(luma * bands) / (bands - 1.0);
                    quantized = max(quantized, 0.30); // Preserve shadow floor
                    float scale = quantized / luma;
                    col.rgb = clamp(col.rgb * scale * 1.08, 0.0, 1.0);
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

    private val maxVertices = 65536
    private val floatsPerVertex = 6 // x, y, r, g, b, a
    private val vertexStrideBytes = floatsPerVertex * 4

    private val floatBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(maxVertices * vertexStrideBytes)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private var vaoId: Int = 0
    private var vboId: Int = 0
    private var currentVertexCount = 0

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

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, maxVertices * vertexStrideBytes, null, GLES30.GL_DYNAMIC_DRAW)

        // Attribute 0: aPosition (2 floats)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, vertexStrideBytes, 0)
        GLES30.glEnableVertexAttribArray(0)

        // Attribute 1: aColor (4 floats)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, vertexStrideBytes, 2 * 4)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glBindVertexArray(0)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun setScreenSize(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
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

        floatBuffer.clear()
        currentVertexCount = 0
    }

    fun flush() {
        if (currentVertexCount == 0) return

        floatBuffer.flip()
        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, currentVertexCount * vertexStrideBytes, floatBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, currentVertexCount)

        GLES30.glBindVertexArray(0)

        floatBuffer.clear()
        currentVertexCount = 0
    }

    private fun addVertex(x: Float, y: Float, color: Color) {
        if (currentVertexCount >= maxVertices - 6) {
            flush()
        }
        floatBuffer.put(x)
        floatBuffer.put(y)
        floatBuffer.put(color.red)
        floatBuffer.put(color.green)
        floatBuffer.put(color.blue)
        floatBuffer.put(color.alpha)
        currentVertexCount++
    }

    fun drawTriangle(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        color: Color
    ) {
        addVertex(x1, y1, color)
        addVertex(x2, y2, color)
        addVertex(x3, y3, color)
    }

    fun drawQuad(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float,
        color: Color
    ) {
        // Triangle 1
        addVertex(x1, y1, color)
        addVertex(x2, y2, color)
        addVertex(x3, y3, color)

        // Triangle 2
        addVertex(x1, y1, color)
        addVertex(x3, y3, color)
        addVertex(x4, y4, color)
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

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)
        return shader
    }
}
