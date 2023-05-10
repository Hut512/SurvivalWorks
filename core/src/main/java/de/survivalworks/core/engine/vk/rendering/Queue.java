package de.survivalworks.core.engine.vk.rendering;

import de.survivalworks.core.engine.vk.Util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class Queue {
    private final VkQueue vkQueue;

    public Queue(Device device,int queueFamI,int queueI){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            VK13.vkGetDeviceQueue(device.getDevice(),queueFamI,queueI,pQueue);
            long queue = pQueue.get(0);
            vkQueue = new VkQueue(queue,device.getDevice());
        }
    }

    public VkQueue getQueue() {
        return vkQueue;
    }

    public void submit(PointerBuffer pointers, LongBuffer longs, IntBuffer ints, LongBuffer longs1, Fence fences) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_SUBMIT_INFO).pCommandBuffers(pointers).pSignalSemaphores(longs1);
            if(longs != null)submitInfo.waitSemaphoreCount(longs.capacity()).pWaitSemaphores(longs).pWaitDstStageMask(ints);
            else submitInfo.waitSemaphoreCount(0);
            long fence = fences != null ? fences.getFence() : VK13.VK_NULL_HANDLE;
            Util.check(VK13.vkQueueSubmit(vkQueue,submitInfo,fence),"Could not Submit Queue");
        }
    }

    public void waitIdle(){
        VK13.vkQueueWaitIdle(vkQueue);
    }

    public static class GraphicsQueue extends Queue{
        public GraphicsQueue(Device device, int queueI) {
            super(device, getGraphicsQueueFamilyIndex(device), queueI);
        }

        public static int getGraphicsQueueFamilyIndex(Device device) {
            int i = -1;
            PhysicalDevice physicalDevice = device.getPhysicalDevice();
            VkQueueFamilyProperties.Buffer queuePropSBuf = physicalDevice.getQueueFamilyProps();
            for (int j = 0; j < queuePropSBuf.capacity(); j++) {
                VkQueueFamilyProperties props = queuePropSBuf.get(j);
                boolean graphicsQueue = (props.queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) != 0;
                if(graphicsQueue){
                    i = j;
                    break;
                }
            }

            if(i < 0)throw new RuntimeException("Failed getting Graphics Queue I");
            return i;
        }
    }

    public static class PresentQueue extends Queue{
        public PresentQueue(Device device,Surface surface,int queueI){
            super(device,getPresentQueueIndex(device,surface),queueI);
        }

        private static int getPresentQueueIndex(Device device, Surface surface) {
            int i = -1;
            try(MemoryStack stack = MemoryStack.stackPush()) {
                PhysicalDevice physicalDevice = device.getPhysicalDevice();
                VkQueueFamilyProperties.Buffer queueBuf = physicalDevice.getQueueFamilyProps();
                IntBuffer ip = stack.mallocInt(1);
                for (int j = 0;  j < queueBuf.capacity(); j++) {
                    KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice.getPhysicalDevice(),j,surface.getSurface(),ip);
                    if(ip.get(0) == VK13.VK_TRUE) {
                        i = j;
                        break;
                    }
                }
            }
            if(i < 0)throw new RuntimeException("Could not get Queue I");
            return i;
        }
    }
}
