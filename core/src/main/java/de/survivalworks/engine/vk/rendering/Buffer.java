package de.survivalworks.engine.vk.rendering;

import de.survivalworks.engine.vk.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.LongBuffer;

public class Buffer {
    private final long allocationSize;
    private final long buffer;
    private final Device device;
    private final long memory;
    private final PointerBuffer pb;
    private final long requestedSize;

    private long mappedMemory;

    public Buffer(Device device,long size,int usage,int reqMask){
        this.device = device;
        this.requestedSize = size;
        mappedMemory = MemoryUtil.NULL;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo createInfo = VkBufferCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO).size(size).usage(usage).sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateBuffer(device.getDevice(),createInfo,null,lp),"Could not create Buffer");
            buffer = lp.get(0);
            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
            VK13.vkGetBufferMemoryRequirements(device.getDevice(),buffer,memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).allocationSize(memoryRequirements.size()).
                    memoryTypeIndex(Util.memoryType(device.getPhysicalDevice(),memoryRequirements.memoryTypeBits(),reqMask));
            Util.check(VK13.vkAllocateMemory(device.getDevice(),allocateInfo,null,lp),"Could not allocate Memory");
            allocationSize = allocateInfo.allocationSize();
            memory = lp.get(0);
            pb = MemoryUtil.memAllocPointer(1);

            Util.check(VK13.vkBindBufferMemory(device.getDevice(),buffer,memory,0),"Could not bind Buffer Memory");

        }
    }

    public void delete(){
        MemoryUtil.memFree(pb);
        VK13.vkDestroyBuffer(device.getDevice(),buffer,null);
        VK13.vkFreeMemory(device.getDevice(),memory,null);
    }

    public long getBuffer() {
        return buffer;
    }

    public long getRequestedSize() {
        return requestedSize;
    }

    public long map(){
        if(mappedMemory == MemoryUtil.NULL) {
            Util.check(VK13.vkMapMemory(device.getDevice(), memory, 0, allocationSize, 0, pb), "could not Map Buffer");
            mappedMemory = pb.get(0);
        }
        return mappedMemory;
    }

    public void unMap(){
        if(mappedMemory != MemoryUtil.NULL){
            VK13.vkUnmapMemory(device.getDevice(),memory);
            mappedMemory = MemoryUtil.NULL;
        }
    }
}
