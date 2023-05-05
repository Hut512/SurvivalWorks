package de.survivalworkers.core.engine.graphics;

import de.survivalworkers.core.engine.graphics.pipeline.Pipeline;
import de.survivalworkers.core.engine.graphics.rendering.Buffer;
import de.survivalworkers.core.vk.device.PhysicalDevice;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class Util {
    private Util(){

    }

    public static int memoryType(PhysicalDevice physDevice, int typeBits, int reqsMask) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties pProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physDevice.getHandle(), pProperties);
            int res = -1;
            VkMemoryType.Buffer memoryTypes = pProperties.memoryTypes();
            for (int i = 0; i < VK_MAX_MEMORY_TYPES; i++) {
                if ((typeBits & 1) == 1 && (memoryTypes.get(i).propertyFlags() & reqsMask) == reqsMask) {
                    res = i;
                    break;
                }
                typeBits >>= 1;
            }
            if (res < 0) throw new RuntimeException("Could not get memoryType");

            return res;
        }
    }

    public static float[] toArrayFloat(List<Float> list){
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public static int[] toArrayInt(List<Integer> list){
        int[] arr = new int[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public static void copyMatrixToBuffer(Buffer projMatrixUniform, Matrix4f projectionMatrix) {
        copyMatrixToBuffer(projMatrixUniform,projectionMatrix,0);
    }

    private static void copyMatrixToBuffer(Buffer projMatrixUniform, Matrix4f projectionMatrix, int i) {
        long mappedMem = projMatrixUniform.map();
        ByteBuffer bp = MemoryUtil.memByteBuffer(mappedMem, (int) projMatrixUniform.getRequestedSize());
        projectionMatrix.get(i,bp);
        projMatrixUniform.unMap();
    }

    public static void setMAtrixAsPushConstant(Pipeline pipeLine, VkCommandBuffer cmdHandle, Matrix4f modelMatrix) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pb = stack.malloc(64);
            modelMatrix.get(0,pb);
            vkCmdPushConstants(cmdHandle,pipeLine.getPipelineLayout(), VK_SHADER_STAGE_VERTEX_BIT,0,pb);
        }
    }
}
