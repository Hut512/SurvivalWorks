package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.util.Util;
import de.survivalworkers.core.client.engine.vk.util.VkUtil;
import de.survivalworkers.core.client.engine.vk.device.SWLogicalDevice;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Buffer {
    private final long allocationSize;
    private final long buffer;
    private final SWLogicalDevice device;
    private final long memory;
    private final PointerBuffer pb;
    private final long requestedSize;

    private long mappedMemory;

    public Buffer(SWLogicalDevice device, long size, int usage, int reqMask){
        this.device = device;
        this.requestedSize = size;
        mappedMemory = MemoryUtil.NULL;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo createInfo = VkBufferCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO).size(size).usage(usage).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer lp = stack.mallocLong(1);
            VkUtil.check(vkCreateBuffer(device.getHandle(), createInfo,null,lp),"Could not create Buffer");
            buffer = lp.get(0);
            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device.getHandle(), buffer, memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).allocationSize(memoryRequirements.size()).
                    memoryTypeIndex(device.getPhysicalDevice().memoryType(memoryRequirements.memoryTypeBits(), reqMask));
            VkUtil.check(vkAllocateMemory(device.getHandle(), allocateInfo,null,lp),"Could not allocate Memory");
            allocationSize = allocateInfo.allocationSize();
            memory = lp.get(0);
            pb = MemoryUtil.memAllocPointer(1);

            VkUtil.check(vkBindBufferMemory(device.getHandle(), buffer,memory,0),"Could not bind Buffer Memory");
        }
    }

    public void close() {
        MemoryUtil.memFree(pb);
        vkDestroyBuffer(device.getHandle(), buffer,null);
        vkFreeMemory(device.getHandle(), memory,null);
    }

    public long getBuffer() {
        return buffer;
    }

    public long getRequestedSize() {
        return requestedSize;
    }

    public long map(){
        if(mappedMemory == MemoryUtil.NULL) {
            VkUtil.check(vkMapMemory(device.getHandle(), memory, 0, allocationSize, 0, pb), "could not Map Buffer");
            mappedMemory = pb.get(0);
        }
        return mappedMemory;
    }

    public void unMap(){
        if(mappedMemory != MemoryUtil.NULL){
            vkUnmapMemory(device.getHandle(), memory);
            mappedMemory = MemoryUtil.NULL;
        }
    }

    public void copyMatrixToBuffer(Matrix4f projectionMatrix) {
        copyMatrixToBuffer(projectionMatrix,0);
    }
    private void copyMatrixToBuffer(Matrix4f projectionMatrix, int i) {
        long mappedMem = this.map();
        ByteBuffer bp = MemoryUtil.memByteBuffer(mappedMem, (int) this.getRequestedSize());
        projectionMatrix.get(i,bp);
        this.unMap();
    }
}
