package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class ImageView {

    @Getter
    private final Device device;
    @Getter
    private final long imgView;

    public ImageView(Device device,long img,ImageViewData data) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack).sType$Default().image(img).viewType(data.viewType).format(data.format).
                    subresourceRange(it -> it.aspectMask(data.aspectMask).baseMipLevel(0).levelCount(data.mipLevels).baseArrayLayer(data.baseArrayLayer).layerCount(data.layerCount));

            Util.check(vkCreateImageView(device.getDevice(), createInfo, null, lp),"Could not create image view");
            imgView = lp.get(0);
        }
    }

    public void close() {
        vkDestroyImageView(device.getDevice(), imgView, null);
    }

    public static class ImageViewData {
        private int aspectMask;
        private int baseArrayLayer;
        private int format;
        private int layerCount;
        private int mipLevels;
        private int viewType;

        public ImageViewData() {
            this.baseArrayLayer = 0;
            this.layerCount = 1;
            this.mipLevels = 1;
            this.viewType = VK_IMAGE_VIEW_TYPE_2D;
        }

        public ImageView.ImageViewData aspectMask(int aspectMask) {
            this.aspectMask = aspectMask;
            return this;
        }

        public ImageView.ImageViewData baseArrayLayer(int baseArrayLayer) {
            this.baseArrayLayer = baseArrayLayer;
            return this;
        }

        public ImageView.ImageViewData format(int format) {
            this.format = format;
            return this;
        }

        public ImageView.ImageViewData layerCount(int layerCount) {
            this.layerCount = layerCount;
            return this;
        }

        public ImageView.ImageViewData mipLevels(int mipLevels) {
            this.mipLevels = mipLevels;
            return this;
        }

        public ImageView.ImageViewData viewType(int viewType) {
            this.viewType = viewType;
            return this;
        }
    }
}
