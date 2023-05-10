package de.survivalworkers.core.client.engine.vk.shaders;

import de.survivalworkers.core.client.engine.vk.Util;
import de.survivalworkers.core.client.engine.vk.rendering.Device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;

import static org.lwjgl.vulkan.VK10.*;

public class ShaderProgram {
        private final Device device;
        private final ShaderModule[] shaderModules;

        public ShaderProgram(Device device, ShaderModuleData[] shaderModuledata) {
                try {
                        this.device = device;
                        int numModules = shaderModuledata != null ? shaderModuledata.length : 0;
                        shaderModules = new ShaderModule[numModules];
                        for (int i = 0; i < numModules; i++) {
                                byte[] moduleCont = Files.readAllBytes(new File(shaderModuledata[i].shaderSpvFile).toPath());
                                long module = createShaderModule(moduleCont);
                                shaderModules[i] = new ShaderModule(shaderModuledata[i].shaderStage(), module);
                        }
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }

        public void delete() {
                for (ShaderModule shaderModule : shaderModules) {
                        VK13.vkDestroyShaderModule(device.getDevice(), shaderModule.shaderModule(), null);
                }
        }

        private long createShaderModule(byte[] code) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                        ByteBuffer pCode = stack.malloc(code.length).put(0, code);
                        VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(pCode);

                        LongBuffer lp = stack.mallocLong(1);
                        Util.check(VK13.vkCreateShaderModule(device.getDevice(), createInfo, null, lp), "Could not create Shader Module");
                        return lp.get(0);
                }
        }

        public ShaderModule[] getShaderModules() {
                return shaderModules;
        }

        public record ShaderModule(int shaderStage, long shaderModule) {
        }

        public record ShaderModuleData(int shaderStage, String shaderSpvFile) {
        }
}
