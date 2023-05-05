package de.survivalworkers.core.vk;

import de.survivalworkers.core.engine.graphics.ForwardRenderActivity;
import de.survivalworkers.core.engine.graphics.pipeline.PipelineCache;
import de.survivalworkers.core.engine.graphics.rendering.CommandPool;
import de.survivalworkers.core.engine.graphics.rendering.Instance;
import de.survivalworkers.core.engine.graphics.rendering.SwapChain;
import de.survivalworkers.core.engine.graphics.scene.Scene;
import de.survivalworkers.core.engine.graphics.vertex.Model;
import de.survivalworkers.core.engine.graphics.vertex.ModelData;
import de.survivalworkers.core.engine.graphics.vertex.TextureCache;
import de.survivalworkers.core.Window;
import de.survivalworkers.core.vk.device.LogicalDevice;
import de.survivalworkers.core.vk.device.PhysicalDevice;
import lombok.Getter;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class Renderer {
    @Getter
    private final Instance instance;
    @Getter
    private final PhysicalDevice physicalDevice;
    @Getter
    private final LogicalDevice logicalDevice;
    private SwapChain swapChain;
    private final CommandPool cmdPool;
    private final ForwardRenderActivity activity;
    private final List<Model> models;
    private final PipelineCache cache;
    private final Scene scene;
    private TextureCache textureCache;

    public Renderer(Window window, Scene scene) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            instance = new Instance("Test");
            LongBuffer pSurface = stack.mallocLong(1);
            window.createSurface(instance.getHandle(), pSurface);
            long surface = pSurface.get(0);
            physicalDevice = new PhysicalDevice(instance, surface);
            logicalDevice = new LogicalDevice(physicalDevice);
            swapChain = new SwapChain(physicalDevice, logicalDevice, surface, window.getWidth(), window.getHeight());
            cmdPool = new CommandPool(logicalDevice, physicalDevice.queryQueueFamilies(stack).getGraphicsFamily());
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

    public void render(Window window) {
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

    private void resize(Window window) {
        //Properties engProps = Properties.getInstance();

        logicalDevice.waitIdle();

        swapChain.close();

        swapChain.resize(window.getWidth(), window.getHeight());
        activity.resize(swapChain);
    }
}
