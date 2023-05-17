package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class TextureSampler {

    private static final int MAX_ANISOTROPY = 16;

    private final Device device;
    @Getter
    private final long sampler;

    public TextureSampler(Device device,int mipLvl){
        this.device = device;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo createInfo = VkSamplerCreateInfo.calloc(stack).sType$Default().magFilter(VK_FILTER_LINEAR).minFilter(VK_FILTER_LINEAR).
                    addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT).addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT).addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT).borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK).
                    unnormalizedCoordinates(false).compareEnable(false).compareOp(VK_COMPARE_OP_ALWAYS).mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR).minLod(0.0f).maxLod(mipLvl).mipLodBias(0.0f);
            if(device.isSamplerAnisotropy())createInfo.anisotropyEnable(true).maxAnisotropy(MAX_ANISOTROPY);
            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateSampler(device.getDevice(),createInfo,null,lp),"Could not create sampler");
            sampler = lp.get(0);
        }
    }

    public void close() {
        vkDestroySampler(device.getDevice(),sampler,null);
    }
}
