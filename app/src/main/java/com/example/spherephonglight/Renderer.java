package com.example.spherephonglight;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Renderer extends Activity {

    private GLSurfaceView glSurfaceView;
    private MyGLRenderer myGlRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView.setEGLContextClientVersion(2);
        myGlRenderer = new MyGLRenderer();
        glSurfaceView.setRenderer(myGlRenderer);
        setContentView(glSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    private class MyGLRenderer implements GLSurfaceView.Renderer {
        private Sphere sphere;
        private float[] mViewMatrix = new float[16];
        private float[] mProjectionMatrix = new float[16];
        private float[] mModelMatrix = new float[16];
        private float[] mMVPMatrix = new float[16];
        private float[] lightDirection = {1.0f, 1.0f, 1.0f}; // Направление света

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig eglConfig) {
            sphere = new Sphere();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // Очистка экрана и буфера глубины
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Установка позиции камеры
            Matrix.setLookAtM(mViewMatrix, 0, 10, 0, -5, 0, 0, 0, 0, 1, 0);

            // Установка матрицы проекции
            float ratio = (float) glSurfaceView.getWidth() / (float) glSurfaceView.getHeight();
            Matrix.perspectiveM(mProjectionMatrix, 0, 45, ratio, 0.1f, 100f);

            // Вычисление MVP-матрицы
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

            // Настройка параметров освещения и рисование сферы
            float[] diffuseColor = {1.0f, 0.0f, 0.0f, 1.0f}; // Цвет диффузного освещения
            float shininess = 500.0f; // Фактор блеска
            sphere.draw(mMVPMatrix, diffuseColor, shininess, lightDirection);
        }
    }

}