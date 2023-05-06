package de.survivalworkers.core.client.engine.vk.device;

import de.survivalworkers.core.client.engine.vk.rendering.Instance;
import de.survivalworkers.core.client.engine.vk.util.VkUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class SWPhysicalDevice {
    @Getter
    private VkPhysicalDevice handle;
    private final long surface;
    @Getter
    private final String[] requiredExtensions;
    @Getter
    private final QueueFamilyIndices queueFamilies;
    @Getter
    private final SwapChainSupportDetails swapChainSupportDetails;
    @Getter
    private final String name;

    public SWPhysicalDevice(Instance instance, long surface, String... requiredExtensions) {
        this.surface = surface;
        this.requiredExtensions = requiredExtensions;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer devices = queryPhysicalDevices(instance, stack);

            if (devices.capacity() == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            for (int i = 0; i < devices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(devices.get(i), instance.getHandle());
                if (isDeviceSuitable(device, requiredExtensions)) {
                    handle = device;
                    break;
                }
            }

            if (handle == null) {
                throw new RuntimeException("Failed to find a suitable GPU");
            }

            queueFamilies = queryQueueFamilies(handle, stack);
            swapChainSupportDetails = querySwapChainSupport(handle, stack);
            name = queryName();

            log.info("Physical device: {}", name);
        }
    }

    public SWPhysicalDevice(Instance instance, long surface) {
        this(instance, surface, VK_KHR_SWAPCHAIN_EXTENSION_NAME);
    }

    private PointerBuffer queryPhysicalDevices(Instance instance, MemoryStack stack) {
        IntBuffer pPhysicalDeviceCount = stack.mallocInt(1);

        VkUtil.check(vkEnumeratePhysicalDevices(instance.getHandle(), pPhysicalDeviceCount, null),"Failed to get physical device count");
        log.info("Device count: {}", pPhysicalDeviceCount.get(0));
        PointerBuffer physicalDevices = stack.mallocPointer(pPhysicalDeviceCount.get(0));
        VkUtil.check(vkEnumeratePhysicalDevices(instance.getHandle(), pPhysicalDeviceCount, physicalDevices),"Failed to get physical devices");
        return physicalDevices;
    }

    private boolean isDeviceSuitable(VkPhysicalDevice device, String[] extensions) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return queryQueueFamilies(device, stack).isComplete() && hasDeviceExtensionSupport(device, extensions) && querySwapChainSupport(device, stack).check() &&
                    hasSamplerAnisotropy(device);
        }
    }

    private QueueFamilyIndices queryQueueFamilies(VkPhysicalDevice device, MemoryStack stack) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        IntBuffer pPropertyCount = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pPropertyCount, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(pPropertyCount.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pPropertyCount, queueFamilies);

        int i = 0;
        for (VkQueueFamilyProperties queueFamily : queueFamilies) {
            if (queueFamily.queueCount() > 0 && (queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                indices.graphicsFamily = i;
            }
            VkUtil.check(vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, pPropertyCount), "Failed to check device surface support");
            if (queueFamily.queueCount() > 0 && pPropertyCount.get(0) != 0) {
                indices.presentFamily = i;
            }
            if (indices.isComplete()) {
                break;
            }

            i++;
        }

        return indices;
    }

    private SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();
        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

        IntBuffer intPointer = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, intPointer, null);

        if (intPointer.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(intPointer.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, intPointer, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, intPointer, null);

        if (intPointer.get(0) != 0) {
            details.presentModes = stack.mallocInt(intPointer.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, intPointer, details.presentModes);
        }
        return details;
    }

    private String queryName() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
            vkGetPhysicalDeviceProperties(handle, properties);
            return properties.deviceNameString();
        }
    }

    private boolean hasDeviceExtensionSupport(VkPhysicalDevice device, String[] extensions) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pPropertyCount = stack.mallocInt(1);
            VkUtil.check(vkEnumerateDeviceExtensionProperties(device, (ByteBuffer) null, pPropertyCount, null), "Failed to get device extension property count");

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(pPropertyCount.get(0), stack);
            VkUtil.check(vkEnumerateDeviceExtensionProperties(device, (ByteBuffer) null, pPropertyCount, availableExtensions), "Failed to get device extension properties");

            Set<String> requiredExtensions = new HashSet<>(List.of(extensions));

            for (VkExtensionProperties extension : availableExtensions) {
                requiredExtensions.remove(extension.extensionNameString());
            }

            return requiredExtensions.isEmpty();
        }
    }

    private boolean hasSamplerAnisotropy(VkPhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
            vkGetPhysicalDeviceFeatures(device, supportedFeatures);
            return supportedFeatures.samplerAnisotropy();
        }
    }

    public static class SwapChainSupportDetails {
        @Getter
        private VkSurfaceCapabilitiesKHR capabilities;
        @Getter
        private VkSurfaceFormatKHR.Buffer formats;
        @Getter
        private IntBuffer presentModes;

        public boolean check() {
            return formats != null && formats.capacity() > 0 && presentModes != null && presentModes.capacity() > 0;
        }
    }

    public static class QueueFamilyIndices {
        @Getter
        private int graphicsFamily = Integer.MAX_VALUE;
        @Getter
        private int presentFamily = Integer.MAX_VALUE;

        public boolean isComplete() {
            return graphicsFamily != Integer.MAX_VALUE && presentFamily != Integer.MAX_VALUE;
        }

        public int[] getUniqueIndices() {
            return graphicsFamily == presentFamily ? new int[]{graphicsFamily} : new int[]{graphicsFamily, presentFamily};
        }
    }

    public int memoryType(int typeBits, int reqsMask) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties pProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(handle, pProperties);
            int res = -1;
            VkMemoryType.Buffer memoryTypes = pProperties.memoryTypes();
            for (int i = 0; i < VK_MAX_MEMORY_TYPES; i++) {
                if ((typeBits & 1) == 1 && (memoryTypes.get(i).propertyFlags() & reqsMask) == reqsMask) {
                    res = i;
                    break;
                }
                typeBits >>= 1;
            }
            if (res < 0) throw new RuntimeException("Could not get memoryType");

            return res;
        }
    }
}
