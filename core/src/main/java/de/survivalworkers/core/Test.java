package de.survivalworkers.core;

import de.survivalworkers.core.client.engine.vk.Render;
import de.survivalworkers.core.client.engine.vk.scene.Entity;
import de.survivalworkers.core.client.engine.vk.scene.Scene;
import de.survivalworkers.core.client.engine.vk.vertex.ModelData;
import de.survivalworkers.core.client.engine.vk.vertex.ModelLoader;
import de.survivalworkers.core.client.engine.Window;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Test {
    private Entity cubeEntity;
    private float angle = 180.0f;
    private Vector3f rotatingAngle = new Vector3f(1, 1, 1);
    private Scene scene;
    public void inti(Window window, Scene scene, Render render){
        this.scene = scene;
        List<ModelData> dataList = new ArrayList<>();
        String id = "CubeModel";
        String id2 = "Model";
        dataList.add(ModelLoader.loadModel(id,"core/src/main/resources/models/orca.obj","core/src/main/resources/models/"));
        dataList.add(ModelLoader.loadModel(id2,"core/src/main/resources/models/cube.obj","core/src/main/resources/models/"));
        cubeEntity = new Entity("CubeEntity",id,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(0.0f,-0.0f,-2.0f);
        scene.addEntity(cubeEntity);
        /*cubeEntity = new Entity("CubeEntity2",id,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(0.0f,-0.0f,-20.0f);
        scene.addEntity(cubeEntity);
        cubeEntity = new Entity("CubeEntity6",id,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(0.0f,-0.0f,-80.0f);
        scene.addEntity(cubeEntity);
        cubeEntity = new Entity("CubeEntity5",id,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(0.0f,-0.0f,-60.0f);
        scene.addEntity(cubeEntity);
        cubeEntity = new Entity("CubeEntity4",id,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(0.0f,-0.0f,-40.0f);
        scene.addEntity(cubeEntity);*/
        cubeEntity = new Entity("CubeEntity3",id2,new Vector3f(0.0f,0.0f,0-2.0f));
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0.0f,0.5f,0.0f));
        cubeEntity.setPosition(-2.0f,-0.0f,-2.0f);
        scene.addEntity(cubeEntity);
        render.loadModels(dataList);
    }

    public void test(){
        /*scene.getCamera().moveForward(0.1f);
        if(true)return;
        angle += 1.0f;
        if (angle >= 360) {
            angle = angle - 360;
        }
        cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), rotatingAngle);
        cubeEntity.updateModelMatrix();*/
    }
}
