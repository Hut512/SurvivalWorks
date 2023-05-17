package de.survivalworkers.core.client.engine.vk.vertex;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.experimental.UtilityClass;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.*;

@UtilityClass
public class ModelLoader {
    public static ModelData loadModel(String id,String modelPath,String texPath){
        return loadModel(id,modelPath,texPath,aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate | aiProcess_FixInfacingNormals |
                aiProcess_CalcTangentSpace | aiProcess_PreTransformVertices);
    }

    private static ModelData loadModel(String id, String modelPath, String texPath, int i) {
        if(!new File(modelPath).exists())throw new RuntimeException("Path to model:" + modelPath + " does not exist");
        if(!new File(texPath).exists())throw new RuntimeException("Path to Texture:" + texPath + " does not exist");
        AIScene scene = aiImportFile(modelPath,i);
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

        aiReleaseImport(scene);
        return data;
    }
    private static ModelData.MeshData processMesh(AIMesh aiMesh) {
        List<Float> vertices = processVertices(aiMesh);
        List<Float> normals = processNormals(aiMesh);
        List<Float> tangents = processTangents(aiMesh, normals);
        List<Float> biTangents = processBitangents(aiMesh, normals);
        List<Float> texCoords = processTexCoords(aiMesh);
        List<Integer> indices = processIndicies(aiMesh);
        if(texCoords.isEmpty()){
            for (int i = 0; i < (vertices.size() / 3) * 2; i++) {
                texCoords.add(0.0f);
            }
        }

        int matI = aiMesh.mMaterialIndex();
        return new ModelData.MeshData(Util.toArrayFloat(vertices),Util.toArrayFloat(normals),Util.toArrayFloat(tangents),Util.toArrayFloat(biTangents),Util.toArrayFloat(texCoords),Util.toArrayInt(indices),matI);
    }

    private static List<Float> processBitangents(AIMesh aiMesh, List<Float> normals) {
        List<Float> biTangents = new ArrayList<>();
        AIVector3D.Buffer aiBitangents = aiMesh.mBitangents();
        while (aiBitangents != null && aiBitangents.remaining() > 0) {
            AIVector3D aiBitangent = aiBitangents.get();
            biTangents.add(aiBitangent.x());
            biTangents.add(aiBitangent.y());
            biTangents.add(aiBitangent.z());
        }

        if(biTangents.isEmpty())biTangents = new ArrayList<>(Collections.nCopies(normals.size(), 0.0f));
        return biTangents;
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

    private static ModelData.Material processMaterial(AIMaterial material,String texDir){
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D colour = AIColor4D.create();

            Vector4f diffuse = ModelData.Material.DEFAULT_COLOR;
            int res = aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, colour);
            if (res == aiReturn_SUCCESS)diffuse = new Vector4f(colour.r(), colour.g(), colour.b(), colour.a());

            AIString aiTexturePath = AIString.calloc(stack);
            aiGetMaterialTexture(material,aiTextureType_DIFFUSE,0,aiTexturePath,(IntBuffer) null,null,null,null,null,null);
            String texPath = aiTexturePath.dataString();
            if (texPath != null && texPath.length() > 0){
                texPath = texDir + File.separator + new File(texPath).getName();
                diffuse = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
            }

            AIString aiNormalMapPath = AIString.calloc(stack);
            Assimp.aiGetMaterialTexture(material,aiTextureType_NORMALS,0,aiNormalMapPath,(IntBuffer) null,null,null,null,null,null);
            String normalPath = aiNormalMapPath.dataString();
            if (normalPath != null && normalPath.length() > 0) normalPath = texDir + File.separator + new File(normalPath).getName();

            AIString aiMetallicRoughnessPath = AIString.calloc(stack);
            Assimp.aiGetMaterialTexture(material,AI_MATKEY_GLTF_PBRMETALLICROUGHNESS_METALLICROUGHNESS_TEXTURE,0,aiMetallicRoughnessPath,(IntBuffer) null,null,null,null,null,null);
            String roughPath = aiMetallicRoughnessPath.dataString();
            if (roughPath != null && roughPath.length() > 0)roughPath = texDir + File.separator + new File(roughPath).getName();

            float[] metallic = new float[]{0.0f};
            int[] pMax = new int[]{1};
            res = aiGetMaterialFloatArray(material, AI_MATKEY_METALLIC_FACTOR, aiTextureType_NONE, 0, metallic, pMax);
            if (res != aiReturn_SUCCESS)metallic[0] = 1.0f;

            float[] roughness = new float[]{0.0f};
            res = aiGetMaterialFloatArray(material, AI_MATKEY_ROUGHNESS_FACTOR, aiTextureType_NONE, 0, roughness, pMax);
            if (res != aiReturn_SUCCESS)roughness[0] = 1.0f;

            return new ModelData.Material(texPath,normalPath,roughPath,diffuse,roughness[0],metallic[0]);
        }
    }



    private static List<Float> processNormals(AIMesh mesh) {
        List<Float> normals = new ArrayList<>();

        AIVector3D.Buffer aiNormals = mesh.mNormals();
        while (aiNormals != null && aiNormals.remaining() > 0) {
            AIVector3D aiNormal = aiNormals.get();
            normals.add(aiNormal.x());
            normals.add(aiNormal.y());
            normals.add(aiNormal.z());
        }
        return normals;
    }

    private static List<Float> processTangents(AIMesh aiMesh, List<Float> normals) {
        List<Float> tangents = new ArrayList<>();
        AIVector3D.Buffer aiTangents = aiMesh.mTangents();
        while (aiTangents != null && aiTangents.remaining() > 0) {
            AIVector3D aiTangent = aiTangents.get();
            tangents.add(aiTangent.x());
            tangents.add(aiTangent.y());
            tangents.add(aiTangent.z());
        }

        if (tangents.isEmpty()) tangents = new ArrayList<>(Collections.nCopies(normals.size(), 0.0f));

        return tangents;
    }

    private static List<Float> processTexCoords(AIMesh mesh) {
        List<Float> texCoords = new ArrayList<>();
        AIVector3D.Buffer aiTexCoords = mesh.mTextureCoords(0);
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
}
