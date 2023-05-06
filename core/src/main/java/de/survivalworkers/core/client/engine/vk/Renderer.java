package de.survivalworkers.core.client.engine.vk;

import de.survivalworkers.core.client.engine.vk.pipeline.PipelineCache;
import de.survivalworkers.core.client.engine.vk.rendering.CommandPool;
import de.survivalworkers.core.client.engine.vk.rendering.Instance;
import de.survivalworkers.core.client.engine.vk.rendering.SwapChain;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.vertex.Model;
import de.survivalworkers.core.client.engine.vk.vertex.ModelData;
import de.survivalworkers.core.client.engine.vk.vertex.TextureCache;
import de.survivalworkers.core.client.SWWindow;
import de.survivalworkers.core.client.engine.vk.device.SWLogicalDevice;
import de.survivalworkers.core.client.engine.vk.device.SWPhysicalDevice;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class Renderer {
    @Getter
    private final Instance instance;
    @Getter
    private final SWPhysicalDevice physicalDevice;
    @Getter
    private final SWLogicalDevice logicalDevice;
    private SwapChain swapChain;
    private final CommandPool cmdPool;
    private final ForwardRenderActivity activity;
    private final List<Model> models;
    private final PipelineCache cache;
    private final Scene scene;
    private TextureCache textureCache;

    public Renderer(SWWindow window, Scene scene) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            instance = new Instance("Test");
            LongBuffer pSurface = stack.mallocLong(1);
            window.createSurface(instance.getHandle(), pSurface);
            long surface = pSurface.get(0);
            physicalDevice = new SWPhysicalDevice(instance, surface);
            logicalDevice = new SWLogicalDevice(physicalDevice);
            swapChain = new SwapChain(physicalDevice, logicalDevice, surface, window.getWidth(), window.getHeight());
            cmdPool = new CommandPool(logicalDevice, physicalDevice.getQueueFamilies().getGraphicsFamily());
            cache = new PipelineCache(logicalDevice);
            this.scene = scene;
            activity = new ForwardRenderActivity(swapChain, cmdPool, cache, scene);
            models = new ArrayList<>();
            textureCache = new TextureCache();
        }
    }

    public void loadModels(List<ModelData> modelDataList) {
        models.addAll(Model.transformModels(modelDataList, textureCache, cmdPool, logicalDevice.getGraphicsQueue()));
        models.forEach(m -> m.getMaterials().sort((a, b) -> Boolean.compare(a.isTransparent(), b.isTransparent())));
        models.sort((a, b) -> {
            boolean aTrans = a.getMaterials().stream().anyMatch(Model.Material::isTransparent);
            boolean bTrans = b.getMaterials().stream().anyMatch(Model.Material::isTransparent);
            return Boolean.compare(aTrans, bTrans);
        });
        activity.registerModels(models);
    }

    public void render(SWWindow window) {
        if (window.getWidth() <= 0 && window.getHeight() <= 0) return;

        if (window.isResized() || swapChain.acquireNextImage()) {
            window.setResized(false);
            resize(window);
            scene.getProjection().resize(window.getWidth(), window.getHeight());
            swapChain.acquireNextImage();
        }

        activity.recordCommandBuffer(models);
        activity.submit(logicalDevice.getPresentQueue());

        if (swapChain.presentImage(logicalDevice.getGraphicsQueue())) window.setResized(true);
    }

    public void close() {
        instance.close();
        logicalDevice.close();
        swapChain.close();
        cmdPool.close();
        activity.close();
        models.forEach(Model::close);
        textureCache.close();
    }

    private void resize(SWWindow window) {
        //Properties engProps = Properties.getInstance();

        logicalDevice.waitIdle();

        swapChain.resize(window.getWidth(), window.getHeight());
        activity.resize(swapChain);
    }
}
