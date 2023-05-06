package de.survivalworkers.core.client.engine.graphics.vertex;

import de.survivalworkers.core.client.engine.graphics.Util;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModelLoader {
    public static ModelData loadModel(String id,String modelPath,String texPath){
        return loadModel(id,modelPath,texPath,
                Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_Triangulate | Assimp.aiProcess_FixInfacingNormals | Assimp.aiProcess_CalcTangentSpace | Assimp.aiProcess_PreTransformVertices);
    }

    private static ModelData loadModel(String id, String modelPath, String texPath, int i) {
        if(!new File(modelPath).exists())throw new RuntimeException("Path to model:" + modelPath + " does not exist");
        if(!new File(texPath).exists())throw new RuntimeException("Path to Texture:" + texPath + " does not exist");
        AIScene scene = Assimp.aiImportFile(modelPath,i);
        if(scene == null)throw new RuntimeException("Could not load model" + modelPath + " and " + texPath);
        List<ModelData.Material> materials = new ArrayList<>();
        for (int j = 0; j < scene.mNumMaterials(); j++) {
            AIMaterial aiMaterial = AIMaterial.create(scene.mMaterials().get(j));
            ModelData.Material material = processMaterial(aiMaterial,texPath);
            materials.add(material);
        }

        PointerBuffer pp = scene.mMeshes();
        List<ModelData.MeshData> meshes = new ArrayList<>();
        for (int j = 0; j < scene.mNumMeshes(); j++) {
            AIMesh aiMesh = AIMesh.create(pp.get(j));
            ModelData.MeshData meshData = processMesh(aiMesh);
            meshes.add(meshData);
        }

        ModelData data = new ModelData(id,meshes,materials);

        Assimp.aiReleaseImport(scene);
        return data;
    }

    private static ModelData.MeshData processMesh(AIMesh aiMesh) {
        List<Float> vertices = processVertices(aiMesh);
        List<Float> texCoords = processTexCoords(aiMesh);
        List<Integer> indices = processIndicies(aiMesh);

        if(texCoords.isEmpty()){
            for (int i = 0; i < (vertices.size() / 3) * 2; i++) {
                texCoords.add(0.0f);
            }
        }

        int matI = aiMesh.mMaterialIndex();
        return new ModelData.MeshData(Util.toArrayFloat(vertices),Util.toArrayFloat(texCoords),Util.toArrayInt(indices),matI);
    }

    private static List<Integer> processIndicies(AIMesh aiMesh) {
        List<Integer> indices = new ArrayList<>();
        AIFace.Buffer aiFaces = aiMesh.mFaces();
        for (int i = 0; i < aiMesh.mNumFaces(); i++) {
            AIFace aiFace = aiFaces.get(i);
            IntBuffer ip = aiFace.mIndices();
            while (ip.remaining() > 0) indices.add(ip.get());

        }
        return indices;
    }

    private static List<Float> processTexCoords(AIMesh aiMesh) {
        List<Float> texCoords = new ArrayList<>();
        AIVector3D.Buffer aiTexCoords = aiMesh.mTextureCoords(0);
        int count = aiTexCoords != null ? aiTexCoords.remaining() : 0;
        for (int i = 0; i < count; i++) {
            AIVector3D texCoord = aiTexCoords.get();
            texCoords.add(texCoord.x());
            texCoords.add(1 - texCoord.y());
        }

        return texCoords;
    }

    private static List<Float> processVertices(AIMesh aiMesh) {
        List<Float> vertices = new ArrayList<>();
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
            vertices.add(aiVertex.z());
        }
        return vertices;
    }

    private static ModelData.Material processMaterial(AIMaterial aiMaterial, String texPath) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D color4D = AIColor4D.create();

            Vector4f diffuse = ModelData.Material.DEFAULT_COLOR;
            int res = Assimp.aiGetMaterialColor(aiMaterial,Assimp.AI_MATKEY_COLOR_DIFFUSE,Assimp.aiTextureType_NONE,0,color4D);
            if(res == Assimp.aiReturn_SUCCESS)diffuse = new Vector4f(color4D.r(),color4D.g(),color4D.b(),color4D.a());
            AIString aiTexturePath = AIString.calloc(stack);
            Assimp.aiGetMaterialTexture(aiMaterial,Assimp.aiTextureType_DIFFUSE,0,aiTexturePath,(IntBuffer) null,null,null,null,null,null);
            String texturePath = aiTexturePath.dataString();
            if(texturePath != null && texturePath.length() > 0){
                //TODO Replace with real Texture path (Currently uses the path specified in the .mtl file)
                texturePath = texPath + File.separator + new File(texturePath).getName();
                diffuse = new Vector4f(0.0f,0.0f,0.0f,0.0f);
            }

            return new ModelData.Material(texturePath,diffuse);
        }
    }
}
