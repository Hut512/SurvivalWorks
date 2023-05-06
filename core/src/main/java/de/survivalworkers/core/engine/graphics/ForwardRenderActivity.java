package de.survivalworkers.core.engine.graphics;

import de.survivalworkers.core.engine.graphics.pipeline.Pipeline;
import de.survivalworkers.core.engine.graphics.pipeline.PipelineCache;
import de.survivalworkers.core.engine.graphics.rendering.*;
import de.survivalworkers.core.engine.graphics.scene.Entity;
import de.survivalworkers.core.engine.graphics.scene.Scene;
import de.survivalworkers.core.engine.graphics.shaders.ShaderProgram;
import de.survivalworkers.core.engine.graphics.vertex.Model;
import de.survivalworkers.core.engine.graphics.vertex.Texture;
import de.survivalworkers.core.engine.graphics.vertex.VertexBufferStruct;
import de.survivalworkers.core.vk.device.LogicalDevice;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class ForwardRenderActivity {
    public static final String VERTEX_SHADER = "core/src/main/resources/shader/vertex.vert";
    public static final String FRAGMENT_SHADER = "core/src/main/resources/shader/fragment.frag";

    private final LogicalDevice device;
    private final CommandBuffer[] commandBuffers;
    private final Fence[] fences;
    private final ShaderProgram shaderProgram;
    private final int materialSize;
    private final Pipeline pipeLine;
    private final PipelineCache pipelineCache;
    private final RenderPass renderPass;
    private final Scene scene;

    private Attachment[] depthAttachments;
    private DescriptorPool descriptorPool;
    private DescriptorSetLayout[] descriptorSetLayouts;
    private Map<String, TextureDescriptorSet> descriptorSetMap;
    private FrameBuffer[] frameBuffers;
    private DescriptorSetLayout.DynUniformDescriptorSetLayout descriptorSetLayout;
    private DescriptorSet.UniformDescriptorSet projMatrixDescriptorSet;
    private Buffer projMatrixUniform;
    private SwapChain swapChain;
    private DescriptorSetLayout.SamplerDescriptorSetLayout textureDescriptorSetLayout;
    private TextureSampler textureSampler;
    private Buffer[] matreciesBuffer;
    private DescriptorSetLayout.UniformDescriptorSetLayout uniformDescriptorSetLayout;
    private DescriptorSetLayout.DynUniformDescriptorSetLayout materialDescriptorLayout;
    private DescriptorSet.UniformDescriptorSet[] viewMatricesDescriptorSet;
    private Buffer[] viewMatricesBuffer;
    private Buffer materialBuffer;
    private DescriptorSet.DynUniformDescriptorSet materialDescriptorSet;

    public ForwardRenderActivity(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene) {
        this.swapChain = swapChain;
        this.pipelineCache = pipelineCache;
        this.scene = scene;
        device = swapChain.getLogicalDevice();

        int numImages = swapChain.getImageViews().length;
        materialSize = calcMaterials();
        createDepthImages();
        renderPass = new RenderPass(swapChain, depthAttachments[0].getImage().getFormat());
        createFrameBuffers();

        shaderProgram = new ShaderProgram(device,new ShaderProgram.ShaderModuleData[]{new ShaderProgram.ShaderModuleData(VK_SHADER_STAGE_VERTEX_BIT,VERTEX_SHADER + ".spv"),
                new ShaderProgram.ShaderModuleData(VK_SHADER_STAGE_FRAGMENT_BIT,FRAGMENT_SHADER + ".spv")});
        createDescriptorSets(numImages);

        Pipeline.PipeLineCreateInfo pipeLineCreationInfo = new Pipeline.PipeLineCreateInfo(renderPass.getRenderPass(), shaderProgram, 1, true, 64, new VertexBufferStruct(), descriptorSetLayouts,
                true);
        pipeLine = new Pipeline(this.pipelineCache, pipeLineCreationInfo);
        pipeLineCreationInfo.descriptorLayouts();

        commandBuffers = new CommandBuffer[numImages];
        fences = new Fence[numImages];
        for (int i = 0; i < numImages; i++) {
            commandBuffers[i] = new CommandBuffer(commandPool, true, false);
            fences[i] = new Fence(device, true);
        }
        Util.copyMatrixToBuffer(projMatrixUniform, scene.getProjection().getProjectionMatrix());
    }

    private int calcMaterials() {
        //long ubo = device.limits().minUniformBufferOffsetAlignment(); //TODO: fix
        long ubo = 1;
        long mult = 144 / ubo + 1;
        return (int) (mult * ubo);
    }

    public void close() {
        projMatrixUniform.close();
        textureSampler.close();
        descriptorPool.close();
        pipeLine.close();
        Arrays.stream(descriptorSetLayouts).forEach(DescriptorSetLayout::close);
        Arrays.stream(depthAttachments).forEach(Attachment::close);
        shaderProgram.close();
        Arrays.stream(frameBuffers).forEach(FrameBuffer::close);
        renderPass.close();
        Arrays.stream(commandBuffers).forEach(CommandBuffer::close);
        Arrays.stream(fences).forEach(Fence::close);
        materialBuffer.close();
        Arrays.stream(viewMatricesBuffer).forEach(Buffer::close);
        materialDescriptorLayout.close();
    }

    private void createDepthImages() {
        int numImages = swapChain.getNumImages();
        final int width = swapChain.getWidth();
        final int height = swapChain.getHeight();
        depthAttachments = new Attachment[numImages];
        for (int i = 0; i < numImages; i++) {
            depthAttachments[i] = new Attachment(device,width ,height ,
                    VK_FORMAT_D32_SFLOAT, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        }
    }

    private void createDescriptorSets(int numImages) {
        uniformDescriptorSetLayout = new DescriptorSetLayout.UniformDescriptorSetLayout(device, 0, VK_SHADER_STAGE_VERTEX_BIT);
        textureDescriptorSetLayout = new DescriptorSetLayout.SamplerDescriptorSetLayout(device, 0, VK_SHADER_STAGE_FRAGMENT_BIT);
        materialDescriptorLayout = new DescriptorSetLayout.DynUniformDescriptorSetLayout(device,0, VK_SHADER_STAGE_FRAGMENT_BIT);
        descriptorSetLayouts = new DescriptorSetLayout[]{uniformDescriptorSetLayout,uniformDescriptorSetLayout ,textureDescriptorSetLayout,materialDescriptorLayout};

        List<DescriptorPool.DescriptorTypeCount> descriptorTypeCounts = new ArrayList<>();
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(swapChain.getNumImages() + 1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER));
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(/*Properties.getInstance().getMaxMaterials()*/10, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER));
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC));
        descriptorPool = new DescriptorPool(device, descriptorTypeCounts);
        descriptorSetMap = new HashMap<>();
        textureSampler = new TextureSampler(device, 1);
        projMatrixUniform = new Buffer(device, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        projMatrixDescriptorSet = new DescriptorSet.UniformDescriptorSet(descriptorPool, uniformDescriptorSetLayout, projMatrixUniform, 0);
        
        viewMatricesDescriptorSet = new DescriptorSet.UniformDescriptorSet[numImages];
        viewMatricesBuffer = new Buffer[numImages];
        materialBuffer = new Buffer(device, (long) materialSize * /*Properties.getInstance().getMaxMaterials()*/10, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        materialDescriptorSet = new DescriptorSet.DynUniformDescriptorSet(descriptorPool,materialDescriptorLayout,materialBuffer,0,materialSize);
        for (int i = 0; i < numImages; i++) {
            log.debug(String.valueOf(i));
            viewMatricesBuffer[i] = new Buffer(device,64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
            viewMatricesDescriptorSet[i] = new DescriptorSet.UniformDescriptorSet(descriptorPool,uniformDescriptorSetLayout,viewMatricesBuffer[i],0);
        }
    }

    private void createFrameBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ImageView[] imageViews = swapChain.getImageViews();
            int numImages = imageViews.length;

            LongBuffer pAttachments = stack.mallocLong(2);
            frameBuffers = new FrameBuffer[numImages];
            for (int i = 0; i < numImages; i++) {
                pAttachments.put(0, imageViews[i].getImgView());
                pAttachments.put(1, depthAttachments[i].getImageView().getImgView());
                frameBuffers[i] = new FrameBuffer(device, swapChain.getWidth(), swapChain.getHeight(),
                        pAttachments, renderPass.getRenderPass());
            }
        }
    }

    public void recordCommandBuffer(List<Model> vulkanModelList) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = swapChain.getCreateInfo().imageExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();
            int idx = swapChain.getCurrentFrame();

            Fence fence = fences[idx];
            CommandBuffer commandBuffer = commandBuffers[idx];
            FrameBuffer frameBuffer = frameBuffers[idx];

            fence.fenceWait();
            fence.reset();

            commandBuffer.reset();
            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1));
            clearValues.apply(1, v -> v.depthStencil().depth(1.0f));

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).renderPass(renderPass.getRenderPass()).pClearValues(clearValues).
                    renderArea(a -> a.extent().set(width, height)).framebuffer(frameBuffer.getFrameBuffer());

            commandBuffer.beginRec();
            VkCommandBuffer cmdHandle = commandBuffer.getCmdBuf();
            vkCmdBeginRenderPass(cmdHandle, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeLine.getPipeline());

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack).x(0).y(height).height(-height).width(width).minDepth(0.0f).maxDepth(1.0f);
            vkCmdSetViewport(cmdHandle, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack).extent(it -> it.width(width).height(height)).offset(it -> it.x(0).y(0));
            vkCmdSetScissor(cmdHandle, 0, scissor);

            LongBuffer descriptorSet = stack.mallocLong(4).put(0,projMatrixDescriptorSet.getHandle()).put(1,viewMatricesDescriptorSet[idx].getHandle()).
                    put(3,materialDescriptorLayout.getDescriptorLayout());
            Util.copyMatrixToBuffer(viewMatricesBuffer[idx],scene.getCamera().getViewMatrix());

            recordEntities(stack,cmdHandle,descriptorSet,vulkanModelList);

            vkCmdEndRenderPass(cmdHandle);
            commandBuffer.endRec();
        }
    }

    private void recordEntities(MemoryStack stack,VkCommandBuffer buffer,LongBuffer lp,List<Model> models){
        LongBuffer offsets = stack.mallocLong(1);
        offsets.put(0, 0L);
        LongBuffer vertexBuffer = stack.mallocLong(1);
        IntBuffer dynDescOffset = stack.callocInt(1);
        int i = 0;
        for (Model vulkanModel : models) {
            String modelId = vulkanModel.getModelId();
            List<Entity> entities = scene.get(modelId);
            if (entities.isEmpty()) {
                i += vulkanModel.getMaterials().size();
                continue;
            }
            for (Model.Material material : vulkanModel.getMaterials()) {
                if (material.meshes().isEmpty()){
                    i++;
                    continue;
                }
                dynDescOffset.put(0,i * materialSize);
                TextureDescriptorSet textureDescriptorSet = descriptorSetMap.get(material.texture().getFileName());
                lp.put(1, textureDescriptorSet.getHandle());

                for (Model.Mesh mesh : material.meshes()) {
                    vertexBuffer.put(0, mesh.verticesBuffer().getBuffer());
                    vkCmdBindVertexBuffers(buffer, 0, vertexBuffer, offsets);
                    vkCmdBindIndexBuffer(buffer, mesh.indicesBuffer().getBuffer(), 0, VK_INDEX_TYPE_UINT32);

                    for (Entity entity : entities) {
                        vkCmdBindDescriptorSets(buffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipeLine.getPipelineLayout(), 0, lp, null);

                        Util.setMAtrixAsPushConstant(pipeLine, buffer, entity.getModelMatrix());
                        vkCmdDrawIndexed(buffer, mesh.numIndices(), 1, 0, 0, 0);
                    }
                }
            }
        }
    }

    public void registerModels(List<Model> models) {
        device.waitIdle();
        for (Model model : models) {
            for (int i = 0; i < model.getMaterials().size(); i++) {
                updateTextureDescriptorSet(model.getMaterials().get(i).texture());
                updateMaterialBuffer(materialBuffer,model.getMaterials().get(i),materialSize * i);
            }
        }
    }

    private void updateMaterialBuffer(Buffer materialBuffer, Model.Material material, int i) {
        long memory = materialBuffer.map();
        ByteBuffer buffer = MemoryUtil.memByteBuffer(memory, (int) materialBuffer.getRequestedSize());
        material.diffuseColor().get(i,buffer);
        materialBuffer.unMap();
    }

    public void resize(SwapChain swapChain) {
        Util.copyMatrixToBuffer(projMatrixUniform, scene.getProjection().getProjectionMatrix());
        this.swapChain = swapChain;
        Arrays.stream(frameBuffers).forEach(FrameBuffer::close);
        Arrays.stream(depthAttachments).forEach(Attachment::close);
        createDepthImages();
        createFrameBuffers();
    }

    public void submit(VkQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = commandBuffers[idx];
            Fence currentFence = fences[idx];
            SwapChain.SyncSemaphores syncSemaphores = swapChain.getSyncSemaphoresList()[idx];
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType$Default()
                    .pCommandBuffers(stack.pointers(commandBuffer.getCmdBuf()))
                    .pSignalSemaphores(stack.longs(syncSemaphores.imgAcquisitionSemaphore().getHandle()))
                    .pWaitSemaphores(stack.longs(syncSemaphores.renderCompleteSemaphore().getHandle()))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            vkQueueSubmit(queue, submitInfo, currentFence.getHandle());
        }
    }

    private void updateTextureDescriptorSet(Texture texture) {
        String textureFileName = texture.getFileName();
        TextureDescriptorSet textureDescriptorSet = descriptorSetMap.get(textureFileName);
        if (textureDescriptorSet == null) {
            log.debug(texture.getFileName());
            textureDescriptorSet = new TextureDescriptorSet(descriptorPool, textureDescriptorSetLayout, texture, textureSampler, 0);
            descriptorSetMap.put(textureFileName, textureDescriptorSet);
        }
    }
}
