package de.survivalworkers.core.client.engine.graphics.rendering;

import de.survivalworkers.core.client.vk.util.VkUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.slf4j.event.Level;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

@Slf4j
public class Instance implements Closeable {


    private final VkDebugUtilsMessengerCallbackEXT dbgFunc = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                Level level;
                if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
                    level = Level.DEBUG;
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                    level = Level.INFO;
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                    level = Level.WARN;
                } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                    level = Level.ERROR;
                } else {
                    level = Level.INFO;
                }

                String type;
                if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
                    type = "GENERAL";
                } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
                    type = "VALIDATION";
                } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
                    type = "PERFORMANCE";
                } else {
                    type = "UNKNOWN";
                }

                log.atLevel(level)
                        .addArgument(type)
                        .addArgument(() -> VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData).pMessageIdNameString())
                        .addArgument(() -> VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData).pMessageString())
                        .log("{}: [{}]\n\t{}\n");

                /*
                 * false indicates that layer should not bail-out of an
                 * API call that had validation failures. This may mean that the
                 * app dies inside the driver due to invalid parameter(s).
                 * That's what would happen without validation layers, so we'll
                 * keep that behavior here.
                 */
                return VK_FALSE;
            }
    );

    @Getter
    VkInstance handle;

    public Instance(String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pName = stack.ASCII(name);
            VkApplicationInfo appInfo = VkApplicationInfo.malloc(stack)
                    .sType$Default()
                    .pNext(MemoryUtil.NULL)
                    .pApplicationName(pName)
                    .applicationVersion(1)
                    .pEngineName(pName)
                    .engineVersion(0)
                    .apiVersion(VK_API_VERSION_1_0);

            PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null) {
                throw new RuntimeException("Failed to get GLFW instance extensions");
            }

            PointerBuffer requiredExtensions;

            if (log.isDebugEnabled()) {
                ByteBuffer debug = stack.ASCII(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
                requiredExtensions = stack.mallocPointer(glfwExtensions.remaining() + 1);
                requiredExtensions.put(glfwExtensions).put(debug);
            } else {
                requiredExtensions = stack.mallocPointer(glfwExtensions.remaining());
                requiredExtensions.put(glfwExtensions);
            }

            requiredExtensions.flip();

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.malloc(stack)
                    .sType$Default()
                    .flags(0)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(requiredExtensions);

            if (log.isDebugEnabled()) {
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                        .sType$Default()
                        .messageSeverity(
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        )
                        .messageType(
                                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                        )
                        .pfnUserCallback(dbgFunc);

                instanceCreateInfo.pNext(debugCreateInfo)
                        .ppEnabledLayerNames(getSupportedValidationLayers(stack));
            } else {
                instanceCreateInfo.pNext(MemoryUtil.NULL)
                        .ppEnabledLayerNames(null);
            }

            PointerBuffer pInstance = stack.mallocPointer(1);
            VkUtil.check(vkCreateInstance(instanceCreateInfo, null, pInstance),"Failed to initialize instance");
            handle = new VkInstance(pInstance.get(0), instanceCreateInfo);
        }
    }

    private PointerBuffer getSupportedValidationLayers(MemoryStack stack) {
            IntBuffer pPropertyCount = stack.mallocInt(1);
            vkEnumerateInstanceLayerProperties(pPropertyCount, null);
            int numLayers = pPropertyCount.get(0);
            VkLayerProperties.Buffer pProperties = VkLayerProperties.malloc(numLayers, stack);
            vkEnumerateInstanceLayerProperties(pPropertyCount, pProperties);

            Set<String> supportedLayers = new HashSet<>();
            for (int i = 0; i < numLayers; i++) {
                VkLayerProperties props = pProperties.get(i);
                String layerName = props.layerNameString();
                supportedLayers.add(layerName);
            }

            if (supportedLayers.contains("VK_LAYER_KHRONOS_validation")) {
                return stack.pointers(stack.ASCII("VK_LAYER_KHRONOS_validation"));
            }

            if (supportedLayers.contains("VK_LAYER_LUNARG_standard_validation")) {
                return stack.pointers(stack.ASCII("VK_LAYER_LUNARG_standard_validation"));
            }

            List<String> requestedLayers = List.of("VK_LAYER_GOOGLE_threading",
                    "VK_LAYER_LUNARG_parameter_validation",
                    "VK_LAYER_LUNARG_object_tracker",
                    "VK_LAYER_LUNARG_core_validation",
                    "VK_LAYER_GOOGLE_unique_objects");

            return stack.pointers(requestedLayers.stream()
                    .filter(supportedLayers::contains)
                    .map(stack::ASCII)
                    .toArray(ByteBuffer[]::new));
    }

    public void close(){
        vkDestroyInstance(handle,null);

        dbgFunc.free();
    }
}
