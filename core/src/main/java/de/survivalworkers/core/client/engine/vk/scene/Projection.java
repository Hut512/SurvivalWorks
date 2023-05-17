package de.survivalworkers.core.client.engine.vk.scene;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

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
        projectionMatrix.setPerspective(1.0471976f, (float) width / (float) height, 1.0f, 500.0f,true);
    }
}
