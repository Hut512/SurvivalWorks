package de.survivalworkers.core.client.engine.vk;

import de.survivalworkers.core.client.engine.SWWindow;
import de.survivalworkers.core.client.engine.vk.geometry.GeoRenderActitity;
import de.survivalworkers.core.client.engine.vk.lighting.LigRenderActivity;
import de.survivalworkers.core.client.engine.vk.rendering.*;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.vertex.Model;
import de.survivalworkers.core.client.engine.vk.vertex.ModelData;
import de.survivalworkers.core.client.engine.vk.vertex.TextureCache;
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
    //private final ForwardRenderActivity activity;
    private final List<Model> models;
    private final PipelineCache cache;
    private final Scene scene;
    private TextureCache textureCache;
    private GeoRenderActitity geoActivity;
    private LigRenderActivity ligActivity;

    public Render(SWWindow window, Scene scene){
        instance = new Instance(true);
        physicalDevice = PhysicalDevice.create(instance, "NVIDIA GeForce RTX 2070 SUPER");
        device = new Device(physicalDevice);
        surface = new Surface(physicalDevice,window.getHandle());
        gQueue = new Queue.GraphicsQueue(device,0);
        pQueue = new Queue.PresentQueue(device,surface,0);
        swapChain = new SwapChain(device,surface,window,3,false);
        cmdPool = new CommandPool(device, Queue.GraphicsQueue.getGraphicsQueueFamilyIndex(device));
        cache = new PipelineCache(device);
        this.scene = scene;
        //activity = new ForwardRenderActivity(swapChain,cmdPool,cache,scene);
        models = new ArrayList<>();
        textureCache = new TextureCache();
        geoActivity = new GeoRenderActitity(swapChain,cmdPool,cache,scene);
        ligActivity = new LigRenderActivity(swapChain,cmdPool,cache,geoActivity.getAttachments(),scene);
    }

    public void loadModels(List<ModelData> modelDataList){
        models.addAll(Model.transformModels(modelDataList,textureCache,cmdPool,gQueue));
       /* models.forEach(m -> m.getMaterials().sort((a, b) -> Boolean.compare(a.isTransparent(), b.isTransparent())));
        models.sort((a, b) -> {
            boolean aTrans = a.getMaterials().stream().anyMatch(Model.Material::isTransparent);
            boolean bTrans = b.getMaterials().stream().anyMatch(Model.Material::isTransparent);
            return Boolean.compare(aTrans, bTrans);
        });
        activity.registerModels(models);*/
        geoActivity.registerModels(models);
    }

    public void render(SWWindow window){
        if (window.getWidth() <= 0 && window.getHeight() <= 0) return;

        if (window.isResized() || swapChain.acquireNextImage()) {
            window.setResized(false);
            resize(window);
            scene.getProjection().resize(window.getWidth(), window.getHeight());
            swapChain.acquireNextImage();
        }

        //activity.recordCommandBuffer(models);
        //activity.submit(pQueue);
        geoActivity.recordCommandBuffer(models);
        geoActivity.submit(pQueue);
        ligActivity.prepareCommandBuffer();
        ligActivity.submit(pQueue);

        if (swapChain.presentImage(gQueue)) window.setResized(true);
    }

    public void close(){
        instance.close();
        physicalDevice.close();
        device.close();
        surface.close();
        swapChain.close();
        cmdPool.close();
        //activity.close();
        models.forEach(Model::close);
        textureCache.close();
        ligActivity.close();
        geoActivity.close();
    }

    public Scene getScene() {
        return scene;
    }

    private void resize(SWWindow window) {
        device.waitIdle();
        gQueue.waitIdle();

        swapChain.close();

        swapChain = new SwapChain(device, surface, window, 3,true);
        //activity.resize(swapChain);
        geoActivity.resize(swapChain);
        ligActivity.resize(swapChain,geoActivity.getAttachments());
    }
}
