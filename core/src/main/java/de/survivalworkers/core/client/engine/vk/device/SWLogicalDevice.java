package de.survivalworkers.core.client.engine.vk.device;

import de.survivalworkers.core.client.engine.vk.util.VkUtil;
import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

public class SWLogicalDevice {
    @Getter
    private final SWPhysicalDevice physicalDevice;
    @Getter
    private final VkDevice handle;
    @Getter
    private final VkQueue graphicsQueue;
    @Getter
    private final VkQueue presentQueue;

    public SWLogicalDevice(SWPhysicalDevice physicalDevice) {
        this.physicalDevice = physicalDevice;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            SWPhysicalDevice.QueueFamilyIndices queueFamilyIndices = physicalDevice.getQueueFamilies();

            int[] uniqueQueueFamilies = physicalDevice.getQueueFamilies().getUniqueIndices();
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.malloc(uniqueQueueFamilies.length);

            FloatBuffer pQueuePriorities = stack.floats(1.0f);
            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                queueCreateInfos.get(i)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .queueFamilyIndex(uniqueQueueFamilies[i])
                        .pQueuePriorities(pQueuePriorities);
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack)
                    .samplerAnisotropy(true);

            String[] extensions = physicalDevice.getRequiredExtensions();

            PointerBuffer ppEnabledExtensionNames = stack.mallocPointer(extensions.length);

            for (int i = 0; i < extensions.length; i++) {
                ppEnabledExtensionNames.put(i, stack.ASCII(extensions[i]));
            }

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queueCreateInfos)
                    .ppEnabledExtensionNames(ppEnabledExtensionNames)
                    .pEnabledFeatures(deviceFeatures);

            PointerBuffer pDevice = stack.mallocPointer(1);

            VkUtil.check(vkCreateDevice(physicalDevice.getHandle(), createInfo, null, pDevice), "Failed to create logical device");

            handle = new VkDevice(pDevice.get(0), physicalDevice.getHandle(), createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);

            vkGetDeviceQueue(handle, queueFamilyIndices.getGraphicsFamily(), 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), handle);
            vkGetDeviceQueue(handle, queueFamilyIndices.getPresentFamily(), 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), handle);
        }
    }

    public void waitIdle() {
        vkDeviceWaitIdle(handle);
    }

    public void close() {
        vkDestroyDevice(handle,null);
    }
}
