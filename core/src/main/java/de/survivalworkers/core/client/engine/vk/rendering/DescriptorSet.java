package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public abstract class DescriptorSet {

    @Getter
    protected long descriptorSet;

    public static class DynUniformDescriptorSet extends SimpleDescriptorSet {
        public DynUniformDescriptorSet(DescriptorPool descriptorPool, DescriptorSetLayout descriptorSetLayout, Buffer buffer, int binding, long size) {
            super(descriptorPool, descriptorSetLayout, buffer, binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, size);
        }
    }

    public static class SimpleDescriptorSet extends DescriptorSet{
        public SimpleDescriptorSet(DescriptorPool pool,DescriptorSetLayout layout,Buffer buffer,int binding,int type,long size){
            try(MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer descriptorLayout = stack.mallocLong(1);
                descriptorLayout.put(0,layout.getDescriptorLayout());
                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default().descriptorPool(pool.getDescriptorPool()).pSetLayouts(descriptorLayout);

                LongBuffer descriptorSet = stack.mallocLong(1);
                Util.check(vkAllocateDescriptorSets(pool.getDevice().getDevice(), allocInfo,descriptorSet),"Could not allocate Descriptor set");
                super.descriptorSet = descriptorSet.get(0);

                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1,stack).buffer(buffer.getBuffer()).offset(0).range(size);
                VkWriteDescriptorSet.Buffer descBuffer = VkWriteDescriptorSet.calloc(1,stack);
                descBuffer.get(0).sType$Default().dstSet(super.descriptorSet).dstBinding(binding).descriptorType(type).descriptorCount(1).pBufferInfo(bufferInfo);

                descBuffer.get(0).sType$Default().dstSet(this.descriptorSet).dstBinding(binding).descriptorType(type).descriptorCount(1).pBufferInfo(bufferInfo);

                vkUpdateDescriptorSets(pool.getDevice().getDevice(), descBuffer,null);
            }
        }
    }

    public static class UniformDescriptorSet extends SimpleDescriptorSet {
        public UniformDescriptorSet(DescriptorPool descriptorPool, DescriptorSetLayout descriptorSetLayout, Buffer buffer, int binding) {
            super(descriptorPool, descriptorSetLayout, buffer, binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, buffer.getRequestedSize());
        }
    }
}
