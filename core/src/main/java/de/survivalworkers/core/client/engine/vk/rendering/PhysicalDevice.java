package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class PhysicalDevice {
    private final VkPhysicalDevice physicalDevice;
    private final VkPhysicalDeviceProperties physicalDeviceProperties;
    private final VkExtensionProperties.Buffer deviceExtensions;
    private final VkQueueFamilyProperties.Buffer queueFamilyProps;
    private final VkPhysicalDeviceMemoryProperties memoryProperties;
    private final VkPhysicalDeviceFeatures physicalDeviceFeatures;
    private PhysicalDevice(VkPhysicalDevice vkdevice){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.physicalDevice = vkdevice;

            IntBuffer ip = stack.mallocInt(1);

            physicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
            VK13.vkGetPhysicalDeviceProperties(physicalDevice,physicalDeviceProperties);

            Util.check(VK13.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, ip, null),"Could not Enumerate Extension Properties");
            deviceExtensions = VkExtensionProperties.calloc(ip.get(0));
            Util.check(VK13.vkEnumerateDeviceExtensionProperties(physicalDevice,(String) null,ip,deviceExtensions),"Could not Enumerate Extension Properties(2)");

            VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice,ip,null);
            queueFamilyProps = VkQueueFamilyProperties.calloc(ip.get(0));
            VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice,ip,queueFamilyProps);

            physicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc();
            VK13.vkGetPhysicalDeviceFeatures(physicalDevice,physicalDeviceFeatures);

            memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
            VK13.vkGetPhysicalDeviceMemoryProperties(physicalDevice,memoryProperties);
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
                }else physicalDevice.delete();
            }

            selphysicalDevice = selphysicalDevice == null && !devices.isEmpty() ? devices.remove(0) : selphysicalDevice;

            for (PhysicalDevice device : devices) {
                device.delete();
            }

            if(selphysicalDevice == null)throw new RuntimeException("No Suitable GPU");

            return selphysicalDevice;
        }
    }

    protected static PointerBuffer getPhysicalDevices(Instance instance,MemoryStack stack){
        PointerBuffer physicalDevices;
        IntBuffer intBuffer = stack.mallocInt(1);
        Util.check(VK13.vkEnumeratePhysicalDevices(instance.getInstance(),intBuffer,null),"Could not get Physical Devices");
        physicalDevices = stack.mallocPointer(intBuffer.get(0));
        Util.check(VK13.vkEnumeratePhysicalDevices(instance.getInstance(),intBuffer,physicalDevices),"Could not get Physical Devices (1)");
        return physicalDevices;
    }

    public void delete() {
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
            if((props.queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) != 0){
                res = true;
                break;
            }
        }

        return res;
    }

    public VkQueueFamilyProperties.Buffer getQueueFamilyProps() {
        return queueFamilyProps;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkPhysicalDeviceMemoryProperties getMemoryProperties() {
        return memoryProperties;
    }

    public VkPhysicalDeviceFeatures getPhysicalDeviceFeatures() {
        return physicalDeviceFeatures;
    }

    public VkPhysicalDeviceProperties getPhysicalDeviceProperties() {
        return physicalDeviceProperties;
    }
}
