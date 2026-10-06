package com.example.render

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Small OpenGL ES 3.0 textured batch for pixel-art atlases.
 *
 * The same implementation serves civilian and enemy presentation layers;
 * gameplay systems never depend on this renderer.
 */
class GlSpriteBatchRenderer(
    private val assets: AssetManager,
    private val assetPath: String = "sprites/pedestrians/pedestrians_atlas.webp",
    private val columns: Int = PedestrianSpriteAtlas.columns,
    private val rows: Int = PedestrianSpriteAtlas.rows
) {

    private val vertexShaderCode = """
        #version 300 es
        layout(location = 0) in vec2 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        layout(location = 2) in float aAlpha;
        uniform mat4 uProjection;
        out vec2 vTexCoord;
        out float vAlpha;
        void main() {
            vTexCoord = aTexCoord;
            vAlpha = aAlpha;
            gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #version 300 es
        precision lowp float;
        in vec2 vTexCoord;
        in float vAlpha;
        uniform sampler2D uTexture;
        uniform vec4 uTint;
        out vec4 fragColor;
        void main() {
            vec4 tex = texture(uTexture, vTexCoord);
            if (tex.a < 0.02) discard;
            fragColor = tex * uTint;
            fragColor.a *= vAlpha;
        }
    """.trimIndent()

    private val maxSprites = 256
    private val floatsPerVertex = 5
    private val maxVertices = maxSprites * 6
    private val buffer = ByteBuffer
        .allocateDirect(maxVertices * floatsPerVertex * 4)
        .order(ByteOrder.nativeOrder())
    private val projection = FloatArray(16)

    private var program = 0
    private var textureId = 0
    private var uProjectionLoc = -1
    private var uTextureLoc = -1
    private var uTintLoc = -1
    private var vaoId = 0
    private var vboId = 0
    private var vertexCount = 0

    fun initGL() {
        program = createProgram(vertexShaderCode, fragmentShaderCode)
        uProjectionLoc = GLES30.glGetUniformLocation(program, "uProjection")
        uTextureLoc = GLES30.glGetUniformLocation(program, "uTexture")
        uTintLoc = GLES30.glGetUniformLocation(program, "uTint")

        val ids = IntArray(2)
        GLES30.glGenVertexArrays(1, ids, 0)
        vaoId = ids[0]
        GLES30.glGenBuffers(1, ids, 1)
        vboId = ids[1]

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            maxVertices * floatsPerVertex * 4,
            null,
            GLES30.GL_DYNAMIC_DRAW
        )
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, floatsPerVertex * 4, 0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, floatsPerVertex * 4, 8)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, floatsPerVertex * 4, 16)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glBindVertexArray(0)

        val tex = IntArray(1)
        GLES30.glGenTextures(1, tex, 0)
        textureId = tex[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        val options = BitmapFactory.Options().apply { inScaled = false }
        assets.open(assetPath).use {
            val bitmap = BitmapFactory.decodeStream(it, null, options)
                ?: error("Unable to decode sprite atlas: $assetPath")
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun setScreenSize(width: Int, height: Int) {
        android.opengl.Matrix.orthoM(
            projection, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f
        )
    }

    fun begin(tint: Color = Color.White) {
        buffer.clear()
        vertexCount = 0
        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uProjectionLoc, 1, false, projection, 0)
        GLES30.glUniform4f(uTintLoc, tint.red, tint.green, tint.blue, tint.alpha)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(uTextureLoc, 0)
        GLES30.glBindVertexArray(vaoId)
    }

    fun drawSprite(
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        frameIndex: Int,
        alpha: Float = 1f,
        flipX: Boolean = false
    ) {
        if (vertexCount + 6 > maxVertices) flush()

        val safeFrame = frameIndex.mod(columns * rows)
        val col = safeFrame % columns
        val row = safeFrame / columns
        val u0 = col.toFloat() / columns
        val u1 = (col + 1).toFloat() / columns
        val v0 = 1f - (row + 1).toFloat() / rows
        val v1 = 1f - row.toFloat() / rows

        val left = centerX - width / 2f
        val right = centerX + width / 2f
        val top = bottomY - height
        val bottom = bottomY

        fun vertex(x: Float, y: Float, u: Float, v: Float) {
            buffer.putFloat(x)
            buffer.putFloat(y)
            buffer.putFloat(u)
            buffer.putFloat(v)
            buffer.putFloat(alpha.coerceIn(0f, 1f))
            vertexCount++
        }

        val lu = if (flipX) u1 else u0
        val ru = if (flipX) u0 else u1

        vertex(left, top, lu, v1)
        vertex(right, top, ru, v1)
        vertex(right, bottom, ru, v0)
        vertex(left, top, lu, v1)
        vertex(right, bottom, ru, v0)
        vertex(left, bottom, lu, v0)
    }

    fun flush() {
        if (vertexCount == 0) return
        buffer.flip()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferSubData(
            GLES30.GL_ARRAY_BUFFER,
            0,
            vertexCount * floatsPerVertex * 4,
            buffer
        )
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        buffer.clear()
        vertexCount = 0
    }

    fun release() {
        if (textureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (program != 0) {
            GLES30.glDeleteProgram(program)
            program = 0
        }
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
            vaoId = 0
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        fun compile(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)
            return shader
        }
        val vs = compile(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, vs)
        GLES30.glAttachShader(p, fs)
        GLES30.glLinkProgram(p)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return p
    }
}
