package de.survivalworkers.core.engine.graphics.rendering;

import de.survivalworkers.core.engine.graphics.Util;
import de.survivalworkers.core.vk.util.VkUtil;
import de.survivalworkers.core.vk.device.LogicalDevice;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
@Slf4j
public class Image {
    private final LogicalDevice device;
    private final int format;
    private final int mipLvl;
    private final long image;
    private final long memory;


    public Image(LogicalDevice device, ImageData data){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.format = data.format;
            this.mipLvl = data.mipLvl;

            VkImageCreateInfo createInfo = VkImageCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO).imageType(VK_IMAGE_TYPE_2D).format(format).extent(it -> it.width(data.width).height(data.height).
                            depth(1)).mipLevels(mipLvl).arrayLayers(data.arrayLayers).samples(data.sampleCount).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).sharingMode(VK_SHARING_MODE_EXCLUSIVE).
                    tiling(VK_IMAGE_TILING_OPTIMAL).usage(data.usage);

            LongBuffer lp = stack.mallocLong(1);
            log.debug(data.width + ":" + data.height);
            VkUtil.check(vkCreateImage(device.getHandle(),createInfo,null,lp),"Could not create Image");
            image = lp.get(0);

            VkMemoryRequirements memReq = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device.getHandle(),image,memReq);

            VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).allocationSize(memReq.size()).
                    memoryTypeIndex(Util.memoryType(device.getPhysicalDevice(),memReq.memoryTypeBits(),0));

            VkUtil.check(vkAllocateMemory(device.getHandle(),memAlloc,null,lp),"Could not Allocate Memory");
            memory = lp.get(0);
            VkUtil.check(vkBindImageMemory(device.getHandle(),image,memory,0),"Could not bind memory");
        }
    }

    public void close() {
        vkDestroyImage(device.getHandle(),image,null);
        vkFreeMemory(device.getHandle(),memory,null);
    }

    public int getFormat() {
        return format;
    }

    public int getMipLevel() {
        return mipLvl;
    }

    public long getImage() {
        return image;
    }

    public long getMemory() {
        return memory;
    }

    public static class ImageData{
        private int format;
        private int mipLvl;
        private int sampleCount;
        private int arrayLayers;
        private int height;
        private int width;
        private int usage;
        public ImageData(){
            this.format = VK_FORMAT_R8G8B8A8_SRGB;
            this.mipLvl = 1;
            this.sampleCount = 1;
            this.arrayLayers = 1;
        }

        public ImageData arrayLayers(int layers){
            this.arrayLayers = layers;
            return this;
        }

        public ImageData format(int format){
            this.format = format;
            return this;
        }

        public ImageData height(int height){
            this.height = height;
            return this;
        }

        public ImageData width(int width){
            this.width = width;
            return this;
        }

        public ImageData mipLevels(int lvl){
            this.mipLvl = lvl;
            return this;
        }

        public ImageData sampleCount(int count){
            this.sampleCount = count;
            return this;
        }

        public ImageData usage(int usage){
            this.usage = usage;
            return this;
        }
    }
}
