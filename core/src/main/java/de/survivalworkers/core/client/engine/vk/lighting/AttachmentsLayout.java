package de.survivalworkers.core.client.engine.vk.lighting;

import de.survivalworkers.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import  de.survivalworkers.core.client.engine.vk.rendering.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class AttachmentsLayout extends DescriptorSetLayout {

    public AttachmentsLayout(Device device, int numAttachments) {
        super(device);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(numAttachments, stack);
            for (int i = 0; i < numAttachments; i++) {
                layoutBindings.get(i).binding(i).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            }
            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(layoutBindings);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateDescriptorSetLayout(device.getDevice(), layoutInfo, null, lp), "Could not create descriptor set layout");
            super.descriptorLayout = lp.get(0);
        }
    }
}
