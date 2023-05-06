package de.survivalworkers.core.client.engine.vk.scene;

import org.joml.Matrix4f;

public class Projection {
    Matrix4f projectionMatrix;

    public Projection(){
        projectionMatrix = new Matrix4f();
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public void resize(int width, int height) {;
        projectionMatrix.identity();
        /*projectionMatrix.setPerspective(engProps.getFov(), (float) width / (float) height,
                engProps.getzNear(), engProps.getzFar(),true);*/
    }
}
