package de.survivalworkers.core.client.engine.vk.lighting;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import  de.survivalworkers.core.client.engine.vk.rendering.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class LigRenderPass {

    private final Device device;
    @Getter
    private final long vkRenderPass;

    public LigRenderPass(SwapChain swapChain) {
        device = swapChain.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1, stack);

            attachments.get(0).format(swapChain.getSurfaceFormat().imageFormat()).samples(VK_SAMPLE_COUNT_1_BIT).loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE).stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED).finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack).attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subPass = VkSubpassDescription.calloc(1, stack).pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS).colorAttachmentCount(colorReference.remaining()).pColorAttachments(colorReference);

            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(1, stack);
            subpassDependencies.get(0).srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0).srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).srcAccessMask(0)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack).sType$Default().pAttachments(attachments).pSubpasses(subPass).pDependencies(subpassDependencies);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateRenderPass(device.getDevice(), renderPassInfo, null, lp), "Could not create render pass");
            vkRenderPass = lp.get(0);
        }
    }

    public void close() {
        vkDestroyRenderPass(device.getDevice(), vkRenderPass, null);
    }
}
