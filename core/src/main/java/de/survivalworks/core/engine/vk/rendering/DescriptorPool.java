package de.survivalworks.core.engine.vk.rendering;

import de.survivalworks.core.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import java.nio.LongBuffer;
import java.util.List;

public class DescriptorPool {
    private final Device device;
    private final long descriptorPool;
    public DescriptorPool(Device device, List<DescriptorTypeCount> descriptors){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            int maxSets = 0;
            VkDescriptorPoolSize.Buffer typeCounts = VkDescriptorPoolSize.calloc(descriptors.size(),stack);
            for (int i = 0; i < descriptors.size(); i++) {
                maxSets += descriptors.get(i).count();
                typeCounts.get(i).type(descriptors.get(i).type).descriptorCount(descriptors.get(i).count());
            }

            VkDescriptorPoolCreateInfo createInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).flags(VK13.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT).pPoolSizes(typeCounts).
                    maxSets(maxSets);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateDescriptorPool(device.getDevice(),createInfo,null,lp),"Could not create descriptor Pool");
            descriptorPool = lp.get(0);
        }
    }

    public record DescriptorTypeCount(int count,int type){}

    public void freeDescriptors(long descriptorSet){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);
            lp.put(0,descriptorSet);

            Util.check(VK13.vkFreeDescriptorSets(device.getDevice(),descriptorPool,lp),"Could not free Descriptor Set");
        }
    }

    public void delete(){
        VK13.vkDestroyDescriptorPool(device.getDevice(),descriptorPool,null);
    }

    public Device getDevice() {
        return device;
    }

    public long getDescriptorPool() {
        return descriptorPool;
    }
}
