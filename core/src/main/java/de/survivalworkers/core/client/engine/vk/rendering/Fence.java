package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class Fence {

    @Getter
    private final Device device;
    @Getter
    private final long fence;

    public Fence(Device device,boolean signaled){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo createInfo = VkFenceCreateInfo.calloc(stack).sType$Default().flags(signaled ? VK13.VK_FENCE_CREATE_SIGNALED_BIT : 0);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateFence(device.getDevice(),createInfo,null,lp),"Could not create Fence");
            fence = lp.get(0);
        }
    }

    public void close() {
        vkDestroyFence(device.getDevice(),fence,null);
    }

    public void fenceWait(){
        vkWaitForFences(device.getDevice(),fence,true,Long.MAX_VALUE);
    }

    public void reset(){
        vkResetFences(device.getDevice(),fence);
    }

}
