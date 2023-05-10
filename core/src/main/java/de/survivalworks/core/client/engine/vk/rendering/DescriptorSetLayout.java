package de.survivalworks.core.client.engine.vk.rendering;

import de.survivalworks.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;

public abstract class DescriptorSetLayout {
    private final Device device;
    protected long descriptorLayout;

    protected DescriptorSetLayout(Device device){
        this.device = device;
    }

    public void delete(){
        VK13.vkDestroyDescriptorSetLayout(device.getDevice(),descriptorLayout,null);
    }

    public long getDescriptorLayout() {
        return descriptorLayout;
    }

    public static class SimpleDescriptorSetLayout extends DescriptorSetLayout{
        public SimpleDescriptorSetLayout(Device device,int type,int binding,int stage){
            super(device);
            try(MemoryStack stack = MemoryStack.stackPush()) {
                VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(1,stack);
                layoutBindings.get(0).binding(binding).descriptorType(type).descriptorCount(1).stageFlags(stage);
                VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(layoutBindings);
                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(layoutBindings);
                LongBuffer lp = stack.mallocLong(1);
                Util.check(VK13.vkCreateDescriptorSetLayout(device.getDevice(),layoutInfo,null,lp),"Could not create descriptor Set Layout");
                super.descriptorLayout = lp.get(0);
            }
        }
    }

    public static class SamplerDescriptorSetLayout extends SimpleDescriptorSetLayout{
        public SamplerDescriptorSetLayout(Device device,int binding,int stage){
            super(device,VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,binding,stage);
        }
    }

    public static class UniformDescriptorSetLayout extends SimpleDescriptorSetLayout{
        public UniformDescriptorSetLayout(Device device,int binding,int stage){
            super(device, VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,binding,stage);
        }
    }

    public static class DynUniformDescriptorSetLayout extends SimpleDescriptorSetLayout{
        public DynUniformDescriptorSetLayout(Device device,int binding,int stage){
            super(device,VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC,binding,stage);
        }
    }
}
