package com.carlospinan.airhockeytextured.data

import android.opengl.GLES20.GL_FLOAT
import com.carlospinan.airhockeytextured.common.BYTES_PER_FLOAT
import com.carlospinan.airhockeytextured.utilities.OpenGLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VertexArray(
    vertexData: FloatArray
) {

    private val floatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * BYTES_PER_FLOAT)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertexData)

    fun setVertexAttribPointer(
        dataOffset: Int,
        attributeLocation: Int,
        componentCount: Int,
        stride: Int
    ) {
        floatBuffer.position(dataOffset)

        OpenGLES20.gl2VertexAttribPointer(
            attributeLocation,
            componentCount,
            GL_FLOAT,
            false,
            stride,
            floatBuffer
        )

        OpenGLES20.gl2EnableVertexAttribArray(attributeLocation)

        floatBuffer.position(0)
    }

}