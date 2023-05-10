package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class FrameBuffer {
    private final Device device;
    private final long frameBuffer;

    public FrameBuffer(Device device, int width, int height, LongBuffer lp,long renderPass){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO).pAttachments(lp).width(width).height(height).layers(1).renderPass(renderPass);
            LongBuffer lp2 = stack.mallocLong(1);
            Util.check(vkCreateFramebuffer(device.getDevice(),createInfo,null,lp2),"Could not create FrameBuffer");
            frameBuffer = lp2.get(0);
        }
    }

    public void delete(){
        VK13.vkDestroyFramebuffer(device.getDevice(),frameBuffer,null);
    }

    public long getFrameBuffer() {
        return frameBuffer;
    }
}
