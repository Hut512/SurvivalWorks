package de.survivalworkers.core.client.engine.vk.vertex;

import de.survivalworkers.core.client.engine.vk.device.SWLogicalDevice;

import java.util.HashMap;

public class TextureCache {
    private final HashMap<String, Texture> textures;

    public TextureCache(){
        textures = new HashMap<>();
    }

    public Texture createTexture(SWLogicalDevice device, String path, int format){
        String texPath = path;
        if(path == null || path.trim().isEmpty())texPath = /*Properties.getInstance().getTexPath()*/ "";
        Texture texture = textures.get(texPath);
        if(texture == null){
            texture = new Texture(device,texPath,format);
            textures.put(path,texture);
        }
        return texture;
    }

    public void close() {
        textures.forEach((k,v) -> v.close());
        textures.clear();
    }
}