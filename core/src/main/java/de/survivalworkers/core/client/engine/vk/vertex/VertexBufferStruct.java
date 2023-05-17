package de.survivalworkers.core.client.engine.vk.vertex;

import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK11.*;

public class VertexBufferStruct extends VertexInputInfo{
    private static final int NUM_ATTRIB = 5;
    private static final int POS_COMP = 3;
    private static final int TEX_COMP = 2;
    private static final int NORMAL_COMP = 3;
    private final VkVertexInputAttributeDescription.Buffer viAttrib;
    private final VkVertexInputBindingDescription.Buffer viDesc;

    public VertexBufferStruct(){
        viAttrib = VkVertexInputAttributeDescription.calloc(NUM_ATTRIB);
        viDesc = VkVertexInputBindingDescription.calloc(1);
        inputInfo = VkPipelineVertexInputStateCreateInfo.calloc();
        int i = 0;
        viAttrib.get(i).binding(0).location(i).format(VK_FORMAT_R32G32B32_SFLOAT).offset(0);
        i++;
        viAttrib.get(i).binding(0).location(i).format(VK_FORMAT_R32G32B32_SFLOAT).offset(POS_COMP * 4);
        i++;
        viAttrib.get(i).binding(0).location(i).format(VK_FORMAT_R32G32B32_SFLOAT).offset(NORMAL_COMP * 4 + POS_COMP * 4);
        i++;
        viAttrib.get(i).binding(0).location(i).format(VK_FORMAT_R32G32B32_SFLOAT).offset(NORMAL_COMP * 4 * 2 + POS_COMP * 4);
        i++;
        viAttrib.get(i).binding(0).location(i).format(VK_FORMAT_R32G32_SFLOAT).offset(NORMAL_COMP * 4 * 3 + POS_COMP * 4);
        viDesc.get(0).binding(0).stride(POS_COMP * 4 + NORMAL_COMP * 4 * 3 + TEX_COMP * 4).
                inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        inputInfo.sType$Default().pVertexBindingDescriptions(viDesc).pVertexAttributeDescriptions(viAttrib);
    }

    @Override
    public void close(){
        super.close();
        viDesc.free();
        viAttrib.free();
    }

}
