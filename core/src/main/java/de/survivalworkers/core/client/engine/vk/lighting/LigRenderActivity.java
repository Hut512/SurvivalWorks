package de.survivalworkers.core.client.engine.vk.lighting;

import de.survivalworkers.core.client.engine.vk.pipeline.Pipeline;
import de.survivalworkers.core.client.engine.vk.pipeline.PipelineCache;
import de.survivalworkers.core.client.engine.vk.rendering.Buffer;
import de.survivalworkers.core.client.engine.vk.scene.Light;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.shaders.ShaderProgram;
import org.joml.*;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;
import  de.survivalworkers.core.client.engine.vk.rendering.Queue;
import  de.survivalworkers.core.client.engine.vk.rendering.*;

import java.nio.*;
import java.util.*;

import static org.lwjgl.vulkan.VK11.*;

public class LigRenderActivity {

    private static final String LIGHTING_FRAGMENT_SHADER_FILE_GLSL = "core/src/main/resources/shader/lightning_frag.frag";
    private static final String LIGHTING_FRAGMENT_SHADER_FILE_SPV = LIGHTING_FRAGMENT_SHADER_FILE_GLSL + ".spv";
    private static final String LIGHTING_VERTEX_SHADER_FILE_GLSL = "core/src/main/resources/shader/lightning_vert.vert";
    private static final String LIGHTING_VERTEX_SHADER_FILE_SPV = LIGHTING_VERTEX_SHADER_FILE_GLSL + ".spv";

    private final Vector4f auxVec;
    private final Device device;
    private final LigFrameBuffer lightingFrameBuffer;
    private final Scene scene;

    private AttachmentsDescriptorSet attachmentsDescriptorSet;
    private AttachmentsLayout attachmentsLayout;
    private CommandBuffer[] commandBuffers;
    private DescriptorPool descriptorPool;
    private DescriptorSetLayout[] descriptorSetLayouts;
    private Fence[] fences;
    private Buffer invProjBuffer;
    private DescriptorSet.UniformDescriptorSet invProjMatrixDescriptorSet;
    private Buffer[] lightsBuffers;
    private DescriptorSet.UniformDescriptorSet[] lightsDescriptorSets;
    private Pipeline pipeline;
    private ShaderProgram shaderProgram;
    private SwapChain swapChain;
    private DescriptorSetLayout.UniformDescriptorSetLayout uniformDescriptorSetLayout;
    private int bLights = 0;

    public LigRenderActivity(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, List<Attachment> attachments, Scene scene) {
        this.swapChain = swapChain;
        this.scene = scene;
        device = swapChain.getDevice();
        auxVec = new Vector4f();

        lightingFrameBuffer = new LigFrameBuffer(swapChain);
        int numImages = swapChain.getNumImages();
        createShaders();
        createDescriptorPool(attachments);
        createUniforms(numImages);
        createDescriptorSets(attachments, numImages);
        createPipeline(pipelineCache);
        createCommandBuffers(commandPool, numImages);
        updateInvProjMatrix();

        for (int i = 0; i < numImages; i++) {
            preRecordCommandBuffer(i);
        }
    }

    public void close() {
        uniformDescriptorSetLayout.close();
        attachmentsDescriptorSet.close();
        attachmentsLayout.close();
        descriptorPool.close();
        Arrays.stream(lightsBuffers).forEach(Buffer::close);
        pipeline.close();
        invProjBuffer.close();
        lightingFrameBuffer.close();
        shaderProgram.close();
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

    private void createDescriptorPool(List<Attachment> attachments) {
        List<DescriptorPool.DescriptorTypeCount> descriptorTypeCounts = new ArrayList<>();
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(attachments.size(), VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER));
        descriptorTypeCounts.add(new DescriptorPool.DescriptorTypeCount(swapChain.getNumImages() + 1, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER));
        descriptorPool = new DescriptorPool(device, descriptorTypeCounts);
    }

    private void createDescriptorSets(List<Attachment> attachments, int numImages) {
        attachmentsLayout = new AttachmentsLayout(device, attachments.size());
        uniformDescriptorSetLayout = new DescriptorSetLayout.UniformDescriptorSetLayout(device, 0, VK_SHADER_STAGE_FRAGMENT_BIT);
        descriptorSetLayouts = new DescriptorSetLayout[]{attachmentsLayout, uniformDescriptorSetLayout, uniformDescriptorSetLayout,};

        attachmentsDescriptorSet = new AttachmentsDescriptorSet(descriptorPool, attachmentsLayout, attachments, 0);
        invProjMatrixDescriptorSet = new DescriptorSet.UniformDescriptorSet(descriptorPool, uniformDescriptorSetLayout, invProjBuffer, 0);

        lightsDescriptorSets = new DescriptorSet.UniformDescriptorSet[numImages];
        for (int i = 0; i < numImages; i++) {
            lightsDescriptorSets[i] = new DescriptorSet.UniformDescriptorSet(descriptorPool, uniformDescriptorSetLayout,
                    lightsBuffers[i], 0);
        }
    }

