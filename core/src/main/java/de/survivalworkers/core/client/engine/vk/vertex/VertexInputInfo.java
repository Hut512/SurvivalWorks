package de.survivalworkers.core.client.engine.vk.vertex;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

public abstract class VertexInputInfo {

    protected VkPipelineVertexInputStateCreateInfo inputInfo;

    public void close() {
        inputInfo.free();
    }

    public VkPipelineVertexInputStateCreateInfo getInputInfo() {
        return inputInfo;
    }
}
