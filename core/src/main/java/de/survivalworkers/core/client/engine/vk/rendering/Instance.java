package  de.survivalworkers.core.client.engine.vk.rendering;

import de.survivalworkers.core.client.engine.vk.Util;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;

import java.nio.*;
import java.util.*;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

@Slf4j
public class Instance {

    @Getter
    private final VkInstance instance;
    public Instance(boolean validate){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer name = stack.UTF8("Test");
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack).sType$Default().pApplicationName(name).applicationVersion(1).pEngineName(name).engineVersion(0).
                    apiVersion(VK_API_VERSION_1_3);

            List<String> validationLayers = getSupportedValidationLayers();
            boolean supportsValidation = validate;
            if (validate && validationLayers.size() == 0) {
                supportsValidation = false;
            }

            PointerBuffer requiredLayers = null;
            if (supportsValidation) {
                requiredLayers = stack.mallocPointer(validationLayers.size());
                for (int i = 0; i < validationLayers.size(); i++) {
                    requiredLayers.put(i, stack.ASCII(validationLayers.get(i)));
                }
            }

            PointerBuffer glfwExtension = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if(glfwExtension == null)throw new RuntimeException("No GLFW Surface Extensions");
            PointerBuffer requiredExtensions;
            if (supportsValidation) {
                ByteBuffer debug = stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
                requiredExtensions = stack.mallocPointer(glfwExtension.remaining() + 1);
                requiredExtensions.put(glfwExtension).put(debug);
            } else {
                requiredExtensions = stack.mallocPointer(glfwExtension.remaining());
                requiredExtensions.put(glfwExtension);
            }
            requiredExtensions.flip();

            long extension = MemoryUtil.NULL;
            if(supportsValidation)extension = createDebug().address();
            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack).sType$Default().pNext(extension).pApplicationInfo(appInfo).ppEnabledLayerNames(requiredLayers).
                    ppEnabledExtensionNames(requiredExtensions);
            PointerBuffer pInstance = stack.mallocPointer(1);
            Util.check(vkCreateInstance(instanceCreateInfo,null,pInstance),"Err in instance init");
            instance = new VkInstance(pInstance.get(0),instanceCreateInfo);
        }
    }

    private List<String> getSupportedValidationLayers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numLayersArr = stack.callocInt(1);
            vkEnumerateInstanceLayerProperties(numLayersArr, null);
            int numLayers = numLayersArr.get(0);
            VkLayerProperties.Buffer propsBuf = VkLayerProperties.calloc(numLayers, stack);
            vkEnumerateInstanceLayerProperties(numLayersArr, propsBuf);
            List<String> supportedLayers = new ArrayList<>();
            for (int i = 0; i < numLayers; i++) {
                VkLayerProperties props = propsBuf.get(i);
                String layerName = props.layerNameString();
                supportedLayers.add(layerName);
            }
            List<String> layersToUse = new ArrayList<>();

            if (supportedLayers.contains("VK_LAYER_KHRONOS_validation")) {
                layersToUse.add("VK_LAYER_KHRONOS_validation");
                return layersToUse;
            }

            if (supportedLayers.contains("VK_LAYER_LUNARG_standard_validation")) {
                layersToUse.add("VK_LAYER_LUNARG_standard_validation");
                return layersToUse;
            }

            List<String> requestedLayers = new ArrayList<>();
            requestedLayers.add("VK_LAYER_GOOGLE_threading");
            requestedLayers.add("VK_LAYER_LUNARG_parameter_validation");
            requestedLayers.add("VK_LAYER_LUNARG_object_tracker");
            requestedLayers.add("VK_LAYER_LUNARG_core_validation");
            requestedLayers.add("VK_LAYER_GOOGLE_unique_objects");

            return requestedLayers.stream().filter(supportedLayers::contains).toList();
        }
    }
    public void close(){
        vkDestroyInstance(instance,null);
    }
    private static VkDebugUtilsMessengerCreateInfoEXT createDebug(){
        return VkDebugUtilsMessengerCreateInfoEXT.calloc().sType$Default().messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT).
                messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT).
                pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) ->{
                    log.debug(VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData).pMessageString());
                    return VK13.VK_FALSE;
                });
    }
}
