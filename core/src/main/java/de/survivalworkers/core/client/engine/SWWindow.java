package de.survivalworkers.core.client.engine;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkInstance;

import java.awt.*;
import java.io.Closeable;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;


public class SWWindow implements Closeable {
    private String title;
    @Getter
    private int width, height;
    @Getter
    private final long handle;
    @Getter
    @Setter
    private boolean resized;

    public SWWindow(int width, int height) {
        title = "SurvivalWorks";
        this.width = width;
        this.height = height;

        if (!glfwInit()) {
            throw new RuntimeException("Could not init GLFW");
        }

        if (!glfwVulkanSupported()) {
            throw new RuntimeException("No Vulcan supporting device found");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        handle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create window");
        }

        glfwMaximizeWindow(handle);
        glfwRequestWindowAttention(handle);
        /*For Icon (Icon does not work)
        try {
            BufferedImage image = ImageIO.read(Objects.requireNonNull(Window.class.getResourceAsStream("/Icon.png")));
            byte[] iconData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            ByteBuffer buffer = BufferUtils.createByteBuffer(iconData.length);
            buffer.put(iconData);
            buffer.flip();
            GLFWImage.Buffer imgbuffer = GLFWImage.create(1);
            GLFWImage glfwImage = GLFWImage.create().set(128,128,buffer);
            imgbuffer.put(0,glfwImage);
            glfwSetWindowIcon(handle, imgbuffer);
        }catch (IOException e) {
            SurvivalWorkers.LOGGER.log(e);
        }*/
        glfwSetWindowSizeLimits(handle, width, height, width, height);
        glfwSetFramebufferSizeCallback(handle, ((handle1, width1, height1) -> resize(width1,height1)));
    }

    public SWWindow() {
        this((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth(),
                (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight());

        glfwSetWindowPos(handle,0,0);
    }
    
    public void createSurface(VkInstance instance, LongBuffer pSurface) {
        Util.check(glfwCreateWindowSurface(instance, handle, null, pSurface), "Failed to create surface");
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