package de.survivalworks.engine.vk.rendering;

import de.survivalworks.engine.vk.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;

public class Device {
    private final PhysicalDevice physicalDevice;
    private final VkDevice device;
    private final boolean samplerAnisotropy;

    public Device(PhysicalDevice physicalDevice){
        this.physicalDevice = physicalDevice;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer reqExt = stack.mallocPointer(1);
            reqExt.put(0,stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack).samplerAnisotropy(true);

            VkQueueFamilyProperties.Buffer queueBuf = physicalDevice.getQueueFamilyProps();
            VkDeviceQueueCreateInfo.Buffer createInfoBuf = VkDeviceQueueCreateInfo.calloc(queueBuf.capacity(),stack);
            for (int i = 0; i < queueBuf.capacity(); i++) {
                FloatBuffer prio = stack.callocFloat(queueBuf.get(i).queueCount());
                createInfoBuf.get(i).sType(VK13.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO).queueFamilyIndex(i).pQueuePriorities(prio);
            }

            VkPhysicalDeviceFeatures supported = this.physicalDevice.getPhysicalDeviceFeatures();
            samplerAnisotropy = supported.samplerAnisotropy();

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO).ppEnabledExtensionNames(reqExt).
                    pEnabledFeatures(features).pQueueCreateInfos(createInfoBuf);
            PointerBuffer pp = stack.mallocPointer(1);
            Util.check(VK13.vkCreateDevice(physicalDevice.getPhysicalDevice(),deviceCreateInfo,null,pp),"Could not create device");
            device = new VkDevice(pp.get(0),physicalDevice.getPhysicalDevice(),deviceCreateInfo);
        }
    }

    public void delete(){
        VK13.vkDestroyDevice(device,null);
    }

    public PhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice getDevice() {
        return device;
    }

    public void waitIdle(){
        VK13.vkDeviceWaitIdle(device);
    }

    public boolean isSamplerAnisotropy() {
        return samplerAnisotropy;
    }
}
