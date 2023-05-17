package de.survivalworkers.core.client.engine.vk.rendering;

import lombok.Getter;
import org.lwjgl.vulkan.VK13;

public class Attachment {

    @Getter
    private final Image image;
    @Getter
    private final ImageView imageView;
    @Getter
    private boolean depthAttachment;

    public Attachment(Device device,int width,int height,int format,int usage){
        Image.ImageData imageData = new Image.ImageData().width(width).height(height).usage(usage | VK13.VK_IMAGE_USAGE_SAMPLED_BIT).format(format);
        image = new Image(device,imageData);
        int aspectMask = 0;
        if((usage & VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0){
            aspectMask = VK13.VK_IMAGE_ASPECT_COLOR_BIT;
            depthAttachment = false;
        }
        if((usage & VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0){
            aspectMask = VK13.VK_IMAGE_ASPECT_DEPTH_BIT;
            depthAttachment = true;
        }
        ImageView.ImageViewData viewData = new ImageView.ImageViewData().format(image.getFormat()).aspectMask(aspectMask);
        imageView = new ImageView(device,image.getImage(),viewData);
    }

    public void close() {
        imageView.close();
        image.close();
    }
}
