package de.survivalworks.core.client.engine.vk.rendering;

import de.survivalworks.core.client.engine.vk.Util;
import de.survivalworks.core.client.engine.vk.vertex.Texture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;

public class TextureDescriptorSet extends DescriptorSet{
    public TextureDescriptorSet(DescriptorPool pool, DescriptorSetLayout layout, Texture texture, TextureSampler sampler, int binding){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
            pDescriptorSetLayout.put(0, layout.getDescriptorLayout());
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).descriptorPool(pool.getDescriptorPool()).pSetLayouts(pDescriptorSetLayout);

            LongBuffer pDescriptorSet = stack.mallocLong(1);
            Util.check(VK13.vkAllocateDescriptorSets(pool.getDevice().getDevice(), allocInfo, pDescriptorSet), "Could not allocate descriptor set");
            descriptorSet = pDescriptorSet.get(0);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack).imageLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL).imageView(texture.getImageView().getImgView()).sampler(sampler.getSampler());

            VkWriteDescriptorSet.Buffer descrBuffer = VkWriteDescriptorSet.calloc(1, stack);
            descrBuffer.get(0).sType(VK13.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(descriptorSet).dstBinding(binding).descriptorType(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).pImageInfo(imageInfo);

            VK13.vkUpdateDescriptorSets(pool.getDevice().getDevice(), descrBuffer, null);
        }
    }
}
