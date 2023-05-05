package de.survivalworkers.core.engine.graphics.pipeline;

import de.survivalworkers.core.engine.graphics.rendering.DescriptorSetLayout;
import de.survivalworkers.core.engine.graphics.shaders.ShaderProgram;
import de.survivalworkers.core.engine.graphics.vertex.VertexInputInfo;
import de.survivalworkers.core.vk.device.LogicalDevice;
import de.survivalworkers.core.vk.util.VkUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Pipeline implements Closeable{

    private final LogicalDevice device;
    private final long pipeline;
    private final long pipelineLayout;

    public Pipeline(PipelineCache pipelineCache,PipeLineCreateInfo createInfo) {
        this.device = pipelineCache.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);

            ByteBuffer name = stack.UTF8("main");

            ShaderProgram.ShaderModule[] shaderModules = createInfo.shaderProgram().getShaderModules();
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(shaderModules.length, stack);
            for (int i = 0; i < shaderModules.length; i++) {
                stages.get(i).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(shaderModules[i].shaderStage()).module(shaderModules[i].shaderModule()).pName(name);
            }

            VkPipelineInputAssemblyStateCreateInfo assemblyCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).
                    topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

            VkPipelineViewportStateCreateInfo pipelineCreateInfo = VkPipelineViewportStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO).viewportCount(1).scissorCount(1);

            VkPipelineRasterizationStateCreateInfo rasterizationCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO).polygonMode(VK_POLYGON_MODE_FILL).
                    cullMode(VK_CULL_MODE_NONE).frontFace(VK_FRONT_FACE_CLOCKWISE).lineWidth(1.0f);

            VkPipelineMultisampleStateCreateInfo sampleCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO).rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineColorBlendAttachmentState.Buffer blendState = VkPipelineColorBlendAttachmentState.calloc(createInfo.colorAttachments(), stack);
            for (int i = 0; i < createInfo.colorAttachments(); i++) {
                blendState.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT).blendEnable(createInfo.useBlend());
                if(createInfo.useBlend())blendState.get(i).colorBlendOp(VK_BLEND_OP_ADD).alphaBlendOp(VK_BLEND_OP_ADD).srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA).
                        dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA).srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE).dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            }

            VkPipelineColorBlendStateCreateInfo blendCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).pAttachments(blendState);

            VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).
                    pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkPushConstantRange.Buffer pushRange = null;
            if(createInfo.constantSize() > 0)pushRange = VkPushConstantRange.calloc(1,stack).stageFlags(VK_SHADER_STAGE_VERTEX_BIT).offset(0).size(createInfo.constantSize);

            LongBuffer ppLayout = stack.mallocLong(createInfo.descriptorLayouts().length);
            for (int i = 0; i < createInfo.descriptorLayouts().length; i++) {
                ppLayout.put(i,createInfo.descriptorLayouts()[i].getDescriptorLayout());
            }

            VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).pSetLayouts(ppLayout).pPushConstantRanges(pushRange);

            VkUtil.check(vkCreatePipelineLayout(device.getHandle(), layoutCreateInfo, null, lp), "Could not create Pipeline Layout");
            pipelineLayout = lp.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack).sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO).pStages(stages).
                    pVertexInputState(createInfo.inputStateInfo().getStateInfo()).pInputAssemblyState(assemblyCreateInfo).pViewportState(pipelineCreateInfo).pRasterizationState(rasterizationCreateInfo).pMultisampleState(sampleCreateInfo).
                    pColorBlendState(blendCreateInfo).pDynamicState(dynamicStateCreateInfo).layout(pipelineLayout).renderPass(createInfo.renderPass);

            //INFO depthBoundsTestEnable set to true for not rendering far away Objects
            if(createInfo.hasDepth()) {
                pipeline.pDepthStencilState(VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO).depthTestEnable(true).depthWriteEnable(true).
                        depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL).depthBoundsTestEnable(false).stencilTestEnable(false));
            }

            VkUtil.check(vkCreateGraphicsPipelines(device.getHandle(), pipelineCache.getPipelineCache(), pipeline, null, lp), "Could not create Graphics Pipeline");
            this.pipeline = lp.get(0);
        }

    }

    public void close(){
        vkDestroyPipeline(device.getHandle(), pipelineLayout,null);
        vkDestroyPipeline(device.getHandle(), pipeline,null);
    }

    public long getPipeline() {
        return pipeline;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    public record PipeLineCreateInfo(long renderPass, ShaderProgram shaderProgram, int colorAttachments, boolean hasDepth, int constantSize, VertexInputInfo inputStateInfo, DescriptorSetLayout[] descriptorLayouts, boolean useBlend)
    implements Closeable{
        public void close(){
            inputStateInfo.close();
        }
    }
}
