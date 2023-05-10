package de.survivalworks.core.engine.vk.scene;

import org.joml.Matrix4f;

public class Projection {
    Matrix4f projectionMatrix;

    public Projection(){
        projectionMatrix = new Matrix4f();
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public void resize(int width, int height) {
        projectionMatrix.identity();
        projectionMatrix.setPerspective(60, (float) width / (float) height,
                1.0f, 500.0f,true);
    }
}
