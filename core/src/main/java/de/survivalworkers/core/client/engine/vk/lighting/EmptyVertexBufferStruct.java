package de.survivalworkers.core.client.engine.vk.lighting;

import de.survivalworkers.core.client.engine.vk.vertex.VertexInputInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;

public class EmptyVertexBufferStruct extends VertexInputInfo {

    public EmptyVertexBufferStruct() {
        inputInfo = VkPipelineVertexInputStateCreateInfo.calloc();
        inputInfo.sType$Default().pVertexBindingDescriptions(null).pVertexAttributeDescriptions(null);
    }
}