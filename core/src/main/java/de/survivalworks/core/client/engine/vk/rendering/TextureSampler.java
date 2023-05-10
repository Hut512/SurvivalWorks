package de.survivalworks.core.client.engine.vk.rendering;

import de.survivalworks.core.client.engine.vk.Util;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

public class TextureSampler {
    private static final int MAX_ANISOTROPY = 16;
    private final Device device;
    private final long sampler;

    public TextureSampler(Device device,int mipLvl){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo createInfo = VkSamplerCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO).magFilter(VK13.VK_FILTER_LINEAR).minFilter(VK13.VK_FILTER_LINEAR).
                    addressModeU(VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT).addressModeV(VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT).addressModeW(VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT).borderColor(VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK).
                    unnormalizedCoordinates(false).compareEnable(false).compareOp(VK13.VK_SAMPLER_MIPMAP_MODE_LINEAR).minLod(0.0f).maxLod(mipLvl).mipLodBias(0.0f);
            if(device.isSamplerAnisotropy())createInfo.anisotropyEnable(true).maxAnisotropy(MAX_ANISOTROPY);
            LongBuffer lp = stack.mallocLong(1);
            Util.check(VK13.vkCreateSampler(device.getDevice(),createInfo,null,lp),"Could not create Sampler");
            sampler = lp.get(0);
        }

    }

    public void delete(){
        VK13.vkDestroySampler(device.getDevice(),sampler,null);
    }

    public long getSampler() {
        return sampler;
    }
}
