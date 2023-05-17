package de.survivalworkers.core.client.engine.vk.pipeline;

import de.survivalworkers.core.client.engine.vk.Util;
import de.survivalworkers.core.client.engine.vk.rendering.Device;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class PipelineCache {

    @Getter
    private final Device device;
    @Getter
    private final long pipelineCache;

    public PipelineCache(Device device){
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.calloc(stack).sType$Default();

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreatePipelineCache(device.getDevice(), createInfo, null, lp), "Could not create pipeline cache");
            pipelineCache = lp.get(0);
        }
    }

    public void close() {
        vkDestroyPipelineCache(device.getDevice(), pipelineCache, null);
    }
}
