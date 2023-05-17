package de.survivalworkers.core.client.engine.vk.lighting;

import de.survivalworkers.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import  de.survivalworkers.core.client.engine.vk.rendering.*;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK11.*;

public class AttachmentsDescriptorSet extends DescriptorSet {

    private final Device device;
    private final int binding;
    private final TextureSampler textureSampler;

    public AttachmentsDescriptorSet(DescriptorPool descriptorPool, AttachmentsLayout descriptorSetLayout,
                                    List<Attachment> attachments, int binding) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            device = descriptorPool.getDevice();
            this.binding = binding;
            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
            pDescriptorSetLayout.put(0, descriptorSetLayout.getDescriptorLayout());
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default().descriptorPool(descriptorPool.getDescriptorPool()).pSetLayouts(pDescriptorSetLayout);

            LongBuffer pDescriptorSet = stack.mallocLong(1);
            Util.check(vkAllocateDescriptorSets(device.getDevice(), allocInfo, pDescriptorSet), "Could not create descriptor set");

            descriptorSet = pDescriptorSet.get(0);

            textureSampler = new TextureSampler(device, 1);

            update(attachments);
        }
    }

    public void close() {
        textureSampler.close();
    }

    public void update(List<Attachment> attachments) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int numAttachments = attachments.size();
            VkWriteDescriptorSet.Buffer descrBuffer = VkWriteDescriptorSet.calloc(numAttachments, stack);
            for (int i = 0; i < numAttachments; i++) {
                Attachment attachment = attachments.get(i);
                VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack).sampler(textureSampler.getSampler()).imageView(attachment.getImageView().getImgView());
                if (attachment.isDepthAttachment()) imageInfo.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
                else imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);


                descrBuffer.get(i).sType$Default().dstSet(descriptorSet).dstBinding(binding + i).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).pImageInfo(imageInfo);
            }

            vkUpdateDescriptorSets(device.getDevice(), descrBuffer, null);
        }
    }
}