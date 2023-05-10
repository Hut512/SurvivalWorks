package de.survivalworks.engine.vk.rendering;

import de.survivalworks.Main;
import de.survivalworks.engine.vk.Util;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.slf4j.event.Level;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class Instance {
    VkInstance instance;
    public Instance(boolean validate){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer name = stack.UTF8(Main.NAME);
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_APPLICATION_INFO).pApplicationName(name).applicationVersion(1).pEngineName(name).engineVersion(0).
                    apiVersion(VK13.VK_API_VERSION_1_3);

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
            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack).sType(VK13.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO).pNext(extension).pApplicationInfo(appInfo).ppEnabledLayerNames(requiredLayers).
                    ppEnabledExtensionNames(requiredExtensions);
            PointerBuffer pInstance = stack.mallocPointer(1);
            Util.check(VK13.vkCreateInstance(instanceCreateInfo,null,pInstance),"Err in instance init");
            instance = new VkInstance(pInstance.get(0),instanceCreateInfo);
        }
    }

    private List<String> getSupportedValidationLayers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numLayersArr = stack.callocInt(1);
            VK13.vkEnumerateInstanceLayerProperties(numLayersArr, null);
            int numLayers = numLayersArr.get(0);
            VkLayerProperties.Buffer propsBuf = VkLayerProperties.calloc(numLayers, stack);
            VK13.vkEnumerateInstanceLayerProperties(numLayersArr, propsBuf);
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

            List<String> overlap = requestedLayers.stream().filter(supportedLayers::contains).toList();

            return overlap;
        }
    }

    public void delete(){
        VK13.vkDestroyInstance(instance,null);
    }

    public VkInstance getInstance() {
        return instance;
    }
    private static VkDebugUtilsMessengerCreateInfoEXT createDebug(){
        return VkDebugUtilsMessengerCreateInfoEXT.calloc().sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT).messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT).
                messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT).
                pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) ->{
                    log.debug(VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData).pMessageString());
                    return VK13.VK_FALSE;
                });
    }
}
