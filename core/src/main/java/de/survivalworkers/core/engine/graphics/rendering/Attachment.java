package de.survivalworkers.core.engine.graphics.rendering;

import de.survivalworkers.core.vk.device.LogicalDevice;

import static org.lwjgl.vulkan.VK10.*;

public class Attachment {
    private final Image image;
    private final ImageView imageView;
    private boolean depthAttachment;

    public Attachment(LogicalDevice device, int width, int height, int format, int usage){
        Image.ImageData imageData = new Image.ImageData().width(width).height(height).usage(usage | VK_IMAGE_USAGE_SAMPLED_BIT).format(format);
        image = new Image(device,imageData);
        int aspectMask = 0;
        if((usage & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0){
            aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
            depthAttachment = false;
        }
        if((usage & VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0){
            aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;
            depthAttachment = true;
        }
        ImageView.ImageViewData viewData = new ImageView.ImageViewData().format(image.getFormat()).aspectMask(aspectMask);
        imageView = new ImageView(device,image.getImage(),viewData);
    }

    public void close() {
        imageView.close();
        image.close();
    }

    public Image getImage() {
        return image;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public boolean isDepthAttachment() {
        return depthAttachment;
    }
}
