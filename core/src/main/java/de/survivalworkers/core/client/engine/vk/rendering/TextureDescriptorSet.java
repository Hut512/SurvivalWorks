package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.vertex.Texture;
import de.survivalworkers.core.client.engine.vk.util.VkUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class TextureDescriptorSet extends DescriptorSet {
    public TextureDescriptorSet(DescriptorPool pool, DescriptorSetLayout layout, Texture texture, TextureSampler sampler, int binding){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
            pDescriptorSetLayout.put(0, layout.getDescriptorLayout());
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).descriptorPool(pool.getDescriptorPool()).pSetLayouts(pDescriptorSetLayout);

            LongBuffer pDescriptorSet = stack.mallocLong(1);
            VkUtil.check(vkAllocateDescriptorSets(pool.getDevice().getHandle(), allocInfo, pDescriptorSet), "Could not allocate descriptor set");
            long descriptorSet = pDescriptorSet.get(0);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL).imageView(texture.getHandle().getImgView()).sampler(sampler.getHandle());

            VkWriteDescriptorSet.Buffer descrBuffer = VkWriteDescriptorSet.calloc(1, stack);
            descrBuffer.get(0).sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(descriptorSet).dstBinding(binding).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).pImageInfo(imageInfo);

            vkUpdateDescriptorSets(pool.getDevice().getHandle(), descrBuffer, null);
        }
    }
}
