package de.survivalworkers.core.client.engine.graphics.vertex;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

public abstract class VertexInputInfo {
    protected VkPipelineVertexInputStateCreateInfo stateInfo;
    public void close() {
        stateInfo.free();
    }

    public VkPipelineVertexInputStateCreateInfo getStateInfo() {
        return stateInfo;
    }
}
