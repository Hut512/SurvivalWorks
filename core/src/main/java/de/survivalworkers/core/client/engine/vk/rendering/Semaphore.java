package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.util.VkUtil;
import de.survivalworkers.core.client.engine.vk.device.SWLogicalDevice;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.io.Closeable;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Semaphore implements Closeable {
    private final SWLogicalDevice logicalDevice;
    @Getter
    private final long handle;

    public Semaphore(SWLogicalDevice logicalDevice) {
        this.logicalDevice = logicalDevice;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType$Default();

            LongBuffer lp = stack.mallocLong(1);
            VkUtil.check(vkCreateSemaphore(logicalDevice.getHandle(), semaphoreCreateInfo, null, lp), "Failed to create semaphore");
            handle = lp.get(0);
        }
    }

    public void close() {
        vkDestroySemaphore(logicalDevice.getHandle(), handle, null);
    }
}
