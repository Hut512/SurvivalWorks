package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public abstract class DescriptorSetLayout {
    private final Device device;
    @Getter
    protected long descriptorLayout;

    protected DescriptorSetLayout(Device device){
        this.device = device;
    }

    public void close() {
        vkDestroyDescriptorSetLayout(device.getDevice(),descriptorLayout,null);
    }

    public static class DynUniformDescriptorSetLayout extends SimpleDescriptorSetLayout {
        public DynUniformDescriptorSetLayout(Device device, int binding, int stage) {
            super(device, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, binding, stage);
        }
    }

    public static class SamplerDescriptorSetLayout extends SimpleDescriptorSetLayout {
        public SamplerDescriptorSetLayout(Device device, int binding, int stage) {
            super(device, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, binding, stage);
        }
    }

    public static class SimpleDescriptorSetLayout extends DescriptorSetLayout{
        public SimpleDescriptorSetLayout(Device device,int type,int binding,int stage){
            super(device);
            try(MemoryStack stack = MemoryStack.stackPush()) {
                VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(1,stack);
                layoutBindings.get(0).binding(binding).descriptorType(type).descriptorCount(1).stageFlags(stage);

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default().pBindings(layoutBindings);
                LongBuffer pSetLayout = stack.mallocLong(1);
                Util.check(vkCreateDescriptorSetLayout(device.getDevice(),layoutInfo,null,pSetLayout), "Could not create descriptor Set Layout");
                super.descriptorLayout = pSetLayout.get(0);
            }
        }
    }

    public static class UniformDescriptorSetLayout extends SimpleDescriptorSetLayout{
        public UniformDescriptorSetLayout(Device device,int binding,int stage){
            super(device, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,binding,stage);
        }
    }
}
