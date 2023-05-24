package de.survivalworkers.core.client.engine;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkInstance;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.stb.STBImage.stbi_load;

@Slf4j
public class SWWindow implements Closeable {
    private String title;
    @Getter
    private int width, height;
    @Getter
    private final long handle;
    @Getter
    @Setter
    private boolean resized;

    public SWWindow(int width, int height,boolean windowed) {
        title = "SurvivalWorks";


        if (!glfwInit()) {
            throw new RuntimeException("Could not init GLFW");
        }

        if(width == 1 || height == 1){
            width = glfwGetVideoMode(glfwGetPrimaryMonitor()).width();
            height = glfwGetVideoMode(glfwGetPrimaryMonitor()).height();
        }
        this.width = width;
        this.height = height;


        if (!glfwVulkanSupported()) {
            throw new RuntimeException("No Vulcan supporting device found");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        handle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) {
            throw new RuntimeException("Could not create window");
        }

        if(windowed) glfwMaximizeWindow(handle);
        else glfwSetWindowPos(handle,0,0);

        glfwRequestWindowAttention(handle);
        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer i = stack.mallocInt(1);
            IntBuffer j = stack.mallocInt(1);
            IntBuffer k = stack.mallocInt(1);
            ByteBuffer img = stbi_load("core/src/main/resources/Icon.png",i,j,k,4);
            if(img == null)throw new RuntimeException("Could not Load Image");
            GLFWImage glfwImg = GLFWImage.malloc();
            glfwImg.set(i.get(),j.get(),img);
            glfwSetWindowIcon(handle,GLFWImage.calloc(1,stack).put(0,glfwImg));
        }
        glfwSetWindowSizeLimits(handle, width, height, width, height);
        glfwSetFramebufferSizeCallback(handle, ((handle1, width1, height1) -> resize(width1,height1)));
    }

    public SWWindow() {
        this(1,1,false);
    }
    
    public void createSurface(VkInstance instance, LongBuffer pSurface) {
        Util.check(glfwCreateWindowSurface(instance, handle, null, pSurface), "Could not create surface");
    }

    public void pollEvents(){
        glfwPollEvents();
    }

    public void close() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void resize(int width, int height) {
        resized = true;
        this.width = width;
        this.height = height;
    }
}