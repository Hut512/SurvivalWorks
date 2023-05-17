package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class FrameBuffer {

    private final Device device;
    @Getter
    private final long frameBuffer;

    public FrameBuffer(Device device, int width, int height, LongBuffer lp,long renderPass){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.calloc(stack).sType$Default().pAttachments(lp).width(width).height(height).layers(1).renderPass(renderPass);
            LongBuffer lp2 = stack.mallocLong(1);
            Util.check(vkCreateFramebuffer(device.getDevice(),createInfo,null,lp2),"Could not create FrameBuffer");
            frameBuffer = lp2.get(0);
        }
    }

    public void close() {
        vkDestroyFramebuffer(device.getDevice(),frameBuffer,null);
    }

}