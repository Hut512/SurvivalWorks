package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public abstract class DescriptorSet {
    protected long descriptorSet;

    public long getDescriptorSet() {
        return descriptorSet;
    }

    public static class UniformDescriptorSet extends SimpleDescriptorSet{
        public UniformDescriptorSet(DescriptorPool pool,DescriptorSetLayout layout,Buffer buffer,int binding){
            super(pool,layout,buffer,binding, VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,buffer.getRequestedSize());
        }
    }

    public static class SimpleDescriptorSet extends DescriptorSet{
        public SimpleDescriptorSet(DescriptorPool pool,DescriptorSetLayout layout,Buffer buffer,int binding,int type,long size){
            try(MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer descriptorLayout = stack.mallocLong(1);
                descriptorLayout.put(0,layout.getDescriptorLayout());
                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO).descriptorPool(pool.getDescriptorPool()).pSetLayouts(descriptorLayout);

                LongBuffer descriptorSet = stack.mallocLong(1);
                Util.check(VK13.vkAllocateDescriptorSets(pool.getDevice().getDevice(), allocInfo,descriptorSet),"Could not allocate Descriptor set");
                super.descriptorSet = descriptorSet.get(0);

                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1,stack).buffer(buffer.getBuffer()).offset(0).range(size);
                VkWriteDescriptorSet.Buffer descBuffer = VkWriteDescriptorSet.calloc(1,stack);
                descBuffer.get(0).sType(VK13.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(super.descriptorSet).dstBinding(binding).descriptorType(type).descriptorCount(1).pBufferInfo(bufferInfo);

                VK13.vkUpdateDescriptorSets(pool.getDevice().getDevice(), descBuffer,null);
            }
        }
    }

    public static class DynUniformDescriptorSet extends SimpleDescriptorSet{
        public DynUniformDescriptorSet(DescriptorPool pool,DescriptorSetLayout layout,Buffer buffer,int binding,long size){
            super(pool,layout,buffer,binding, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC,size);
        }
    }
}
