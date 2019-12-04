package com.carlospinan.heightmap

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLES20.*
import android.opengl.Matrix.*
import androidx.core.math.MathUtils
import com.carlospinan.heightmap.common.GLBaseRenderer
import com.carlospinan.heightmap.extensions.loadCubeMap
import com.carlospinan.heightmap.extensions.loadTexture
import com.carlospinan.heightmap.objects.Heightmap
import com.carlospinan.heightmap.objects.ParticleShooter
import com.carlospinan.heightmap.objects.ParticleSystem
import com.carlospinan.heightmap.objects.Skybox
import com.carlospinan.heightmap.programs.HeightmapShaderProgram
import com.carlospinan.heightmap.programs.ParticleShaderProgram
import com.carlospinan.heightmap.programs.SkyboxShaderProgram
import com.carlospinan.heightmap.utilities.Point
import com.carlospinan.heightmap.utilities.Vector
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


const val ANGLE_VARIANCE = 5f
const val SPEED_VARIANCE = 1f

class HeightBoxRenderer(
    private val context: Context
) : GLBaseRenderer() {

    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewMatrixForSkybox = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private val tempMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    private lateinit var redParticleShooter: ParticleShooter
    private lateinit var greenParticleShooter: ParticleShooter
    private lateinit var blueParticleShooter: ParticleShooter

    private var globalStartTime = -1L
    private var particleTexture = -1
    private var skyboxTexture = -1

    private lateinit var skybox: Skybox
    private lateinit var skyboxProgram: SkyboxShaderProgram

    private lateinit var heightmap: Heightmap
    private lateinit var heightmapProgram: HeightmapShaderProgram

    private lateinit var particleProgram: ParticleShaderProgram
    private lateinit var particleSystem: ParticleSystem

    private var xRotation = 0f
    private var yRotation = 0f

    override fun onSurfaceCreated(unused: GL10?, eglConfig: EGLConfig?) {
        glClearColor(0f, 0f, 0f, 0f)

        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)

        heightmapProgram = HeightmapShaderProgram(context)
        heightmap = Heightmap(
            (context.resources.getDrawable(R.drawable.heightmap) as BitmapDrawable).bitmap
        )

        skyboxProgram = SkyboxShaderProgram(context)
        skybox = Skybox()

        particleProgram = ParticleShaderProgram(context)
        particleSystem = ParticleSystem(10000)
        globalStartTime = System.nanoTime()

        val particleDirection = Vector(0f, 0.5f, 0f)

        redParticleShooter = ParticleShooter(
            Point(-1f, 0f, 0f),
            particleDirection,
            Color.rgb(255, 50, 5),
            ANGLE_VARIANCE,
            SPEED_VARIANCE
        )

        greenParticleShooter = ParticleShooter(
            Point.zero(),
            particleDirection,
            Color.rgb(25, 255, 25),
            ANGLE_VARIANCE,
            SPEED_VARIANCE
        )

        blueParticleShooter = ParticleShooter(
            Point(1f, 0f, 0f),
            particleDirection,
            Color.rgb(5, 50, 255),
            ANGLE_VARIANCE,
            SPEED_VARIANCE
        )

        particleTexture = context.loadTexture(R.drawable.particle_texture)

        skyboxTexture = context.loadCubeMap(
            intArrayOf(
                R.drawable.left, R.drawable.right,
                R.drawable.bottom, R.drawable.top,
                R.drawable.front, R.drawable.back
            )
        )

    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        perspectiveM(
            projectionMatrix,
            0,
            45f,
            width.toFloat() / height.toFloat(),
            1f,
            100f // 10f
        )

        updateViewMatrices()
    }

    override fun onDrawFrame(unused: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        drawHeightmap()
        drawSkyBox()
        drawParticles()
    }

    override fun handleTouchDrag(x: Float, y: Float) {
        xRotation += x / 50f
        yRotation += y / 50f

        // xRotation = MathUtils.clamp(xRotation, -90f, 90f)
        yRotation = MathUtils.clamp(yRotation, -90f, 90f)

        // Setup view matrix
        updateViewMatrices()
    }

    private fun updateViewMatrices() {
        setIdentityM(viewMatrix, 0)
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f)
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f)
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.size)

        // We want the translation to apply to the regular view matrix, and not
        // the skybox.
        translateM(viewMatrix, 0, 0f, -1.5f, -5f)
    }

    private fun updateMvpMatrix() {
        multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        multiplyMM(
            modelViewProjectionMatrix,
            0,
            projectionMatrix,
            0,
            tempMatrix,
            0
        )
    }

    private fun updateMvpMatrixForSkybox() {
        multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0)
        multiplyMM(
            modelViewProjectionMatrix,
            0,
            projectionMatrix,
            0,
            tempMatrix,
            0
        )
    }

    private fun drawHeightmap() {
        setIdentityM(modelMatrix, 0)
        // Expand the heightmap's dimensions, but don't expand the height as
        // much so that we don't get insanely tall mountains.
        scaleM(modelMatrix, 0, 100f, 10f, 100f)

        updateMvpMatrix()

        heightmapProgram.useProgram()
        heightmapProgram.setUniforms(modelViewProjectionMatrix)
        heightmap.bindData(heightmapProgram)
        heightmap.draw()
    }

    private fun drawSkyBox() {
        setIdentityM(
            modelMatrix,
            0
        )
        updateMvpMatrixForSkybox()

        glDepthFunc(GL_LEQUAL) // This avoids problems with the skybox itself getting clipped.

        skyboxProgram.useProgram()
        skyboxProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture)
        skybox.bindData(skyboxProgram)
        skybox.draw()

        glDepthFunc(GL_LESS)
    }

    private fun drawParticles() {
        val currentTime = (System.nanoTime() - globalStartTime) / 1000000000f

        redParticleShooter.addParticles(particleSystem, currentTime, 1)
        greenParticleShooter.addParticles(particleSystem, currentTime, 1)
        blueParticleShooter.addParticles(particleSystem, currentTime, 1)

        setIdentityM(modelMatrix, 0)
        updateMvpMatrix()

        glDepthMask(false)
        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE)

        particleProgram.useProgram()
        particleProgram.setUniforms(modelViewProjectionMatrix, currentTime, particleTexture)
        particleSystem.bindData(particleProgram)
        particleSystem.draw()

        glDisable(GL_BLEND)
        glDepthMask(true)
    }

}