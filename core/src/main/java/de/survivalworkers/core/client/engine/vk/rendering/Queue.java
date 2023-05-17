package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.*;

import static org.lwjgl.vulkan.VK11.*;

public class Queue {
    @Getter
    private final VkQueue queue;

    public Queue(Device device,int queueFamI,int queueI){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device.getDevice(),queueFamI,queueI,pQueue);
            long queue = pQueue.get(0);
            this.queue = new VkQueue(queue,device.getDevice());
        }
    }
    public void submit(PointerBuffer pointers, LongBuffer longs, IntBuffer ints, LongBuffer longs1, Fence fences) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType$Default().pCommandBuffers(pointers).pSignalSemaphores(longs1);
            if(longs != null)submitInfo.waitSemaphoreCount(longs.capacity()).pWaitSemaphores(longs).pWaitDstStageMask(ints);
            else submitInfo.waitSemaphoreCount(0);
            long fence = fences != null ? fences.getFence() : VK_NULL_HANDLE;
            Util.check(vkQueueSubmit(queue,submitInfo,fence),"Could not Submit Queue");
        }
    }

    public void waitIdle() {
        vkQueueWaitIdle(queue);
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

            if(i < 0)throw new RuntimeException("Could not get graphics Queue I");
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
                    if(ip.get(0) == VK_TRUE) {
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
