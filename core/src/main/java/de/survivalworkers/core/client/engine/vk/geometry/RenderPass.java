package de.survivalworkers.core.client.engine.vk.geometry;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import  de.survivalworkers.core.client.engine.vk.rendering.Attachment;
import  de.survivalworkers.core.client.engine.vk.rendering.Device;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class RenderPass {
    private final Device device;
    @Getter
    private final long vkRenderPass;

    public RenderPass(Device device, List<Attachment> attachments) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int numAttachments = attachments.size();
            VkAttachmentDescription.Buffer attachmentsDesc = VkAttachmentDescription.calloc(numAttachments, stack);
            int depthAttachmentPos = 0;
            for (int i = 0; i < numAttachments; i++) {
                Attachment attachment = attachments.get(i);
                attachmentsDesc.get(i).format(attachment.getImage().getFormat()).loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE).stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                        .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE).samples(1).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                if (attachment.isDepthAttachment()) {
                    depthAttachmentPos = i;
                    attachmentsDesc.get(i).finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
                } else attachmentsDesc.get(i).finalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            }

            VkAttachmentReference.Buffer colorReferences = VkAttachmentReference.calloc(3, stack);
            for (int i = 0; i < 3; i++) {
                colorReferences.get(i).attachment(i).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }

            VkAttachmentReference depthReference = VkAttachmentReference.calloc(stack).attachment(depthAttachmentPos).layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack).pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS).pColorAttachments(colorReferences).colorAttachmentCount(colorReferences.capacity())
                    .pDepthStencilAttachment(depthReference);

            VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(2, stack);
            subpassDependencies.get(0).srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0).srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT).dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);

            subpassDependencies.get(1).srcSubpass(0).dstSubpass(VK_SUBPASS_EXTERNAL).srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstAccessMask(VK_ACCESS_MEMORY_READ_BIT).dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack).sType$Default().pAttachments(attachmentsDesc).pSubpasses(subpass).pDependencies(subpassDependencies);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateRenderPass(device.getDevice(), renderPassInfo, null, lp), "Could not create render pass");
            vkRenderPass = lp.get(0);
        }
    }

    public void close() {
        vkDestroyRenderPass(device.getDevice(), vkRenderPass, null);
    }

}
