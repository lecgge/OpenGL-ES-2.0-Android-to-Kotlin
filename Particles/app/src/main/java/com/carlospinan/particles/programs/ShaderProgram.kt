package com.carlospinan.particles.programs

import android.content.Context
import com.carlospinan.particles.extensions.readTextFileFromResource
import com.carlospinan.particles.utilities.OpenGLES20
import com.carlospinan.particles.utilities.ShaderUtils

const val U_MATRIX = "u_Matrix"
const val U_TIME = "u_Time"
const val U_TEXTURE_UNIT = "u_TextureUnit"

const val A_POSITION = "a_Position"
const val A_COLOR = "a_Color"
const val A_DIRECTION_VECTOR = "a_DirectionVector"
const val A_PARTICLE_START_TIME = "a_ParticleStartTime"

open class ShaderProgram(
    context: Context,
    vertexShaderResourceId: Int,
    fragmentShaderResourceId: Int
) {

    val program by lazy {
        ShaderUtils.buildProgram(
            context.readTextFileFromResource(vertexShaderResourceId),
            context.readTextFileFromResource(fragmentShaderResourceId)
        )
    }

    fun useProgram() {
        // Set the current OpenGL shader program to this program.
        OpenGLES20.gl2UseProgram(program)
    }

}