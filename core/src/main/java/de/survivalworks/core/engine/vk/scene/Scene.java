package de.survivalworks.core.engine.vk.scene;

import de.survivalworks.core.engine.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {
    private Camera camera;
    private Map<String , List<Entity>> entitiesMap;
    private Projection projection;

    public Scene(Window window){
        entitiesMap = new HashMap<>();
        projection = new Projection();
        projection.resize(window.getWidth(), window.getHeight());
        camera = new Camera();
    }

    public void addEntity(Entity entity) {
        List<Entity> entities = entitiesMap.get(entity.getModelId());
        if (entities == null) {
            entities = new ArrayList<>();
            entitiesMap.put(entity.getModelId(), entities);
        }
        entities.add(entity);
    }

    public List<Entity> get(String  id){
        return entitiesMap.get(id);
    }

    public void removeEntities(){
        entitiesMap.clear();
    }

    public Projection getProjection() {
        return projection;
    }

    public Camera getCamera() {
        return camera;
    }
}
