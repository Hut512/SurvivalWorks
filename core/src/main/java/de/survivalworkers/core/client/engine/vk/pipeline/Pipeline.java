package de.survivalworkers.core.client.engine.vk.pipeline;

import de.survivalworkers.core.client.engine.vk.Util;
import de.survivalworkers.core.client.engine.vk.rendering.DescriptorSetLayout;
import de.survivalworkers.core.client.engine.vk.rendering.Device;
import de.survivalworkers.core.client.engine.vk.shaders.ShaderProgram;
import de.survivalworkers.core.client.engine.vk.vertex.VertexInputInfo;
import lombok.Getter;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.*;

import static org.lwjgl.vulkan.VK11.*;

public class Pipeline {

    private final Device device;
    @Getter
    private final long pipeline;
    @Getter
    private final long pipelineLayout;

    public Pipeline(PipelineCache pipelineCache,PipeLineCreateInfo createInfo) {
        this.device = pipelineCache.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);

            ByteBuffer name = stack.UTF8("main");

            ShaderProgram.ShaderModule[] shaderModules = createInfo.shaderProgram().getShaderModules();
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(shaderModules.length, stack);
            for (int i = 0; i < shaderModules.length; i++) {
                stages.get(i).sType$Default().stage(shaderModules[i].shaderStage()).module(shaderModules[i].handle()).pName(name);
            }

            VkPipelineInputAssemblyStateCreateInfo assemblyCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default().
                    topology(VK13.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

            VkPipelineViewportStateCreateInfo pipelineCreateInfo = VkPipelineViewportStateCreateInfo.calloc(stack).sType$Default().viewportCount(1).scissorCount(1);

            VkPipelineRasterizationStateCreateInfo rasterizationCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default().
                    polygonMode(VK13.VK_POLYGON_MODE_FILL).cullMode(VK13.VK_CULL_MODE_NONE).frontFace(VK13.VK_FRONT_FACE_CLOCKWISE).lineWidth(1.0f);

            VkPipelineMultisampleStateCreateInfo sampleCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default().rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineColorBlendAttachmentState.Buffer blendState = VkPipelineColorBlendAttachmentState.calloc(createInfo.colorAttachments(), stack);
            for (int i = 0; i < createInfo.colorAttachments(); i++) {
                blendState.get(i).colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT).blendEnable(createInfo.useBlend());
                if(createInfo.useBlend())blendState.get(i).colorBlendOp(VK_BLEND_OP_ADD).alphaBlendOp(VK_BLEND_OP_ADD).srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA).
                        dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA).srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE).dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            }

            VkPipelineColorBlendStateCreateInfo blendCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default().pAttachments(blendState);

            VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default().
                    pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkPushConstantRange.Buffer pushRange = null;
            if(createInfo.constantSize() > 0)pushRange = VkPushConstantRange.calloc(1,stack).stageFlags(VK_SHADER_STAGE_VERTEX_BIT).offset(0).size(createInfo.constantSize);

            LongBuffer ppLayout = stack.mallocLong(createInfo.descriptorLayouts().length);
            for (int i = 0; i < createInfo.descriptorLayouts().length; i++) {
                ppLayout.put(i, createInfo.descriptorLayouts()[i].getDescriptorLayout());
            }

            VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default().pSetLayouts(ppLayout).pPushConstantRanges(pushRange);

            Util.check(vkCreatePipelineLayout(device.getDevice(), layoutCreateInfo, null, lp), "Could not create Pipeline Layout");
            pipelineLayout = lp.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack).sType$Default().pStages(stages).
                    pVertexInputState(createInfo.inputStateInfo().getInputInfo()).pInputAssemblyState(assemblyCreateInfo).pViewportState(pipelineCreateInfo).pRasterizationState(rasterizationCreateInfo).pMultisampleState(sampleCreateInfo).
                    pColorBlendState(blendCreateInfo).pDynamicState(dynamicStateCreateInfo).layout(pipelineLayout).renderPass(createInfo.renderPass);

            //INFO depthBoundsTestEnable set to true for not rendering far away Objects
            if(createInfo.hasDepth()) {
                pipeline.pDepthStencilState(VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType$Default().depthTestEnable(true).depthWriteEnable(true).
                        depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL).depthBoundsTestEnable(false).stencilTestEnable(true));
            }

            Util.check(VK13.vkCreateGraphicsPipelines(device.getDevice(), pipelineCache.getPipelineCache(), pipeline, null, lp), "Could not create Graphics Pipeline");
            this.pipeline = lp.get(0);
        }

    }

    public void close(){
        VK13.vkDestroyPipeline(device.getDevice(),pipelineLayout,null);
        VK13.vkDestroyPipeline(device.getDevice(),pipeline,null);
    }

    public void setMAtrixAsPushConstant(VkCommandBuffer cmdHandle, Matrix4f modelMatrix) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pb = stack.malloc(64);
            modelMatrix.get(0,pb);
            VK13.vkCmdPushConstants(cmdHandle,this.getPipelineLayout(), VK13.VK_SHADER_STAGE_VERTEX_BIT,0,pb);
        }
    }

    public record PipeLineCreateInfo(long renderPass, ShaderProgram shaderProgram, int colorAttachments, boolean hasDepth, int constantSize, VertexInputInfo inputStateInfo, DescriptorSetLayout[] descriptorLayouts, boolean useBlend){
        public void close(){
            inputStateInfo.close();
        }
    }
}
