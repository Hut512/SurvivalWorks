package de.survivalworkers.core.client.engine;

import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;

public class Window {
    private int width, height;
    private String title;
    private long window;
    private boolean resized;

    public Window() {
        this.title = "Survival Workers";

        if (!GLFW.glfwInit()) throw new RuntimeException("Could not init GLFW");
        if (!GLFWVulkan.glfwVulkanSupported()) throw new IllegalStateException("No Vulcan supporting device found");

        /*GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        width = videoMode.width();
        height = videoMode.height();*/

        width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        height = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API,GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED,GLFW.GLFW_FALSE);


        window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) throw new RuntimeException("Window not created");

        GLFW.glfwMaximizeWindow(window);
        GLFW.glfwRequestWindowAttention(window);
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
            GLFW.glfwSetWindowIcon(window, imgbuffer);
        }catch (IOException e) {
            SurvivalWorkers.LOGGER.log(e);
        }*/
        GLFW.glfwSetWindowPos(window,0,0);
        GLFW.glfwSetWindowSizeLimits(window,width,height,width,height);
        GLFW.glfwSetFramebufferSizeCallback(window,((window1, width1, height1) -> resize(width1,height1)));
    }

    public long window() {
        return window;
    }

    public void pollEvents(){
        GLFW.glfwPollEvents();
    }

    public void delete() {
        GLFW.glfwWindowShouldClose(window);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public void swapBuffers() {
        GLFW.glfwSwapBuffers(window);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public void resize(int width, int height) {
        resized = true;
        this.width = width;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean isResized() {
        return resized;
    }

    public void resetResized() {
        resized = false;
    }

    public void setResized(boolean resized) {
        this.resized = resized;
    }
}