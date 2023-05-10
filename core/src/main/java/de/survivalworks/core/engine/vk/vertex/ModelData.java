package de.survivalworks.core.engine.vk.vertex;

import org.joml.Vector4f;

import java.util.List;

public class ModelData {
    private List<MeshData> meshData;
    private String  id;
    private List<Material> materials;

    public ModelData(String  id, List<MeshData> meshData,List<Material> materials){
        this.id = id;
        this.meshData = meshData;
        this.materials = materials;
    }

    public List<MeshData> getMeshData() {
        return meshData;
    }

    public String  getId() {
        return id;
    }

    public record MeshData(float[] pos, float[] texCords, int[] indices,int materialI){}

    public record Material(String texPath, Vector4f diffuseColor) {
        public static final Vector4f DEFAULT_COLOR = new Vector4f(1.0f,1.0f,1.0f,1.0f);

        public Material(){
            this(null,DEFAULT_COLOR);
        }
    }

    public List<Material> getMaterials() {
        return materials;
    }
}
