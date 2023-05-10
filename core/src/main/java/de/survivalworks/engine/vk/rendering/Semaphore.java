package de.survivalworks.engine.vk.rendering;

import de.survivalworks.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Semaphore {
    private final Device device;
    private final long semaphore;

    public Semaphore(Device device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateSemaphore(device.getDevice(), semaphoreCreateInfo, null, lp), "Failed to create semaphore");
            semaphore = lp.get(0);
        }
    }

    public void delete() {
        vkDestroySemaphore(device.getDevice(), semaphore, null);
    }

    public long getSemaphore() {
        return semaphore;
    }
}
