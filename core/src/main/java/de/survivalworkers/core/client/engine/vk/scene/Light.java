package de.survivalworkers.core.client.engine.vk.scene;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Light extends RenderObject{
    @Getter
    private Vector4f color;
    @Setter
    @Getter
    private boolean point;

    public Light(Vector3f position,boolean point) {
        super(position);
        color = new Vector4f(0.0f,0.0f,0.0f,0.0f);
        this.point = point;
    }
}
