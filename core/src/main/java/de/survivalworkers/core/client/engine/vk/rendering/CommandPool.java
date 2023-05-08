package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

public class CommandPool {
    private final Device device;
    private final long commandPool;

    public CommandPool(Device device,int queueFamilyI){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).flags(VK13.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT).queueFamilyIndex(queueFamilyI);
            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateCommandPool(device.getDevice(), poolInfo, null, lp), "Could not create Command Pool");
            commandPool = lp.get(0);
        }
    }

    public void delete(){
        VK13.vkDestroyCommandPool(device.getDevice(),commandPool,null);
    }

    public Device getDevice() {
        return device;
    }

    public long getCommandPool() {
        return commandPool;
    }
}
