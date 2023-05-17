package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import de.survivalworkers.core.client.engine.vk.vertex.Texture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class TextureDescriptorSet extends DescriptorSet{
    public TextureDescriptorSet(DescriptorPool pool, DescriptorSetLayout layout, Texture texture, TextureSampler sampler, int binding){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
            pDescriptorSetLayout.put(0, layout.getDescriptorLayout());
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default().descriptorPool(pool.getDescriptorPool()).pSetLayouts(pDescriptorSetLayout);

            LongBuffer pDescriptorSet = stack.mallocLong(1);
            Util.check(vkAllocateDescriptorSets(pool.getDevice().getDevice(), allocInfo, pDescriptorSet), "Could not allocate descriptor set");
            descriptorSet = pDescriptorSet.get(0);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack).imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL).imageView(texture.getImageView().getImgView()).sampler(sampler.getSampler());

            VkWriteDescriptorSet.Buffer descrBuffer = VkWriteDescriptorSet.calloc(1, stack);
            descrBuffer.get(0).sType$Default().dstSet(descriptorSet).dstBinding(binding).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).pImageInfo(imageInfo);

            vkUpdateDescriptorSets(pool.getDevice().getDevice(), descrBuffer, null);
        }
    }
}
