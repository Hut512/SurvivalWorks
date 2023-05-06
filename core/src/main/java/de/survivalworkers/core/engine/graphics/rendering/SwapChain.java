package de.survivalworkers.core.engine.graphics.rendering;

import de.survivalworkers.core.Window;
import de.survivalworkers.core.vk.util.VkUtil;
import de.survivalworkers.core.vk.device.LogicalDevice;
import de.survivalworkers.core.vk.device.PhysicalDevice;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.Closeable;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class SwapChain implements Closeable {
    private final PhysicalDevice physicalDevice;
    @Getter
    private final LogicalDevice logicalDevice;
    @Getter
    private final int numImages;
    @Getter
    private final SurfaceFormat surfaceFormat;
    @Getter
    private ImageView[] imageViews;
    @Getter
    private SyncSemaphores[] syncSemaphoresList;

    private VkSwapchainCreateInfoKHR createInfo;
    @Getter
    private int width;
    @Getter
    private int height;
    @Getter
    private long handle;
    @Getter
    private int currentFrame;

    public SwapChain(PhysicalDevice physicalDevice, LogicalDevice logicalDevice, long surface, int width, int height) {
        this.physicalDevice = physicalDevice;
        this.logicalDevice = logicalDevice;
        this.height = height;
        this.width = width;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PhysicalDevice.SwapChainSupportDetails swapChainSupportDetails  = physicalDevice.querySwapChainSupport(stack);

            numImages = calcNumImages(swapChainSupportDetails.getCapabilities());

            surfaceFormat = selectSurfaceFormat(swapChainSupportDetails.getFormats());

            createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType$Default()
                    .surface(surface)
                    .minImageCount(numImages)
                    .imageFormat(surfaceFormat.imageFormat())
                    .imageColorSpace(surfaceFormat.colorSpace())
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .preTransform(swapChainSupportDetails.getCapabilities().currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .clipped(true);
            //for vsync
            //createInfo.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
            createInfo.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);

            syncSemaphoresList = new SyncSemaphores[numImages];
            for (int i = 0; i < numImages; i++) {
                syncSemaphoresList[i] = new SyncSemaphores(logicalDevice);
            }

            resize(width, height);
        }
    }

    public void resize(int width, int height) {
        long start = System.currentTimeMillis();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            createInfo.imageExtent()
                    .set(width, height);
            LongBuffer pSwapchain = stack.mallocLong(1);
            VkUtil.check(vkCreateSwapchainKHR(logicalDevice.getHandle(), createInfo, null, pSwapchain), "Failed to create swap chain");
            handle = pSwapchain.get(0);

            imageViews = createImageViews(stack, logicalDevice, handle, surfaceFormat.imageFormat);
            currentFrame = 0;
        }
        log.trace("Swapchain resized in {}ms", System.currentTimeMillis() - start);
    }

    public boolean acquireNextImage() {
        boolean resize = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ip = stack.mallocInt(1);
            int err = vkAcquireNextImageKHR(logicalDevice.getHandle(), handle, Long.MAX_VALUE, syncSemaphoresList[currentFrame].imgAcquisitionSemaphore().getHandle(), MemoryUtil.NULL, ip);
            if (err == VK_ERROR_OUT_OF_DATE_KHR) {
                resize = true;
            } else if (err == VK_SUBOPTIMAL_KHR) {
                //Sub Optimal
            } else {
                VkUtil.check(err, "Failed to acquire image");
            }

            currentFrame = ip.get(0);
        }

        return resize;
    }

    private int calcNumImages(VkSurfaceCapabilitiesKHR surfaceCapabilities) {
        int result = Math.max(3, surfaceCapabilities.minImageCount());
        if (surfaceCapabilities.maxImageCount() != 0) result = Math.min(result, surfaceCapabilities.maxImageCount());
        return result;
    }

    private SurfaceFormat selectSurfaceFormat(VkSurfaceFormatKHR.Buffer surfaceFormats) {
        int format = 0;
        int colorSpace = 0;
        for (int i = 0; i < surfaceFormats.capacity(); i++) {
            VkSurfaceFormatKHR surfaceFormat = surfaceFormats.get(i);
            if (surfaceFormat.format() != VK_FORMAT_B8G8R8A8_SRGB)
                continue;
            format = surfaceFormat.format();
            colorSpace = surfaceFormat.colorSpace();
            if (colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                break;
        }
        return new SurfaceFormat(format, colorSpace);
    }

    public VkExtent2D calcSwapChainExtent(Window window, VkSurfaceCapabilitiesKHR surfCapabilities) {
        VkExtent2D result = VkExtent2D.calloc();
        if (surfCapabilities.currentExtent().width() == 0xFFFFFFFF) {
            int width = Math.min(window.getWidth(), surfCapabilities.maxImageExtent().width());
            width = Math.max(width, surfCapabilities.minImageExtent().width());

            int height = Math.min(window.getHeight(), surfCapabilities.maxImageExtent().height());
            height = Math.max(height, surfCapabilities.minImageExtent().height());

            result.width(width);
            result.height(height);
        } else {
            result.set(surfCapabilities.currentExtent());
        }
        return result;
    }

    private ImageView[] createImageViews(MemoryStack stack, LogicalDevice device, long swapChain, int format) {
        ImageView[] result;

        IntBuffer ip = stack.mallocInt(1);
        VkUtil.check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getHandle(), swapChain, ip, null), "Could not get Images");
        int numImages = ip.get(0);

        LongBuffer swapChainImages = stack.mallocLong(numImages);
        VkUtil.check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getHandle(), swapChain, ip, swapChainImages), "Could not create Images(1)");

        result = new ImageView[numImages];
        ImageView.ImageViewData imageViewData = new ImageView.ImageViewData().format(format).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        for (int i = 0; i < numImages; i++) {
            result[i] = new ImageView(device, swapChainImages.get(i), imageViewData);
        }

        return result;
    }

    public boolean presentImage(VkQueue queue) {
        boolean resize = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack)
                    .sType$Default()
                    .pWaitSemaphores(stack.longs(syncSemaphoresList[currentFrame].renderCompleteSemaphore().getHandle()))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(handle))
                    .pImageIndices(stack.ints(currentFrame));

            int err = vkQueuePresentKHR(queue, present);
            if (err == VK_ERROR_OUT_OF_DATE_KHR) {
                resize = true;
            } else if (err == VK_SUBOPTIMAL_KHR) {
                //Sub Optimal
            } else if (err != VK_SUCCESS) {
                VkUtil.check(err, "Failed to present KHR");
            }
        }
        currentFrame = (currentFrame + 1) % imageViews.length;
        return resize;
    }

    public record SurfaceFormat(int imageFormat, int colorSpace) {}

    public record SyncSemaphores(Semaphore imgAcquisitionSemaphore, Semaphore renderCompleteSemaphore) {

        public SyncSemaphores(LogicalDevice device) {
            this(new Semaphore(device), new Semaphore(device));
        }

        public void close() {
            imgAcquisitionSemaphore.close();
            renderCompleteSemaphore.close();
        }
    }

    public void close() {
        vkDestroySwapchainKHR(logicalDevice.getHandle(), handle, null);
        createInfo.free();
        Arrays.stream(imageViews).forEach(ImageView::close);
        Arrays.stream(syncSemaphoresList).forEach(SyncSemaphores::close);
    }
}
