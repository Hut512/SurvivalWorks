package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class Semaphore {

    private final Device device;
    @Getter
    private final long semaphore;

    public Semaphore(Device device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).sType$Default();

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateSemaphore(device.getDevice(), semaphoreCreateInfo, null, lp), "Could not create semaphore");
            semaphore = lp.get(0);
        }
    }

    public void close() {
        vkDestroySemaphore(device.getDevice(), semaphore, null);
    }
}
