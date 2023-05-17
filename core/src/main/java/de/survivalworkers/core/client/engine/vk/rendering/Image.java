package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class Image {

    @Getter
    private final Device device;
    @Getter
    private final int format;
    @Getter
    private final int mipLvl;
    @Getter
    private final long image;
    @Getter
    private final long memory;

    public Image(Device device,ImageData data){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.format = data.format;
            this.mipLvl = data.mipLvl;

            VkImageCreateInfo createInfo = VkImageCreateInfo.calloc(stack).sType$Default().imageType(VK_IMAGE_TYPE_2D).format(format).extent(it -> it.width(data.width).height(data.height).depth(1)).
                    mipLevels(mipLvl).arrayLayers(data.arrayLayers).samples(data.sampleCount).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).sharingMode(VK_SHARING_MODE_EXCLUSIVE).tiling(VK_IMAGE_TILING_OPTIMAL).usage(data.usage);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateImage(device.getDevice(),createInfo,null,lp),"Could not create Image");
            image = lp.get(0);

            VkMemoryRequirements memReq = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device.getDevice(),image, memReq);

            VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc(stack).sType$Default().allocationSize(memReq.size()).memoryTypeIndex(device.getPhysicalDevice().memoryType(memReq.memoryTypeBits(),0));

            Util.check(vkAllocateMemory(device.getDevice(),memAlloc,null,lp),"Could not Allocate Memory");
            memory = lp.get(0);
            Util.check(vkBindImageMemory(device.getDevice(),image,memory,0),"Could not bind image memory");
        }
    }

    public void close() {
        vkDestroyImage(device.getDevice(),image,null);
        vkFreeMemory(device.getDevice(),memory,null);
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