    private void createPipeline(PipelineCache pipelineCache) {
        Pipeline.PipeLineCreateInfo pipeLineCreationInfo = new Pipeline.PipeLineCreateInfo(lightingFrameBuffer.getLightingRenderPass().getVkRenderPass(), shaderProgram, 1, false,
                0, new EmptyVertexBufferStruct(), descriptorSetLayouts,false);
        pipeline = new Pipeline(pipelineCache, pipeLineCreationInfo);
        pipeLineCreationInfo.close();
    }

    private void createShaders() {
        shaderProgram = new ShaderProgram(device, new ShaderProgram.ShaderModuleData[]
                {new ShaderProgram.ShaderModuleData(VK_SHADER_STAGE_VERTEX_BIT, LIGHTING_VERTEX_SHADER_FILE_SPV), new ShaderProgram.ShaderModuleData(VK_SHADER_STAGE_FRAGMENT_BIT, LIGHTING_FRAGMENT_SHADER_FILE_SPV),});
    }

    private void createUniforms(int numImages) {
        invProjBuffer = new Buffer(device, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);

        lightsBuffers = new Buffer[numImages];
        for (int i = 0; i < numImages; i++) {
            //INFO replace with max lights or variable size (current 10)
            lightsBuffers[i] = new Buffer(device, (long) 4 * 4 + 16 * 2 * 10 + 16, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        }
    }

    public void preRecordCommandBuffer(int idx) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();

            FrameBuffer frameBuffer = lightingFrameBuffer.getFrameBuffers()[idx];
            CommandBuffer commandBuffer = commandBuffers[idx];

            commandBuffer.reset();
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 1));

            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset().set(0, 0);
            renderArea.extent().set(width, height);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack).sType$Default().renderPass(lightingFrameBuffer.getLightingRenderPass().getVkRenderPass()).pClearValues(clearValues)
                    .framebuffer(frameBuffer.getFrameBuffer()).renderArea(renderArea);

            commandBuffer.beginRec();
            VkCommandBuffer cmdHandle = commandBuffer.getCmdBuf();
            vkCmdBeginRenderPass(cmdHandle, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipeline());

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack).x(0).y(height).height(-height).width(width).minDepth(0.0f).maxDepth(1.0f);
            vkCmdSetViewport(cmdHandle, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack).extent(it -> it.width(width).height(height)).offset(it -> it.x(0).y(0));
            vkCmdSetScissor(cmdHandle, 0, scissor);

            LongBuffer descriptorSets = stack.mallocLong(3).put(0, attachmentsDescriptorSet.getDescriptorSet()).put(1, lightsDescriptorSets[idx].getDescriptorSet())
                    .put(2, invProjMatrixDescriptorSet.getDescriptorSet());
            vkCmdBindDescriptorSets(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, descriptorSets, null);

            vkCmdDraw(cmdHandle, 3, 1, 0, 0);

            vkCmdEndRenderPass(cmdHandle);
            commandBuffer.endRec();
        }
    }

    public void prepareCommandBuffer() {
        int idx = swapChain.getCurrentFrame();
        Fence fence = fences[idx];

        fence.fenceWait();
        fence.reset();

        updateLights(scene.getAmbientLight(), scene.getLights(), scene.getCamera().getViewMatrix(), lightsBuffers[idx]);
    }

    public void resize(SwapChain swapChain, List<Attachment> attachments) {
        this.swapChain = swapChain;
        attachmentsDescriptorSet.update(attachments);
        lightingFrameBuffer.resize(swapChain);

        updateInvProjMatrix();

        int numImages = swapChain.getNumImages();
        for (int i = 0; i < numImages; i++) {
            preRecordCommandBuffer(i);
        }
    }

    public void submit(Queue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = commandBuffers[idx];
            Fence currentFence = fences[idx];
            SwapChain.SyncSemaphores syncSemaphores = swapChain.getSyncSemaphoresList()[idx];
            queue.submit(stack.pointers(commandBuffer.getCmdBuf()), stack.longs(syncSemaphores.geoCompleteSemaphore().getSemaphore()), stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                    stack.longs(syncSemaphores.renderCompleteSemaphore().getSemaphore()), currentFence);
        }
    }

    private void updateInvProjMatrix() {
        Matrix4f invProj = new Matrix4f(scene.getProjection().getProjectionMatrix()).invert();
        invProjBuffer.copyMatrixToBuffer(invProj);
    }

    private void updateLights(Vector4f ambientLight, Light[] lights, Matrix4f viewMatrix, Buffer lightsBuffer) {
        long mappedMemory = lightsBuffer.map();
        ByteBuffer uniformBuffer = MemoryUtil.memByteBuffer(mappedMemory, (int) lightsBuffer.getRequestedSize());

        ambientLight.get(0, uniformBuffer);
        int offset = 16;
        int numLights = lights != null ? lights.length : 0;
        uniformBuffer.putInt(offset, numLights);
        offset += 16;
        for (int i = 0; i < numLights; i++) {
            Light light = lights[i];
            auxVec.set(light.getPosition(), light.isPoint() ? 1.0f:0.0f);
            auxVec.mul(viewMatrix);
            auxVec.w = light.isPoint() ? 1.0f:0.0f;
            auxVec.get(offset, uniformBuffer);
            offset += 16;
            light.getColor().get(offset, uniformBuffer);
            offset += 16;
        }

        lightsBuffer.unMap();
    }
}