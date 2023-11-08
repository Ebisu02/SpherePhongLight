package com.example.spherephonglight;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class Sphere {
    private FloatBuffer vertexBuffer;
    private ShortBuffer indexBuffer;
    private int program;
    private int positionHandle;
    private int normalHandle;
    private int colorHandle;
    private int mvpMatrixHandle;
    private int shininessHandle;
    private int lightDirectionHandle;

    private final int COORDS_PER_VERTEX = 3;
    private final int NORMALS_PER_VERTEX = 3;
    private final int vertexStride = (COORDS_PER_VERTEX + NORMALS_PER_VERTEX) * 4; // 4 bytes per vertex

    private final float radius = 1.0f;
    private final int slices = 20;
    private final int stacks = 20;

    private float[] sphereVertices;
    private short[] indices;

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
            "uniform vec3 lightDirection;" + // Направление света
            "uniform float shininess;" + // Параметр блеска
            "varying vec3 fragmentNormal;" +
            "void main() {" +
            "   vec3 lightDir = normalize(lightDirection);" +
            "   float diffuse = max(dot(fragmentNormal, lightDir), 0.1);" +
            "   vec4 diffuseColor = vColor * diffuse;" +
            "   vec4 ambientColor = vAmbientColor;" +
            "   vec3 reflectDir = reflect(-lightDir, fragmentNormal);" +
            "   float specular = pow(max(dot(reflectDir, normalize(vec3(0, 0, 1))), 0.0), shininess);" +
            "   vec4 specularColor = vec4(1.0, 1.0, 1.0, 1.0) * specular;" +
            "   gl_FragColor = diffuseColor + ambientColor + specularColor;" +
            "}";

    public Sphere() {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Short> indexList = new ArrayList<>();

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

        for (int stackNum = 0; stackNum <= stacks; stackNum++) {
            float stackFraction = (float) stackNum / (float) stacks;
            float stackAngle = stackFraction * (float) Math.PI;

            for (int sliceNum = 0; sliceNum <= slices; sliceNum++) {
                float sliceFraction = (float) sliceNum / (float) slices;
                float sliceAngle = sliceFraction * 2.0f * (float) Math.PI;

                float x = radius * (float) (Math.sin(stackAngle) * Math.cos(sliceAngle));
                float y = radius * (float) (Math.sin(stackAngle) * Math.sin(sliceAngle));
                float z = radius * (float) Math.cos(stackAngle);

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

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        for (Float vertex : vertices) {
            vertexBuffer.put(vertex);
        }
        vertexBuffer.position(0);

        ByteBuffer bbd = ByteBuffer.allocateDirect(indices.length * 2);
        bbd.order(ByteOrder.nativeOrder());
        indexBuffer = bbd.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        if (vertexShaderHandle != 0) {
            GLES20.glShaderSource(vertexShaderHandle, vertexShaderCode);
            GLES20.glCompileShader(vertexShaderHandle);
        }

        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        if (fragmentShaderHandle != 0) {
            GLES20.glShaderSource(fragmentShaderHandle, fragmentShaderCode);
            GLES20.glCompileShader(fragmentShaderHandle);
        }

        program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShaderHandle);
            GLES20.glAttachShader(program, fragmentShaderHandle);
            GLES20.glLinkProgram(program);
            GLES20.glUseProgram(program);
        }

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        normalHandle = GLES20.glGetAttribLocation(program, "vNormal");
        colorHandle = GLES20.glGetUniformLocation(program, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        shininessHandle = GLES20.glGetUniformLocation(program, "shininess");
        lightDirectionHandle = GLES20.glGetUniformLocation(program, "lightDirection");
    }

    public void draw(float[] mMVPMatrix, float[] color, float shininess, float[] lightDirection) {
        GLES20.glUseProgram(program);

        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(normalHandle, NORMALS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLES20.glEnableVertexAttribArray(normalHandle);

        GLES20.glUniform4fv(colorHandle, 1, color , 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1f(shininessHandle, shininess);
        GLES20.glUniform3fv(lightDirectionHandle, 1, lightDirection, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indices.length, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
    }
}
