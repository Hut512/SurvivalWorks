package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK11.*;

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
            VkBufferCreateInfo createInfo = VkBufferCreateInfo.calloc(stack).sType$Default().size(size).usage(usage).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateBuffer(device.getDevice(),createInfo,null,lp),"Could not create Buffer");
            buffer = lp.get(0);
            VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
            VK13.vkGetBufferMemoryRequirements(device.getDevice(),buffer,memoryRequirements);

            VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack).sType$Default().allocationSize(memoryRequirements.size()).
                    memoryTypeIndex(device.getPhysicalDevice().memoryType(memoryRequirements.memoryTypeBits(),reqMask));
            Util.check(VK13.vkAllocateMemory(device.getDevice(),allocateInfo,null,lp),"Could not allocate Memory");
            allocationSize = allocateInfo.allocationSize();
            memory = lp.get(0);
            pb = MemoryUtil.memAllocPointer(1);

            Util.check(vkBindBufferMemory(device.getDevice(),buffer,memory,0),"Could not bind Buffer Memory");
        }
    }

    public void close() {
        MemoryUtil.memFree(pb);
        vkDestroyBuffer(device.getDevice(), buffer, null);
        vkFreeMemory(device.getDevice(), memory, null);
    }

    public long getBuffer() {
        return buffer;
    }

    public long getRequestedSize() {
        return requestedSize;
    }

    public long map(){
        if (mappedMemory == NULL) {
            Util.check(vkMapMemory(device.getDevice(), memory, 0, allocationSize, 0, pb), "Could not map Buffer");
            mappedMemory = pb.get(0);
        }
        return mappedMemory;
    }

    public void unMap(){
        if (mappedMemory != NULL) {
            vkUnmapMemory(device.getDevice(), memory);
            mappedMemory = NULL;
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
