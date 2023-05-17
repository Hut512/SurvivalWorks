package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class CommandPool {

    @Getter
    private final Device device;
    @Getter
    private final long commandPool;

    public CommandPool(Device device,int queueFamilyI){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack).sType$Default().flags(VK13.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT).queueFamilyIndex(queueFamilyI);
            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateCommandPool(device.getDevice(), poolInfo, null, lp), "Could not create Command Pool");
            commandPool = lp.get(0);
        }
    }

    public void close() {
        vkDestroyCommandPool(device.getDevice(), commandPool,null);
    }
}
