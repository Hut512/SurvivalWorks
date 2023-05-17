package de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK11.*;

public class CommandBuffer {

    @Getter
    private final CommandPool pool;
    private final boolean submit;
    @Getter
    private final VkCommandBuffer cmdBuf;

    public CommandBuffer(CommandPool pool,boolean primary,boolean submit){
        this.pool = pool;
        this.submit = submit;
        VkDevice device = pool.getDevice().getDevice();

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo cmdInfo = VkCommandBufferAllocateInfo.calloc(stack).sType$Default().commandPool(pool.getCommandPool()).
                    level(primary ? VK13.VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK13.VK_COMMAND_BUFFER_LEVEL_SECONDARY).commandBufferCount(1);
            PointerBuffer pb = stack.mallocPointer(1);
            Util.check(VK13.vkAllocateCommandBuffers(device,cmdInfo,pb),"Could not allocate cmd buffer");
            cmdBuf = new VkCommandBuffer(pb.get(0),device);
        }
    }

    public void beginRec(){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc(stack).sType$Default();
            if(submit)cmdBufInfo.flags(VK13.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            Util.check(VK13.vkBeginCommandBuffer(cmdBuf,cmdBufInfo),"Could not begin CMD Buf");
        }
    }

    public void close() {
        vkFreeCommandBuffers(pool.getDevice().getDevice(),pool.getCommandPool(),cmdBuf);
    }

    public void endRec() {
        Util.check(vkEndCommandBuffer(cmdBuf),"Could not end command buffer");
    }
    public void reset() {
        vkResetCommandBuffer(cmdBuf,VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
    }
}
