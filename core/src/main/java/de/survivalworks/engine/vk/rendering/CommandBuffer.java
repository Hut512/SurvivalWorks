package de.survivalworks.engine.vk.rendering;

import de.survivalworks.engine.vk.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class CommandBuffer {
    private final CommandPool pool;
    private final boolean submit;
    private final VkCommandBuffer cmdBuf;

    public CommandBuffer(CommandPool pool,boolean primary,boolean submit){
        this.pool = pool;
        this.submit = submit;
        VkDevice device = pool.getDevice().getDevice();

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo cmdInfo = VkCommandBufferAllocateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).commandPool(pool.getCommandPool()).
                    level(primary ? VK13.VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK13.VK_COMMAND_BUFFER_LEVEL_SECONDARY).commandBufferCount(1);
            PointerBuffer pb = stack.mallocPointer(1);
            Util.check(VK13.vkAllocateCommandBuffers(device,cmdInfo,pb),"Could not allocate cmd buffer");
            cmdBuf = new VkCommandBuffer(pb.get(0),device);
        }
    }

    public void beginRec(){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            if(submit)cmdBufInfo.flags(VK13.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            Util.check(VK13.vkBeginCommandBuffer(cmdBuf,cmdBufInfo),"Could not begin CMD Buf");
        }
    }

    public void delete(){
        vkFreeCommandBuffers(pool.getDevice().getDevice(),pool.getCommandPool(),cmdBuf);
    }

    public void endRec(){
        Util.check(VK13.vkEndCommandBuffer(cmdBuf),"Failed Ending buf");
    }

    public VkCommandBuffer getCmdBuf() {
        return cmdBuf;
    }

    public void reset(){
        VK13.vkResetCommandBuffer(cmdBuf,VK13.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
    }
}
