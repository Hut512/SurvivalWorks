package de.survivalworks.core.client.engine.vk.vertex;

import de.survivalworks.core.client.engine.vk.rendering.Device;

import java.util.HashMap;
import java.util.Map;

public class TextureCache {
    private final Map<String,Texture> textures;

    public TextureCache(){
        textures = new HashMap<>();
    }

    public Texture createTexture(Device device, String path, int format){
        String texPath = path;
        if(path == null || path.trim().isEmpty())texPath = "";
        Texture texture = textures.get(texPath);
        if(texture == null){
            texture = new Texture(device,texPath,format);
            textures.put(path,texture);
        }
        return texture;
    }

    public void delete(){
        textures.forEach((k,v) -> v.delete());
        textures.clear();
    }
}
