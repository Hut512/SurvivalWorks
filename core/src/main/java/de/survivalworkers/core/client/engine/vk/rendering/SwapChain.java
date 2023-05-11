package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.SWWindow;
import de.survivalworkers.core.client.engine.vk.Util;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;

public class SwapChain {
    private final Device device;
    private final ImageView[] imageViews;
    private final SurfaceFormat surfaceFormat;
    private final VkExtent2D swapChainExtent;
    private final SyncSemaphores[] syncSemaphoresList;
    private final long swapChain;

    private int currentFrame;
    private int numImages;

    public SwapChain(Device device, Surface surface, SWWindow window, int requestedImages) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {

            PhysicalDevice physicalDevice = device.getPhysicalDevice();

            // Get surface capabilities
            VkSurfaceCapabilitiesKHR surfCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            Util.check(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getPhysicalDevice().getPhysicalDevice(), surface.getSurface(), surfCapabilities), "Failed to get surface capabilities");

            numImages = calcNumImages(surfCapabilities, requestedImages);

            surfaceFormat = calcSurfaceFormat(physicalDevice, surface);

            swapChainExtent = calcSwapChainExtent(window, surfCapabilities);

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack).sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR).
                    surface(surface.getSurface()).minImageCount(numImages).imageFormat(surfaceFormat.imageFormat()).imageColorSpace(surfaceFormat.colorSpace()).
                    imageExtent(swapChainExtent).
                    imageArrayLayers(1).imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT).imageSharingMode(VK_SHARING_MODE_EXCLUSIVE).
                    preTransform(surfCapabilities.currentTransform()).compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR).clipped(true);
            //for vsync
            //createInfo.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
            createInfo.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(KHRSwapchain.vkCreateSwapchainKHR(device.getDevice(), createInfo, null, lp), "Failed to create swap chain");
            swapChain = lp.get(0);

            imageViews = createImageViews(stack, device, swapChain, surfaceFormat.imageFormat);
            numImages = imageViews.length;
            syncSemaphoresList = new SyncSemaphores[numImages];
            for (int i = 0; i < numImages; i++) {
                syncSemaphoresList[i] = new SyncSemaphores(device);
            }
            currentFrame = 0;
        }
    }

    public boolean acquireNextImage() {
        boolean resize = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ip = stack.mallocInt(1);
            int err = KHRSwapchain.vkAcquireNextImageKHR(device.getDevice(), swapChain, ~0L, syncSemaphoresList[currentFrame].imgAcquisitionSemaphore().getSemaphore(), MemoryUtil.NULL, ip);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) resize = true;
            else if (err == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            } else if (err != VK_SUCCESS) throw new RuntimeException("Failed to acquire image: " + err);

            currentFrame = ip.get(0);
        }

        return resize;
    }

    private int calcNumImages(VkSurfaceCapabilitiesKHR surfCapabilities, int requestedImages) {
        int maxImages = surfCapabilities.maxImageCount();
        int minImages = surfCapabilities.minImageCount();
        int result = minImages;
        if (maxImages != 0) result = Math.min(requestedImages, maxImages);
        result = Math.max(result, minImages);

        return result;
    }

    private SurfaceFormat calcSurfaceFormat(PhysicalDevice physicalDevice, Surface surface) {
        int imageFormat;
        int colorSpace;
        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer ip = stack.mallocInt(1);
            Util.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getPhysicalDevice(), surface.getSurface(), ip, null), "Failed to get the number surface formats");
            int numFormats = ip.get(0);
            if (numFormats <= 0) throw new RuntimeException("No surface formats retrieved");


            VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.calloc(numFormats, stack);
            Util.check(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.getPhysicalDevice(), surface.getSurface(), ip, surfaceFormats), "Failed to get surface formats");

            imageFormat = VK_FORMAT_B8G8R8A8_SRGB;
            colorSpace = surfaceFormats.get(0).colorSpace();
            for (int i = 0; i < numFormats; i++) {
                VkSurfaceFormatKHR surfaceFormatKHR = surfaceFormats.get(i);
                if (surfaceFormatKHR.format() == VK_FORMAT_B8G8R8A8_SRGB && surfaceFormatKHR.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    imageFormat = surfaceFormatKHR.format();
                    colorSpace = surfaceFormatKHR.colorSpace();
                    break;
                }
            }
        }
        return new SurfaceFormat(imageFormat, colorSpace);
    }

    public VkExtent2D calcSwapChainExtent(SWWindow window, VkSurfaceCapabilitiesKHR surfCapabilities) {
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

    public void delete() {
        swapChainExtent.free();
        Arrays.stream(imageViews).forEach(ImageView::delete);
        Arrays.stream(syncSemaphoresList).forEach(SyncSemaphores::delete);
        KHRSwapchain.vkDestroySwapchainKHR(device.getDevice(), swapChain, null);
    }

    private ImageView[] createImageViews(MemoryStack stack, Device device, long swapChain, int format) {
        ImageView[] result;

        IntBuffer ip = stack.mallocInt(1);
        Util.check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getDevice(), swapChain, ip, null), "Could not get Images");
        int numImages = ip.get(0);

        LongBuffer swapChainImages = stack.mallocLong(numImages);
        Util.check(KHRSwapchain.vkGetSwapchainImagesKHR(device.getDevice(), swapChain, ip, swapChainImages), "Could not create Images(1)");

        result = new ImageView[numImages];
        ImageView.ImageViewData imageViewData = new ImageView.ImageViewData().format(format).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        for (int i = 0; i < numImages; i++) {
            result[i] = new ImageView(device, swapChainImages.get(i), imageViewData);
        }

        return result;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public Device getDevice() {
        return device;
    }

    public ImageView[] getImageViews() {
        return imageViews;
    }

    public SurfaceFormat getSurfaceFormat() {
        return surfaceFormat;
    }

    public VkExtent2D getSwapChainExtent() {
        return swapChainExtent;
    }

    public SyncSemaphores[] getSyncSemaphoresList() {
        return syncSemaphoresList;
    }

    public long getSwapChain() {
        return swapChain;
    }

    public int getNumImages() {
        return numImages;
    }

    public boolean presentImage(Queue queue) {
        boolean resize = false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack).sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR).pWaitSemaphores(stack.longs(syncSemaphoresList[currentFrame].renderCompleteSemaphore().getSemaphore()))
                    .swapchainCount(1).pSwapchains(stack.longs(swapChain)).pImageIndices(stack.ints(currentFrame));

            int err = KHRSwapchain.vkQueuePresentKHR(queue.getQueue(), present);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                resize = true;
            } else if (err == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                //Sub Optimal
            } else if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to present KHR: " + err);
            }
        }
        currentFrame = (currentFrame + 1) % imageViews.length;
        return resize;
    }

    public record SurfaceFormat(int imageFormat, int colorSpace) {
    }

    public record SyncSemaphores(Semaphore imgAcquisitionSemaphore, Semaphore renderCompleteSemaphore) {

        public SyncSemaphores(Device device) {
            this(new Semaphore(device), new Semaphore(device));
        }

        public void delete() {
            imgAcquisitionSemaphore.delete();
            renderCompleteSemaphore.delete();
        }
    }
}
