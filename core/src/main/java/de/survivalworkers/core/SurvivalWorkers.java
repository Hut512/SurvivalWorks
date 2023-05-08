package de.survivalworkers.core;

import de.survivalworkers.core.client.engine.Engine;
import de.survivalworkers.core.client.engine.io.keys.HIDInput;
import de.survivalworkers.core.client.engine.util.ClientOptions;
import de.survivalworkers.core.client.engine.vk.Render;
import de.survivalworkers.core.client.engine.vk.scene.Entity;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.vertex.ModelData;
import de.survivalworkers.core.client.engine.vk.vertex.ModelLoader;
import de.survivalworkers.core.common.event.EventManager;
import lombok.Getter;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class SurvivalWorkers {
    private Entity cubeEntity;
    private float angle = 180.0f;


    @Getter
    private final EventManager eventManager;
    @Getter
    private final ClientOptions clientOptions;
    @Getter
    private final HIDInput input;
    @Getter
    private final Engine engine;

    public SurvivalWorkers() {
        eventManager = new EventManager();
        clientOptions = new ClientOptions();
        input = new HIDInput();
        engine = new Engine(input,this);
        engine.start();
    }

    public void init(Scene scene, Render render){
        List<ModelData> dataList = new ArrayList<>();
        String id = "CubeModel";
        String id2 = "Model";
        dataList.add(ModelLoader.loadModel(id,"core/src/main/resources/models/orca.obj","core/src/main/resources/models/"));
        dataList.add(ModelLoader.loadModel(id2,"core/src/main/resources/models/cube.obj","core/src/main/resources/models/"));
        cubeEntity = new Entity("CubeEntity",id,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(0.0f,-0.0f,-2.0f);
        scene.addEntity(cubeEntity);
        cubeEntity = new Entity("CubeEntity3",id2,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(-2.0f,-0.0f,-2.0f);
        scene.addEntity(cubeEntity);
        render.loadModels(dataList);
    }

    public static void main(String[] args) {
        new SurvivalWorkers();
    }
}
