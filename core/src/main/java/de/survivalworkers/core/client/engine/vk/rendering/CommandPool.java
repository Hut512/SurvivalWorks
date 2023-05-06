package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.util.VkUtil;
import de.survivalworkers.core.client.engine.vk.device.SWLogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class CommandPool {
    private final SWLogicalDevice device;
    private final long commandPool;

    public CommandPool(SWLogicalDevice device, int queueFamilyI){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT).queueFamilyIndex(queueFamilyI);
            LongBuffer lp = stack.mallocLong(1);
            VkUtil.check(vkCreateCommandPool(device.getHandle(), poolInfo, null, lp), "Could not create Command Pool");
            commandPool = lp.get(0);
        }
    }

    public void close() {
        vkDestroyCommandPool(device.getHandle(), commandPool,null);
    }

    public SWLogicalDevice getDevice() {
        return device;
    }

    public long getCommandPool() {
        return commandPool;
    }
}
