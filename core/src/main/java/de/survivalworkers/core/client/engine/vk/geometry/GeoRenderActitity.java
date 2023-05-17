package de.survivalworkers.core.client.engine.vk.geometry;

import de.survivalworkers.core.client.engine.vk.pipeline.Pipeline;
import de.survivalworkers.core.client.engine.vk.pipeline.PipelineCache;
import de.survivalworkers.core.client.engine.vk.scene.Entity;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.shaders.ShaderProgram;
import de.survivalworkers.core.client.engine.vk.vertex.Model;
import de.survivalworkers.core.client.engine.vk.vertex.Texture;
import de.survivalworkers.core.client.engine.vk.vertex.VertexBufferStruct;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import  de.survivalworkers.core.client.engine.vk.rendering.*;
import  de.survivalworkers.core.client.engine.vk.rendering.Queue;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class GeoRenderActitity {
    public static final String GEO_FRAGMENT_SHADER = "core/src/main/resources/shader/geo_fragment.frag";
    public static final String GEO_VERTEX_SHADER = "core/src/main/resources/shader/geo_vertex.vert";
    private final Device device;
    private final GeoFrameBuffer geometryFrameBuffer;
    private final int materialSize;
    private final PipelineCache pipelineCache;
    private final Scene scene;

    private CommandBuffer[] commandBuffers;
    private DescriptorPool descriptorPool;
    private Map<String, TextureDescriptorSet> descriptorSetMap;
    private Fence[] fences;
    private DescriptorSetLayout[] geometryDescriptorSetLayouts;
    private DescriptorSetLayout.DynUniformDescriptorSetLayout materialDescriptorSetLayout;
    private Buffer materialsBuffer;
    private DescriptorSet.DynUniformDescriptorSet materialsDescriptorSet;
    private Pipeline pipeLine;
    private DescriptorSet.UniformDescriptorSet projMatrixDescriptorSet;
    private Buffer projMatrixUniform;
    private ShaderProgram shaderProgram;
    private SwapChain swapChain;
    private DescriptorSetLayout.SamplerDescriptorSetLayout textureDescriptorSetLayout;
    private TextureSampler textureSampler;
    private DescriptorSetLayout.UniformDescriptorSetLayout uniformDescriptorSetLayout;
    private Buffer[] viewMatricesBuffer;
    private DescriptorSet.UniformDescriptorSet[] viewMatricesDescriptorSets;

    public GeoRenderActitity(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene) {
        this.swapChain = swapChain;
        this.pipelineCache = pipelineCache;
        this.scene = scene;
        device = swapChain.getDevice();
        geometryFrameBuffer = new GeoFrameBuffer(swapChain);
        int numImages = swapChain.getNumImages();
        materialSize = calcMaterialsUniformSize();
        createShaders();
        createDescriptorPool();
        createDescriptorSets(numImages);
        createPipeline();
        createCommandBuffers(commandPool, numImages);
        projMatrixUniform.copyMatrixToBuffer(scene.getProjection().getProjectionMatrix());
    }

    private int calcMaterialsUniformSize() {
        PhysicalDevice physDevice = device.getPhysicalDevice();
        long minUboAlignment = physDevice.getPhysicalDeviceProperties().limits().minUniformBufferOffsetAlignment();
        long mult = (16 * 9) / minUboAlignment + 1;
        return (int) (mult * minUboAlignment);
    }

    public void close() {
        pipeLine.close();
        materialsBuffer.close();
        Arrays.stream(viewMatricesBuffer).forEach(Buffer::close);
        projMatrixUniform.close();
        textureSampler.close();
        materialDescriptorSetLayout.close();
        textureDescriptorSetLayout.close();
        uniformDescriptorSetLayout.close();
        descriptorPool.close();
        shaderProgram.close();
        geometryFrameBuffer.getGeometryRenderPass();
        Arrays.stream(commandBuffers).forEach(CommandBuffer::close);
        Arrays.stream(fences).forEach(Fence::close);
    }

    private void createCommandBuffers(CommandPool commandPool, int numImages) {
        commandBuffers = new CommandBuffer[numImages];
        fences = new Fence[numImages];

        for (int i = 0; i < numImages; i++) {
            commandBuffers[i] = new CommandBuffer(commandPool, true, false);
            fences[i] = new Fence(device, true);
        }
    }

    private void createDescriptorPool() {
        List<DescriptorPool.DescriptorTypeCount> descriptorTypeCounts = new ArrayList<>();
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(swapChain.getNumImages() + 1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER));
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(500 * 3, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER));
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC));
        descriptorPool = new DescriptorPool(device, descriptorTypeCounts);
    }

    private void createDescriptorSets(int numImages) {
        uniformDescriptorSetLayout = new DescriptorSetLayout.UniformDescriptorSetLayout(device, 0, VK_SHADER_STAGE_VERTEX_BIT);
        textureDescriptorSetLayout = new DescriptorSetLayout.SamplerDescriptorSetLayout(device, 0, VK_SHADER_STAGE_FRAGMENT_BIT);
        materialDescriptorSetLayout = new DescriptorSetLayout.DynUniformDescriptorSetLayout(device, 0, VK_SHADER_STAGE_FRAGMENT_BIT);
        geometryDescriptorSetLayouts = new DescriptorSetLayout[]{uniformDescriptorSetLayout, uniformDescriptorSetLayout, textureDescriptorSetLayout, textureDescriptorSetLayout, textureDescriptorSetLayout,
                materialDescriptorSetLayout,};

        descriptorSetMap = new HashMap<>();
        textureSampler = new TextureSampler(device, 1);
        projMatrixUniform = new Buffer(device, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        projMatrixDescriptorSet = new DescriptorSet.UniformDescriptorSet(descriptorPool, uniformDescriptorSetLayout, projMatrixUniform, 0);

        viewMatricesDescriptorSets = new DescriptorSet.UniformDescriptorSet[numImages];
        viewMatricesBuffer = new Buffer[numImages];
        materialsBuffer = new Buffer(device, (long) materialSize * 500, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        materialsDescriptorSet = new DescriptorSet.DynUniformDescriptorSet(descriptorPool, materialDescriptorSetLayout, materialsBuffer, 0, materialSize);
        for (int i = 0; i < numImages; i++) {
            viewMatricesBuffer[i] = new Buffer(device, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
            viewMatricesDescriptorSets[i] = new DescriptorSet.UniformDescriptorSet(descriptorPool, uniformDescriptorSetLayout,
                    viewMatricesBuffer[i], 0);
        }
    }

    private void createPipeline() {
        Pipeline.PipeLineCreateInfo pipeLineCreationInfo = new Pipeline.PipeLineCreateInfo(geometryFrameBuffer.getGeometryRenderPass().getVkRenderPass(), shaderProgram, 3, true
                , 64, new VertexBufferStruct(), geometryDescriptorSetLayouts,true);
        pipeLine = new Pipeline(pipelineCache, pipeLineCreationInfo);
        pipeLineCreationInfo.close();
    }

    private void createShaders() {
        shaderProgram = new ShaderProgram(device, new ShaderProgram.ShaderModuleData[]
                {new ShaderProgram.ShaderModuleData(VK_SHADER_STAGE_VERTEX_BIT, GEO_VERTEX_SHADER + ".spv"), new ShaderProgram.ShaderModuleData(VK_SHADER_STAGE_FRAGMENT_BIT, GEO_FRAGMENT_SHADER + ".spv"),});
    }

    public List<Attachment> getAttachments() {
        return geometryFrameBuffer.getGeometryAttachments().getAttachments();
    }

    public void recordCommandBuffer(List<Model> ModelList) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();
            int idx = swapChain.getCurrentFrame();

            FrameBuffer frameBuffer = geometryFrameBuffer.getFrameBuffer();
            Fence fence = fences[idx];
            CommandBuffer commandBuffer = commandBuffers[idx];

            fence.fenceWait();
            fence.reset();

            commandBuffer.reset();
            List<Attachment> attachments = geometryFrameBuffer.getGeometryAttachments().getAttachments();
            VkClearValue.Buffer clearValues = VkClearValue.calloc(attachments.size(), stack);
            for (Attachment attachment : attachments) {
                if (attachment.isDepthAttachment()) clearValues.apply(v -> v.depthStencil().depth(1.0f));
                else clearValues.apply(v -> v.color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 1));

            }
            clearValues.flip();

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack).sType$Default().renderPass(geometryFrameBuffer.getGeometryRenderPass().getVkRenderPass()).pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height)).framebuffer(frameBuffer.getFrameBuffer());

            commandBuffer.beginRec();
            VkCommandBuffer cmdHandle = commandBuffer.getCmdBuf();
            vkCmdBeginRenderPass(cmdHandle, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeLine.getPipeline());

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack).x(0).y(height).height(-height).width(width).minDepth(0.0f).maxDepth(1.0f);
            vkCmdSetViewport(cmdHandle, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack).extent(it -> it.width(width).height(height)).offset(it -> it.x(0).y(0));
            vkCmdSetScissor(cmdHandle, 0, scissor);

            LongBuffer descriptorSets = stack.mallocLong(6).put(0, projMatrixDescriptorSet.getDescriptorSet()).put(1, viewMatricesDescriptorSets[idx].getDescriptorSet())
                    .put(5, materialsDescriptorSet.getDescriptorSet());
            viewMatricesBuffer[idx].copyMatrixToBuffer(scene.getCamera().getViewMatrix());


            recordEntities(stack, cmdHandle, descriptorSets, ModelList);

            vkCmdEndRenderPass(cmdHandle);
            commandBuffer.endRec();
        }
    }

    private void recordEntities(MemoryStack stack, VkCommandBuffer cmdHandle, LongBuffer descriptorSets, List<Model> ModelList) {
        LongBuffer offsets = stack.mallocLong(1);
        offsets.put(0, 0L);
        LongBuffer vertexBuffer = stack.mallocLong(1);
        IntBuffer dynDescrSetOffset = stack.callocInt(1);
        int materialCount = 0;
        for (Model Model : ModelList) {
            String modelId = Model.getId();
            List<Entity> entities = scene.get(modelId);
            if (entities.isEmpty()) {
                materialCount += Model.getMaterials().size();
                continue;
            }
            for (de.survivalworkers.core.client.engine.vk.vertex.Model.Material material : Model.getMaterials()) {
                int materialOffset = materialCount * materialSize;
                dynDescrSetOffset.put(0, materialOffset);
                TextureDescriptorSet textureDescriptorSet = descriptorSetMap.get(material.texture().getFileName());
                TextureDescriptorSet normalMapDescriptorSet = descriptorSetMap.get(material.normalTex().getFileName());
                TextureDescriptorSet metalRoughDescriptorSet = descriptorSetMap.get(material.metalRoughTex().getFileName());

                for (de.survivalworkers.core.client.engine.vk.vertex.Model.Mesh mesh : material.meshes()) {
                    vertexBuffer.put(0, mesh.verticesBuffer().getBuffer());
                    vkCmdBindVertexBuffers(cmdHandle, 0, vertexBuffer, offsets);
                    vkCmdBindIndexBuffer(cmdHandle, mesh.indicesBuffer().getBuffer(), 0, VK_INDEX_TYPE_UINT32);

                    for (Entity entity : entities) {
                        descriptorSets.put(2, textureDescriptorSet.getDescriptorSet());
                        descriptorSets.put(3, normalMapDescriptorSet.getDescriptorSet());
                        descriptorSets.put(4, metalRoughDescriptorSet.getDescriptorSet());
                        vkCmdBindDescriptorSets(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeLine.getPipelineLayout(), 0, descriptorSets, dynDescrSetOffset);

                        pipeLine.setMAtrixAsPushConstant( cmdHandle, entity.getModelMatrix());
                        vkCmdDrawIndexed(cmdHandle, mesh.numIndices(), 1, 0, 0, 0);
                    }
                }
                materialCount++;
            }
        }
    }

    public void registerModels(List<Model> ModelList) {
        device.waitIdle();
        int materialCount = 0;
        for (Model Model : ModelList) {
            for (de.survivalworkers.core.client.engine.vk.vertex.Model.Material material : Model.getMaterials()) {
                int materialOffset = materialCount * materialSize;
                updateTextureDescriptorSet(material.texture());
                updateTextureDescriptorSet(material.normalTex());
                updateTextureDescriptorSet(material.metalRoughTex());
                updateMaterialsBuffer(materialsBuffer, material, materialOffset);
                materialCount++;
            }
        }
    }

    public void resize(SwapChain swapChain) {
        projMatrixUniform.copyMatrixToBuffer( scene.getProjection().getProjectionMatrix());
        this.swapChain = swapChain;
        geometryFrameBuffer.resize(swapChain);
    }

    public void submit(Queue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = commandBuffers[idx];
            Fence currentFence = fences[idx];
            SwapChain.SyncSemaphores syncSemaphores = swapChain.getSyncSemaphoresList()[idx];
            queue.submit(stack.pointers(commandBuffer.getCmdBuf()), stack.longs(syncSemaphores.imgAcquisitionSemaphore().getSemaphore()), stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                    stack.longs(syncSemaphores.geoCompleteSemaphore().getSemaphore()), currentFence);
        }
    }

    private void updateMaterialsBuffer(Buffer Buffer, Model.Material material, int offset) {
        long mappedMemory = Buffer.map();
        ByteBuffer materialBuffer = MemoryUtil.memByteBuffer(mappedMemory, (int) Buffer.getRequestedSize());
        material.diffuseColor().get(offset, materialBuffer);
        materialBuffer.putFloat(offset + 4 * 4, material.hasTexture() ? 1.0f : 0.0f);
        materialBuffer.putFloat(offset + 4 * 5, material.hasNormalTex() ? 1.0f : 0.0f);
        materialBuffer.putFloat(offset + 4 * 6, material.hasMetalRoughTex() ? 1.0f : 0.0f);
        materialBuffer.putFloat(offset + 4 * 7, material.roughness());
        materialBuffer.putFloat(offset + 4 * 8, material.metallic());
        Buffer.unMap();
    }

    private void updateTextureDescriptorSet(Texture texture) {
        String textureFileName = texture.getFileName();
        TextureDescriptorSet textureDescriptorSet = descriptorSetMap.get(textureFileName);
        if (textureDescriptorSet == null) {
            textureDescriptorSet = new TextureDescriptorSet(descriptorPool, textureDescriptorSetLayout, texture, textureSampler, 0);
            descriptorSetMap.put(textureFileName, textureDescriptorSet);
        }
    }
}
