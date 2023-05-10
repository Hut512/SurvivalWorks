package de.survivalworks.core.client.engine.vk;

import de.survivalworks.core.client.engine.vk.pipeline.Pipeline;
import de.survivalworks.core.client.engine.vk.rendering.Buffer;
import de.survivalworks.core.client.engine.vk.rendering.PhysicalDevice;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryType;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class Util {
    private Util(){

    }
    public static String getCode(int i){
        switch (i) {
            // Success codes
            case VK_SUCCESS:
                return "0";
            case VK_NOT_READY:
                return "A fence or query has not yet completed.";
            case VK_TIMEOUT:
                return "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET:
                return "An event is signaled.";
            case VK_EVENT_RESET:
                return "An event is unsignaled.";
            case VK_INCOMPLETE:
                return "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR:
                return "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY:
                return "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY:
                return "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED:
                return "Initialization of an object could not be completed for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST:
                return "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED:
                return "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT:
                return "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT:
                return "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT:
                return "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER:
                return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS:
                return "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED:
                return "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR:
                return "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
                return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR:
                return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
                        + "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue"
                        + "presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR:
                return "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"
                        + " image.";
            case VK_ERROR_VALIDATION_FAILED_EXT:
                return "A validation layer found an error.";
            default:
                return String.format("%s [%d]", "Unknown", i);
        }
    }

    public static void check(int i,String errMsg){
        String code = getCode(i);
        if(!code.equals("0"))throw new RuntimeException("Code:" + code + " : " + errMsg);
    }

    public static int memoryType(PhysicalDevice physDevice, int typeBits, int reqsMask) {
        int res = -1;
        VkMemoryType.Buffer memoryTypes = physDevice.getMemoryProperties().memoryTypes();
        for (int i = 0; i < VK13.VK_MAX_MEMORY_TYPES; i++) {
            if ((typeBits & 1) == 1 && (memoryTypes.get(i).propertyFlags() & reqsMask) == reqsMask) {
                res = i;
                break;
            }
            typeBits >>= 1;
        }
        if (res < 0) throw new RuntimeException("Could not get memoryType");

        return res;
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
            VK13.vkCmdPushConstants(cmdHandle,pipeLine.getPipelineLayout(), VK13.VK_SHADER_STAGE_VERTEX_BIT,0,pb);
        }
    }
}
