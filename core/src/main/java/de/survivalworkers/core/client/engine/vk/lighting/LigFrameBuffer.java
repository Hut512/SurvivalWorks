package de.survivalworkers.core.client.engine.vk.lighting;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import  de.survivalworkers.core.client.engine.vk.rendering.*;

import java.nio.LongBuffer;
import java.util.Arrays;

public class LigFrameBuffer {
    @Getter
    private final LigRenderPass lightingRenderPass;
    @Getter
    private FrameBuffer[] frameBuffers;

    public LigFrameBuffer(SwapChain swapChain) {
        lightingRenderPass = new LigRenderPass(swapChain);
        createFrameBuffers(swapChain);
    }

    public void close() {
        Arrays.stream(frameBuffers).forEach(FrameBuffer::close);
        lightingRenderPass.close();
    }

    private void createFrameBuffers(SwapChain swapChain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D extent2D = swapChain.getSwapChainExtent();
            int width = extent2D.width();
            int height = extent2D.height();

            int numImages = swapChain.getNumImages();
            frameBuffers = new FrameBuffer[numImages];
            LongBuffer attachmentsBuff = stack.mallocLong(1);
            for (int i = 0; i < numImages; i++) {
                attachmentsBuff.put(0, swapChain.getImageViews()[i].getImgView());
                frameBuffers[i] = new FrameBuffer(swapChain.getDevice(), width, height, attachmentsBuff, lightingRenderPass.getVkRenderPass());
            }
        }
    }

    public void resize(SwapChain swapChain) {
        Arrays.stream(frameBuffers).forEach(FrameBuffer::close);
        createFrameBuffers(swapChain);
    }
}