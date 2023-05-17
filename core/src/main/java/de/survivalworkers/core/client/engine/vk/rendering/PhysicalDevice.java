package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK11.*;

public class PhysicalDevice {
    @Getter
    private final VkPhysicalDevice physicalDevice;
    @Getter
    private final VkPhysicalDeviceProperties physicalDeviceProperties;
    @Getter
    private final VkExtensionProperties.Buffer deviceExtensions;
    @Getter
    private final VkQueueFamilyProperties.Buffer queueFamilyProps;
    @Getter
    private final VkPhysicalDeviceMemoryProperties memoryProperties;
    @Getter
    private final VkPhysicalDeviceFeatures physicalDeviceFeatures;
    private PhysicalDevice(VkPhysicalDevice vkdevice){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.physicalDevice = vkdevice;

            IntBuffer ip = stack.mallocInt(1);
            physicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
            vkGetPhysicalDeviceProperties(physicalDevice,physicalDeviceProperties);

            Util.check(vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, ip, null),"Could not Enumerate Extension Properties");
            deviceExtensions = VkExtensionProperties.calloc(ip.get(0));
            Util.check(vkEnumerateDeviceExtensionProperties(physicalDevice,(String) null,ip,deviceExtensions),"Could not Enumerate Extension Properties(2)");

            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice,ip,null);
            queueFamilyProps = VkQueueFamilyProperties.calloc(ip.get(0));
            vkGetPhysicalDeviceQueueFamilyProperties(vkdevice,ip,queueFamilyProps);

            physicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc();
            vkGetPhysicalDeviceFeatures(physicalDevice,physicalDeviceFeatures);

            memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
            vkGetPhysicalDeviceMemoryProperties(physicalDevice,memoryProperties);
        }
    }

    public static PhysicalDevice create(Instance instance,String prefName){
        PhysicalDevice selphysicalDevice = null;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer physicalDevices = getPhysicalDevices(instance,stack);
            if(physicalDevices.capacity() < 1)throw new IllegalStateException("No GPU");

            List<PhysicalDevice> devices = new ArrayList<>();
            for (int i = 0; i < physicalDevices.capacity(); i++) {
                VkPhysicalDevice vkPhysicalDevice = new VkPhysicalDevice(physicalDevices.get(i),instance.getInstance());
                PhysicalDevice physicalDevice = new PhysicalDevice(vkPhysicalDevice);
                if(physicalDevice.hasGraphicsQueue() && physicalDevice.hasSwapChainExtension()){
                    if(prefName != null && prefName.equals(physicalDevice.getDeviceName())){
                        selphysicalDevice = physicalDevice;
                        break;
                    }
                    devices.add(physicalDevice);
                }else physicalDevice.close();

            }

            selphysicalDevice = selphysicalDevice == null && !devices.isEmpty() ? devices.remove(0) : selphysicalDevice;

            for (PhysicalDevice device : devices) {
                device.close();
            }

            if(selphysicalDevice == null)throw new RuntimeException("No Suitable GPU");

        }

        return selphysicalDevice;
    }

    protected static PointerBuffer getPhysicalDevices(Instance instance,MemoryStack stack){
        PointerBuffer physicalDevices;
        IntBuffer ip = stack.mallocInt(1);
        Util.check(vkEnumeratePhysicalDevices(instance.getInstance(), ip,null),"Could not get number of physical devices");
        physicalDevices = stack.mallocPointer(ip.get(0));
        Util.check(vkEnumeratePhysicalDevices(instance.getInstance(), ip,physicalDevices),"Could not get physical devices");
        return physicalDevices;
    }

    public void close() {
        memoryProperties.free();
        physicalDeviceFeatures.free();
        queueFamilyProps.free();
        deviceExtensions.free();
        physicalDeviceProperties.free();
    }

    private String getDeviceName() {
        return physicalDeviceProperties.deviceNameString();
    }

    private boolean hasSwapChainExtension() {
        boolean res = false;
        for (int i = 0; i < deviceExtensions.capacity(); i++) {
            String name = deviceExtensions.get(i).extensionNameString();
            if(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(name)) {
                res = true;
                break;
            }
        }

        return res;
    }

    private boolean hasGraphicsQueue() {
        boolean res = false;
        int queueFamCount = queueFamilyProps != null ? queueFamilyProps.capacity() : 0;
        for (int i = 0; i < queueFamCount; i++) {
            VkQueueFamilyProperties props = queueFamilyProps.get(i);
            if((props.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0){
                res = true;
                break;
            }
        }
        return res;
    }

    public int memoryType(int typeBits, int reqsMask) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties pProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, pProperties);
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
