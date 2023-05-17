package de.survivalworkers.core.client.engine.vk.geometry;

import  de.survivalworkers.core.client.engine.vk.rendering.Attachment;
import  de.survivalworkers.core.client.engine.vk.rendering.Device;
import lombok.Getter;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class GeoAttachment implements Closeable {
    @Getter
    private final List<Attachment> attachments;
    @Getter
    private final Attachment deptAttachment;
    @Getter
    private final int height,width;

    public GeoAttachment(Device device, int width, int height) {
        this.width = width;
        this.height = height;
        attachments = new ArrayList<>();
        Attachment attachment = new Attachment(device, width, height, VK_FORMAT_R16G16B16A16_SFLOAT, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
        attachments.add(attachment);
        attachment = new Attachment(device, width, height, VK_FORMAT_A2B10G10R10_UNORM_PACK32, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
        attachments.add(attachment);
        attachment = new Attachment(device, width, height, VK_FORMAT_R16G16B16A16_SFLOAT, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
        attachments.add(attachment);
        deptAttachment = new Attachment(device, width, height, VK_FORMAT_D32_SFLOAT, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        attachments.add(deptAttachment);
    }

    public void close() {
        attachments.forEach(Attachment::close);
    }
}
