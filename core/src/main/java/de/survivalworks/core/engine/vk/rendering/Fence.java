package de.survivalworks.core.engine.vk.rendering;

import de.survivalworks.core.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkResetFences;

public class Fence {
    private final Device device;
    private final long fence;

    public Fence(Device device,boolean signaled){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo createInfo = VkFenceCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).flags(signaled ? VK13.VK_FENCE_CREATE_SIGNALED_BIT : 0);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateFence(device.getDevice(),createInfo,null,lp),"Could not create Fence");
            fence = lp.get(0);
        }
    }

    public void delete(){
        VK13.vkDestroyFence(device.getDevice(),fence,null);
    }

    public void fenceWait(){
        vkWaitForFences(device.getDevice(),fence,true,Long.MAX_VALUE);
    }

    public long getFence() {
        return fence;
    }

    public void reset(){
        vkResetFences(device.getDevice(),fence);
    }
}
