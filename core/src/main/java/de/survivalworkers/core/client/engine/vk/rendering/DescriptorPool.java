package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK11.*;

public class DescriptorPool {

    @Getter
    private final Device device;
    @Getter
    private final long descriptorPool;
    public DescriptorPool(Device device, List<DescriptorTypeCount> descriptors){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            int maxSets = 0;
            VkDescriptorPoolSize.Buffer typeCounts = VkDescriptorPoolSize.calloc(descriptors.size(),stack);
            for (int i = 0; i < descriptors.size(); i++) {
                maxSets += descriptors.get(i).count();
                typeCounts.get(i).type(descriptors.get(i).type()).descriptorCount(descriptors.get(i).count());
            }

            VkDescriptorPoolCreateInfo createInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default().flags(VK13.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT).pPoolSizes(typeCounts).
                    maxSets(maxSets);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateDescriptorPool(device.getDevice(),createInfo,null,lp),"Could not create descriptor Pool");
            descriptorPool = lp.get(0);
        }
    }

    public record DescriptorTypeCount(int count,int type){}

    public void freeDescriptorSet(long descriptorSet){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);
            lp.put(0,descriptorSet);

            Util.check(VK13.vkFreeDescriptorSets(device.getDevice(),descriptorPool,lp),"Could not free Descriptor Set");
        }
    }

    public void close(){
        VK13.vkDestroyDescriptorPool(device.getDevice(),descriptorPool,null);
    }

}
