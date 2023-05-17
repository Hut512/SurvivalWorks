package de.survivalworkers.core.client.engine.vk.vertex;

import de.survivalworkers.core.client.engine.vk.rendering.*;

import java.util.*;

public class TextureCache {
    private final Map<String,Texture> textures;

    public TextureCache(){
        textures = new HashMap<>();
    }

    public Texture createTexture(Device device, String path, int format){
        String texPath = path;
        if(path == null || path.trim().isEmpty())texPath = "core/src/main/resources/models/default.png";
        Texture texture = textures.get(texPath);
        if(texture == null){
            texture = new Texture(device,texPath,format);
            textures.put(texPath,texture);
        }
        return texture;
    }

    public void close(){
        textures.forEach((k,v) -> v.close());
        textures.clear();
    }
}