package de.survivalworks.core.client.engine.vk.vertex;

import de.survivalworks.core.client.engine.vk.rendering.*;
import de.survivalworks.core.client.engine.vk.rendering.Queue;
import de.survivalworks.core.engine.vk.rendering.*;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCopy;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class Model {
    private final String modelId;
    private final List<Material> materials;

    public Model(String modelId) {
        this.modelId = modelId;
        materials = new ArrayList<>();
    }

    private static TransferBuffers createIndicesBuffers(Device device, ModelData.MeshData meshData) {
        int[] indices = meshData.indices();
        int numIndices = indices.length;
        int bufferSize = numIndices * 4;

        Buffer srcBuffer = new Buffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        Buffer dstBuffer = new Buffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        long mappedMemory = srcBuffer.map();
        IntBuffer data = MemoryUtil.memIntBuffer(mappedMemory, (int) srcBuffer.getRequestedSize());
        data.put(indices);
        srcBuffer.unMap();

        return new TransferBuffers(srcBuffer, dstBuffer);
    }

    private static TransferBuffers createVerticesBuffers(Device device, ModelData.MeshData meshData) {
        float[] positions = meshData.pos();
        float[] textCoords = meshData.texCords();
        if (textCoords == null || textCoords.length == 0) {
            textCoords = new float[(positions.length / 3) * 2];
        }
        int numElements = positions.length + textCoords.length;
        int bufferSize = numElements * 4;

        Buffer srcBuffer = new Buffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        Buffer dstBuffer = new Buffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        long mappedMemory = srcBuffer.map();
        FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int) srcBuffer.getRequestedSize());

        int rows = positions.length / 3;
        for (int row = 0; row < rows; row++) {
            int startPos = row * 3;
            int startTextCoord = row * 2;
            data.put(positions[startPos]);
            data.put(positions[startPos + 1]);
            data.put(positions[startPos + 2]);
            data.put(textCoords[startTextCoord]);
            data.put(textCoords[startTextCoord + 1]);
        }

        srcBuffer.unMap();

        return new TransferBuffers(srcBuffer, dstBuffer);
    }

    private static void recordTransferCommand(CommandBuffer cmd, TransferBuffers transferBuffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                    .srcOffset(0).dstOffset(0).size(transferBuffers.srcBuffer().getRequestedSize());
            VK13.vkCmdCopyBuffer(cmd.getCmdBuf(), transferBuffers.srcBuffer().getBuffer(),
                    transferBuffers.dstBuffer().getBuffer(), copyRegion);
        }
    }

    public static List<Model> transformModels(List<ModelData> modelDataList, TextureCache texCache, CommandPool commandPool, Queue queue) {
        List<Model> models = new ArrayList<>();
        Device device = commandPool.getDevice();
        CommandBuffer cmd = new CommandBuffer(commandPool, true, true);
        List<Buffer> stagingBufferList = new ArrayList<>();
        List<Texture> textures = new ArrayList<>();

        cmd.beginRec();

        for (ModelData modelData : modelDataList) {
            Model model = new Model(modelData.getId());
            models.add(model);

            Material defaultMat = null;
            modelData.getMaterials().forEach((material) -> {
                Material material1 = transformMaterial(material,device,texCache,cmd,textures);
                model.materials.add(material1);
            });

            for (ModelData.MeshData meshData : modelData.getMeshData()) {
                TransferBuffers verticesBuffers = createVerticesBuffers(device, meshData);
                TransferBuffers indicesBuffers = createIndicesBuffers(device, meshData);
                stagingBufferList.add(verticesBuffers.srcBuffer());
                stagingBufferList.add(indicesBuffers.srcBuffer());
                recordTransferCommand(cmd, verticesBuffers);
                recordTransferCommand(cmd, indicesBuffers);

                Mesh mesh = new Mesh(verticesBuffers.dstBuffer(), indicesBuffers.dstBuffer(), meshData.indices().length);
                Material material;
                int matI = meshData.materialI();
                if(matI >= 0 && matI < model.materials.size()) material = model.materials.get(matI);
                else {
                    if(defaultMat == null)defaultMat = transformMaterial(new ModelData.Material(),device,texCache,cmd,textures);
                    material = defaultMat;
                }
                material.meshes.add(mesh);
            }
        }

        cmd.endRec();
        Fence fence = new Fence(device, true);
        fence.reset();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            queue.submit(stack.pointers(cmd.getCmdBuf()), null, null, null, fence);
        }
        fence.fenceWait();
        fence.delete();
        cmd.delete();

        stagingBufferList.forEach(Buffer::delete);

        return models;
    }

    private static Material transformMaterial(ModelData.Material material,Device device,TextureCache cache,CommandBuffer cmdBuf,List<Texture> textures){
        Texture tex = cache.createTexture(device,material.texPath(),VK13.VK_FORMAT_R8G8B8A8_SRGB);
        boolean hasTex = material.texPath() != null && material.texPath().trim().length() > 0;
        if(hasTex){
            tex.recordTextureTransition(cmdBuf);
            textures.add(tex);
        }

        return new Material(material.diffuseColor(),tex,hasTex,new ArrayList<>());
    }

    public void delete() {
        materials.forEach(material -> material.meshes.forEach(Mesh::delete));
    }

    public String getModelId() {
        return modelId;
    }

    public List<Material> getMaterials() {
        return materials;
    }

    private record TransferBuffers(Buffer srcBuffer, Buffer dstBuffer) {
    }

    public record Material(Vector4f diffuseColor,Texture texture,boolean hasTexture,List<Mesh> meshes){
        public boolean isTransparent(){
            return texture.isTransparent();
        }
    }

    public record Mesh(Buffer verticesBuffer, Buffer indicesBuffer, int numIndices) {
        public void delete() {
            verticesBuffer.delete();
            indicesBuffer.delete();
        }
    }
}
