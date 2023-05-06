package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.util.VkUtil;
import de.survivalworkers.core.client.engine.vk.device.SWLogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class DescriptorPool {
    private final SWLogicalDevice device;
    private final long descriptorPool;
    public DescriptorPool(SWLogicalDevice device, List<DescriptorTypeCount> descriptors){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            int maxSets = 0;
            VkDescriptorPoolSize.Buffer typeCounts = VkDescriptorPoolSize.calloc(descriptors.size(),stack);
            for (int i = 0; i < descriptors.size(); i++) {
                maxSets += descriptors.get(i).count();
                typeCounts.get(i).type(descriptors.get(i).type).descriptorCount(descriptors.get(i).count());
            }

            VkDescriptorPoolCreateInfo createInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO).flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT).pPoolSizes(typeCounts).
                    maxSets(maxSets);

            LongBuffer lp = stack.mallocLong(1);
            VkUtil.check(vkCreateDescriptorPool(device.getHandle(),createInfo,null,lp),"Could not create descriptor Pool");
            descriptorPool = lp.get(0);
        }
    }

    public record DescriptorTypeCount(int count,int type){}

    public void freeDescriptors(long descriptorSet){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);
            lp.put(0,descriptorSet);

            VkUtil.check(vkFreeDescriptorSets(device.getHandle(),descriptorPool,lp),"Could not free Descriptor Set");
        }
    }

    public void close() {
        vkDestroyDescriptorPool(device.getHandle(),descriptorPool,null);
    }

    public SWLogicalDevice getDevice() {
        return device;
    }

    public long getDescriptorPool() {
        return descriptorPool;
    }
}
