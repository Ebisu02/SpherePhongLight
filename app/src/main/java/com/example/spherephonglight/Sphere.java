package com.example.spherephonglight;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class Sphere {
    private FloatBuffer vertexBuffer;
    private int program;
    private int positionHandle;
    private int normalHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    private final int COORDS_PER_VERTEX = 3;
    private final int NORMALS_PER_VERTEX = 3;
    private final int vertexStride = (COORDS_PER_VERTEX + NORMALS_PER_VERTEX) * 4; // 4 bytes per vertex

    private final float radius = 1.0f;
    private final int slices = 20;
    private final int stacks = 20;

    private float[] sphereVertices;

    private short[] indices;
    private ShortBuffer indexBuffer;
    private int ambientColorHandle;
    private final String vertexShaderCode = "" +
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec3 vNormal;" +
            "varying vec3 fragmentNormal;" +
            "void main() {" +
            "   gl_Position = uMVPMatrix * vPosition;" +
            "   fragmentNormal = normalize(vNormal);" +
            "}";
    private final String fragmentShaderCode = "" +
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "uniform vec4 vAmbientColor;" +
            "varying vec3 fragmentNormal;" +
            "void main() {" +
            "   vec3 lightDirection = normalize(vec3(1.0, 1.0, 1.0));" +
            "   float diffuse = max(dot(fragmentNormal, lightDirection), 0.1);" +
            "   vec4 diffuseColor = vColor * diffuse;" +
            "   vec4 ambientColor = vAmbientColor;" +
            "   gl_FragColor = diffuseColor + ambientColor;" +
            "}";

    public Sphere() {
        ambientColorHandle = GLES20.glGetUniformLocation(program, "vAmbientColor");
        ArrayList<Short> indexList = new ArrayList<Short>();
        for (int stackNum = 0; stackNum < stacks; stackNum++) {
            for (int sliceNum = 0; sliceNum < slices; sliceNum++) {
                short vertexIndex = (short) (stackNum * (slices + 1) + sliceNum);
                indexList.add(vertexIndex);
                indexList.add((short) (vertexIndex + slices + 1));
            }
        }
        indices = new short[indexList.size()];
        for (int i = 0; i < indexList.size(); ++i) {
            indices[i] = indexList.get(i);
        }
        ByteBuffer bbd = ByteBuffer.allocateDirect(indices.length * 2);
        bbd.order(ByteOrder.nativeOrder());
        indexBuffer = bbd.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        // Init sphere vertices and normals
        ArrayList<Float> vertices = new ArrayList<>();
        for (int stackNum = 0; stackNum <= stacks; stackNum++) {
            float stackFraction = (float) stackNum / (float) stacks;
            float stackAngle = stackFraction * (float) Math.PI;

            for (int sliceNum = 0; sliceNum <= slices; sliceNum++) {
                float sliceFraction = (float) sliceNum / (float) slices;
                float sliceAngle = sliceFraction * 2.0f * (float) Math.PI;

                float x = (float) (radius * Math.sin(stackAngle) * Math.cos(sliceAngle));
                float y = (float) (radius * Math.sin(stackAngle) * Math.sin(sliceAngle));
                float z = (float) (radius * Math.cos(stackAngle));

                // Vertex coordinates
                vertices.add(x);
                vertices.add(y);
                vertices.add(z);

                // Normals
                float normalX = x / radius;
                float normalY = y / radius;
                float normalZ = z / radius;
                vertices.add(normalX);
                vertices.add(normalY);
                vertices.add(normalZ);
            }
        }

        sphereVertices = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            sphereVertices[i] = vertices.get(i);
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(sphereVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(sphereVertices);
        vertexBuffer.position(0);

        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (vertexShaderHandle != 0) {
            GLES20.glShaderSource(vertexShaderHandle, vertexShaderCode);
            GLES20.glCompileShader(vertexShaderHandle);

            // Create the fragment shader
            int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            if (fragmentShaderHandle != 0) {
                GLES20.glShaderSource(fragmentShaderHandle, fragmentShaderCode);
                GLES20.glCompileShader(fragmentShaderHandle);

                // Create and link the program
                program = GLES20.glCreateProgram();
                GLES20.glAttachShader(program, vertexShaderHandle);
                GLES20.glAttachShader(program, fragmentShaderHandle);
                GLES20.glLinkProgram(program);
            }
        }
    }

    public void draw(float[] mMVPMatrix, float[] color, float[] ambientColor) {
        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        normalHandle = GLES20.glGetAttribLocation(program, "vNormal");
        colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(normalHandle, NORMALS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(normalHandle);

        GLES20.glUniform4fv(colorHandle, 1, color , 0);
        GLES20.glUniform4fv(ambientColorHandle, 1, ambientColor, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the sphere
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, sphereVertices.length / (COORDS_PER_VERTEX + NORMALS_PER_VERTEX));

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
    }
}