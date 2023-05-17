package de.survivalworkers.core.client.engine.vk.vertex;

import lombok.Getter;
import org.joml.*;

import java.util.List;

public class ModelData {
    @Getter
    private List<MeshData> meshData;
    @Getter
    private String  id;
    @Getter
    private List<Material> materials;


    public ModelData(String  id, List<MeshData> meshData,List<Material> materials){
        this.id = id;
        this.meshData = meshData;
        this.materials = materials;
    }

    public record MeshData(float[] pos, float[] normals, float[] tangents, float[] biTangents, float[] texCords, int[] indices,int materialI){}
    public record Material(String texPath, String normalMapPath, String metalRoughMap, Vector4f diffuseColor, float roughness, float metallic) {
        public static final Vector4f DEFAULT_COLOR = new Vector4f(1.0f,1.0f,1.0f,1.0f);

        public Material() {
            this(null,null,null,DEFAULT_COLOR,0.0f,0.0f);
        }
    }
}