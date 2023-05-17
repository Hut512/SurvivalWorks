package de.survivalworkers.core.client.engine.vk.shaders;

import de.survivalworkers.core.client.engine.vk.Util;
import de.survivalworkers.core.client.engine.vk.rendering.Device;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.*;
import java.nio.*;
import java.nio.file.Files;

import static org.lwjgl.vulkan.VK11.*;

public class ShaderProgram {

    private final Device device;
    @Getter
    private final ShaderModule[] shaderModules;

    public ShaderProgram(Device device, ShaderModuleData[] shaderModuleData) {
        try {
            this.device = device;
            int numModules = shaderModuleData != null ? shaderModuleData.length : 0;
            shaderModules = new ShaderModule[numModules];
            for (int i = 0; i < numModules; i++) {
                byte[] moduleContents = Files.readAllBytes(new File(shaderModuleData[i].shaderSpvFile()).toPath());
                long moduleHandle = createShaderModule(moduleContents);
                shaderModules[i] = new ShaderModule(shaderModuleData[i].shaderStage(), moduleHandle);
            }
        } catch (IOException excp) {
            throw new RuntimeException(excp);
        }
    }

    public void close() {
        for (ShaderModule shaderModule : shaderModules) {
            vkDestroyShaderModule(device.getDevice(), shaderModule.handle(), null);
        }
    }

    private long createShaderModule(byte[] code) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pCode = stack.malloc(code.length).put(0, code);
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack).sType$Default().pCode(pCode);

            LongBuffer lp = stack.mallocLong(1);
            Util.check(vkCreateShaderModule(device.getDevice(), createInfo, null, lp), "Could not create Shader Module");
            return lp.get(0);
        }
    }

    public record ShaderModule(int shaderStage, long handle) {
    }

    public record ShaderModuleData(int shaderStage, String shaderSpvFile) {
    }
}
