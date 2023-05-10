package de.survivalworks.engine.vk.pipeline;

import de.survivalworks.engine.vk.Util;
import de.survivalworks.engine.vk.rendering.Device;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class PipelineCache {
    private final Device device;
    private final long pipelineCache;

    public PipelineCache(Device device){
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreatePipelineCache(device.getDevice(), createInfo, null, lp), "Error creating pipeline cache");
            pipelineCache = lp.get(0);
        }
    }

    public void delete(){
        VK13.vkDestroyPipelineCache(device.getDevice(),pipelineCache,null);
    }

    public Device getDevice() {
        return device;
    }

    public long getPipelineCache() {
        return pipelineCache;
    }
}
