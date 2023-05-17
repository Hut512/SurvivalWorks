package de.survivalworkers.core.client.engine.vk.scene;

import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RenderObject {
    @Getter
    protected Matrix4f modelMatrix;
    @Getter
    protected Vector3f position;
    @Getter
    protected Quaternionf rotation;
    @Getter
    protected Vector3f scale;

    public RenderObject(Vector3f position) {
        this.position = position;
        scale = new Vector3f(1.0f,1.0f,1.0f);
        rotation = new Quaternionf();
        modelMatrix = new Matrix4f();
        updateModelMatrix();
    }

    public final void setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
        updateModelMatrix();
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
        updateModelMatrix();
    }

    public void updateModelMatrix() {
        modelMatrix.translationRotateScale(position, rotation, scale);
    }
}
