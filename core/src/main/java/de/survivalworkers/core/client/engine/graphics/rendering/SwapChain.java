package de.survivalworkers.core.client.engine.graphics.rendering;

import de.survivalworkers.core.SurvivalWorkers;
import de.survivalworkers.core.client.vk.util.VkUtil;
import de.survivalworkers.core.client.vk.device.SWLogicalDevice;
import de.survivalworkers.core.client.vk.device.SWPhysicalDevice;
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
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class SwapChain implements Closeable {
    private final SWPhysicalDevice physicalDevice;
    @Getter
    private final SWLogicalDevice logicalDevice;
    @Getter
    private final int numImages;
    @Getter
    private final SurfaceFormat surfaceFormat;
    @Getter
    private ImageView[] imageViews;
    @Getter
    private SyncSemaphores[] syncSemaphoresList;
    @Getter
    private VkSwapchainCreateInfoKHR createInfo;
    @Getter
    private long handle;
    @Getter
    private int currentFrame;

    public SwapChain(SWPhysicalDevice physicalDevice, SWLogicalDevice logicalDevice, long surface, int windowWidth, int windowHeight) {
        this.physicalDevice = physicalDevice;
        this.logicalDevice = logicalDevice;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SWPhysicalDevice.SwapChainSupportDetails swapChainSupportDetails  = physicalDevice.getSwapChainSupportDetails();

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
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(SurvivalWorkers.getInstance().getClientOptions().getPresentMode())
                    .clipped(true);

            syncSemaphoresList = new SyncSemaphores[numImages];
            for (int i = 0; i < numImages; i++) {
                syncSemaphoresList[i] = new SyncSemaphores(logicalDevice);
            }

            resize(windowWidth, windowHeight);
        }
    }

    public void resize(int windowWidth, int windowHeight) {
        long start = System.currentTimeMillis();
        vkDestroySwapchainKHR(logicalDevice.getHandle(), handle, null);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            resizeSwapChainExtent(createInfo.imageExtent(), physicalDevice.getSwapChainSupportDetails().getCapabilities(), windowWidth, windowHeight);
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

    public void resizeSwapChainExtent(VkExtent2D extend, VkSurfaceCapabilitiesKHR surfaceCapabilities, int windowWidth, int windowHeight) {
        if (surfaceCapabilities.currentExtent().width() == 0xFFFFFFFF) {
            int width = Math.min(windowWidth, surfaceCapabilities.maxImageExtent().width());
            width = Math.max(width, surfaceCapabilities.minImageExtent().width());

            int height = Math.min(windowHeight, surfaceCapabilities.maxImageExtent().height());
            height = Math.max(height, surfaceCapabilities.minImageExtent().height());

            extend.width(width);
            extend.height(height);
        } else {
            extend.set(surfaceCapabilities.currentExtent());
        }
    }

    private ImageView[] createImageViews(MemoryStack stack, SWLogicalDevice device, long swapChain, int format) {
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

        public SyncSemaphores(SWLogicalDevice device) {
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
