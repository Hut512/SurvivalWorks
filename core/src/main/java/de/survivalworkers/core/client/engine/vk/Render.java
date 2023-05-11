package de.survivalworkers.core.client.engine.vk;

import de.survivalworkers.core.client.engine.vk.rendering.*;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.vertex.Model;
import de.survivalworkers.core.client.engine.vk.vertex.ModelData;
import de.survivalworkers.core.client.engine.vk.vertex.TextureCache;
import de.survivalworkers.core.client.engine.Window;
import de.survivalworkers.core.client.engine.vk.pipeline.PipelineCache;

import java.util.ArrayList;
import java.util.List;

public class Render {
    private final Instance instance;
    private final Device device;
    private final Queue.GraphicsQueue gQueue;
    private final PhysicalDevice physicalDevice;
    private final Surface surface;
    private SwapChain swapChain;
    private final CommandPool cmdPool;
    private final Queue.PresentQueue pQueue;
    private final ForwardRenderActivity activity;
    private final List<Model> models;
    private final PipelineCache cache;
    private final Scene scene;
    private TextureCache textureCache;

    public Render(Window window,Scene scene){
        instance = new Instance(true);
        physicalDevice = PhysicalDevice.create(instance, "NVIDIA GeForce RTX 2070 SUPER");
        device = new Device(physicalDevice);
        surface = new Surface(physicalDevice,window.window());
        gQueue = new Queue.GraphicsQueue(device,0);
        pQueue = new Queue.PresentQueue(device,surface,0);
        swapChain = new SwapChain(device,surface,window,3);
        cmdPool = new CommandPool(device, Queue.GraphicsQueue.getGraphicsQueueFamilyIndex(device));
        cache = new PipelineCache(device);
        this.scene = scene;
        activity = new ForwardRenderActivity(swapChain,cmdPool,cache,scene);
        models = new ArrayList<>();
        textureCache = new TextureCache();
    }

    public void loadModels(List<ModelData> modelDataList){
        models.addAll(Model.transformModels(modelDataList,textureCache,cmdPool,gQueue));
        models.forEach(m -> m.getMaterials().sort((a, b) -> Boolean.compare(a.isTransparent(), b.isTransparent())));
        models.sort((a, b) -> {
            boolean aTrans = a.getMaterials().stream().anyMatch(Model.Material::isTransparent);
            boolean bTrans = b.getMaterials().stream().anyMatch(Model.Material::isTransparent);
            return Boolean.compare(aTrans, bTrans);
        });
        activity.registerModels(models);
    }

    public void render(Window window){
        if (window.getWidth() <= 0 && window.getHeight() <= 0) return;

        if (window.isResized() || swapChain.acquireNextImage()) {
            window.resetResized();
            resize(window);
            scene.getProjection().resize(window.getWidth(), window.getHeight());
            swapChain.acquireNextImage();
        }

        activity.recordCommandBuffer(models);
        activity.submit(pQueue);

        if (swapChain.presentImage(gQueue)) window.setResized(true);
    }

    public void delete(){
        instance.delete();
        physicalDevice.delete();
        device.delete();
        surface.delete();
        swapChain.delete();
        cmdPool.delete();
        activity.delete();
        models.forEach(Model::delete);
        textureCache.delete();
    }

    public Scene getScene() {
        return scene;
    }

    private void resize(Window window) {
        device.waitIdle();
        gQueue.waitIdle();

        swapChain.delete();

        swapChain = new SwapChain(device, surface, window, 3);
        activity.resize(swapChain);
    }
}
