package de.survivalworkers.core.client.engine.graphics.rendering;

import de.survivalworkers.core.client.vk.util.VkUtil;
import de.survivalworkers.core.client.vk.device.SWLogicalDevice;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.io.Closeable;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class TextureSampler implements Closeable {
    private static final int MAX_ANISOTROPY = 16;
    private final SWLogicalDevice device;
    @Getter
    private final long handle;

    public TextureSampler(SWLogicalDevice device, int mipLvl){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo createInfo = VkSamplerCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO).magFilter(VK_FILTER_LINEAR).minFilter(VK_FILTER_LINEAR).
                    addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT).addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT).addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT).borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK).
                    unnormalizedCoordinates(false).compareEnable(false).compareOp(VK_SAMPLER_MIPMAP_MODE_LINEAR).minLod(0.0f).maxLod(mipLvl).mipLodBias(0.0f);
            createInfo.anisotropyEnable(true).maxAnisotropy(MAX_ANISOTROPY);
            LongBuffer lp = stack.mallocLong(1);
            VkUtil.check(vkCreateSampler(device.getHandle(),createInfo,null,lp),"Could not create Sampler");
            handle = lp.get(0);
        }

    }

    public void close(){
        vkDestroySampler(device.getHandle(),handle,null);
    }
}
