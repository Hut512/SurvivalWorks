package de.survivalworkers.core.client.engine.graphics.rendering;

import de.survivalworkers.core.client.vk.util.VkUtil;
import de.survivalworkers.core.client.vk.device.SWLogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class FrameBuffer {
    private final SWLogicalDevice device;
    private final long frameBuffer;

    public FrameBuffer(SWLogicalDevice device, int width, int height, LongBuffer lp, long renderPass){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).pAttachments(lp).width(width).height(height).layers(1).renderPass(renderPass);
            LongBuffer lp2 = stack.mallocLong(1);
            VkUtil.check(vkCreateFramebuffer(device.getHandle(), createInfo,null,lp2),"Could not create FrameBuffer");
            frameBuffer = lp2.get(0);
        }
    }

    public void close() {
        vkDestroyFramebuffer(device.getHandle(), frameBuffer,null);
    }

    public long getFrameBuffer() {
        return frameBuffer;
    }
}
