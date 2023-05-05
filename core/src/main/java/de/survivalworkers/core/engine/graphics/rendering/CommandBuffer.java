package de.survivalworkers.core.engine.graphics.rendering;

import de.survivalworkers.core.vk.util.VkUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class CommandBuffer {
    private final CommandPool pool;
    private final boolean submit;
    private final VkCommandBuffer cmdBuf;

    public CommandBuffer(CommandPool pool,boolean primary,boolean submit){
        this.pool = pool;
        this.submit = submit;
        VkDevice device = pool.getDevice().getHandle();

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo cmdInfo = VkCommandBufferAllocateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).commandPool(pool.getCommandPool()).
                    level(primary ? VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK_COMMAND_BUFFER_LEVEL_SECONDARY).commandBufferCount(1);
            PointerBuffer pb = stack.mallocPointer(1);
            VkUtil.check(vkAllocateCommandBuffers(device,cmdInfo,pb),"Could not allocate cmd buffer");
            cmdBuf = new VkCommandBuffer(pb.get(0),device);
        }
    }

    public void beginRec(){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            if(submit)cmdBufInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            VkUtil.check(vkBeginCommandBuffer(cmdBuf,cmdBufInfo),"Could not begin CMD Buf");
        }
    }

    public void close() {
        vkFreeCommandBuffers(pool.getDevice().getHandle(),pool.getCommandPool(),cmdBuf);
    }

    public void endRec(){
        VkUtil.check(vkEndCommandBuffer(cmdBuf),"Failed Ending buf");
    }

    public VkCommandBuffer getCmdBuf() {
        return cmdBuf;
    }

    public void reset(){
        vkResetCommandBuffer(cmdBuf,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
    }
}
