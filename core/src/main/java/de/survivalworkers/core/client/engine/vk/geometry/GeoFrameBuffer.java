package de.survivalworkers.core.client.engine.vk.geometry;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import  de.survivalworkers.core.client.engine.vk.rendering.Attachment;
import  de.survivalworkers.core.client.engine.vk.rendering.FrameBuffer;
import  de.survivalworkers.core.client.engine.vk.rendering.SwapChain;

import java.nio.LongBuffer;
import java.util.List;

public class GeoFrameBuffer {
    @Getter
    private final RenderPass geometryRenderPass;
    @Getter
    private FrameBuffer frameBuffer;
    @Getter
    private GeoAttachment geometryAttachments;

    public GeoFrameBuffer(SwapChain swapChain) {
        createAttachments(swapChain);
        geometryRenderPass = new RenderPass(swapChain.getDevice(), geometryAttachments.getAttachments());
        createFrameBuffer(swapChain);
    }

    public void close() {
        geometryRenderPass.close();
        geometryAttachments.close();
        frameBuffer.close();
    }

    private void createAttachments(SwapChain swapChain) {
        VkExtent2D extent2D = swapChain.getSwapChainExtent();
        int width = extent2D.width();
        int height = extent2D.height();
        geometryAttachments = new GeoAttachment(swapChain.getDevice(), width, height);
    }

    private void createFrameBuffer(SwapChain swapChain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            List<Attachment> attachments = geometryAttachments.getAttachments();
            LongBuffer attachmentsBuff = stack.mallocLong(attachments.size());
            for (Attachment attachment : attachments) {
                attachmentsBuff.put(attachment.getImageView().getImgView());
            }
            attachmentsBuff.flip();

            frameBuffer = new FrameBuffer(swapChain.getDevice(), geometryAttachments.getWidth(), geometryAttachments.getHeight(), attachmentsBuff, geometryRenderPass.getVkRenderPass());
        }
    }

    public void resize(SwapChain swapChain) {
        frameBuffer.close();
        geometryAttachments.close();
        createAttachments(swapChain);
        createFrameBuffer(swapChain);
    }
}
