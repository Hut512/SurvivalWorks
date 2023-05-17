package de.survivalworkers.core.client.engine.vk.scene;

import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity extends RenderObject{
    @Getter
    private String id;
    @Getter
    private String modelId;

    public Entity(String id, String modelId, Vector3f position) {
        super(position);
        this.id = id;
        this.modelId = modelId;
        this.position = position;
        rotation = new Quaternionf();
        modelMatrix = new Matrix4f();
        updateModelMatrix();
    }
}
