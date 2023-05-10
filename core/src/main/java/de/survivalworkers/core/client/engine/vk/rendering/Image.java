package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

public class Image {
    private final Device device;
    private final int format;
    private final int mipLvl;
    private final long image;
    private final long memory;

    public Image(Device device,ImageData data){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.format = data.format;
            this.mipLvl = data.mipLvl;

            VkImageCreateInfo createInfo = VkImageCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO).imageType(VK13.VK_IMAGE_TYPE_2D).format(format).extent(it -> it.width(data.width).height(data.height).depth(1)).
                    mipLevels(mipLvl).arrayLayers(data.arrayLayers).samples(data.sampleCount).initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED).sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE).tiling(VK13.VK_IMAGE_TILING_OPTIMAL).usage(data.usage);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateImage(device.getDevice(),createInfo,null,lp),"Could not create Image");
            image = lp.get(0);

            VkMemoryRequirements memReq = VkMemoryRequirements.calloc(stack);
            VK13.vkGetImageMemoryRequirements(device.getDevice(),image,memReq);

            VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).allocationSize(memReq.size()).
                    memoryTypeIndex(Util.memoryType(device.getPhysicalDevice(),memReq.memoryTypeBits(),0));

            Util.check(VK13.vkAllocateMemory(device.getDevice(),memAlloc,null,lp),"Could not Allocate Memory");
            memory = lp.get(0);
            Util.check(VK13.vkBindImageMemory(device.getDevice(),image,memory,0),"Could not bind memory");
        }
    }

    public void delete(){
        VK13.vkDestroyImage(device.getDevice(),image,null);
        VK13.vkFreeMemory(device.getDevice(),memory,null);
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
            this.format = VK13.VK_FORMAT_R8G8B8A8_SRGB;
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
