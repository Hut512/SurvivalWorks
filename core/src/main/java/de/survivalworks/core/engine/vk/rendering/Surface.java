package de.survivalworks.core.engine.vk.rendering;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;

import java.nio.LongBuffer;

public class Surface {
    private final PhysicalDevice physicalDevice;
    private final long surface;

    public Surface(PhysicalDevice physicalDevice,long window){
        this.physicalDevice = physicalDevice;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            GLFWVulkan.glfwCreateWindowSurface(this.physicalDevice.getPhysicalDevice().getInstance(),window,null,pSurface);
            surface = pSurface.get(0);
        }
    }

    public void delete(){
        KHRSurface.vkDestroySurfaceKHR(physicalDevice.getPhysicalDevice().getInstance(),surface,null);
    }

    public long getSurface() {
        return surface;
    }
}
