package de.survivalworkers.core.client.engine.io;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.InputStream;

public class Configuration {
    public static final String KEY_CONFIG_NAME = "/keys.dat";
    private static Configuration instance;
    private static final String FILENAME = "game.properties";
    @Getter
    private int tps;
    @Getter
    private int requImgs;
    @Getter
    private float fov;
    @Getter
    private float zFar;
    @Getter
    private float zNear;
    @Getter
    private boolean debug;
    private String texPath;
    @Getter
    private int maxMaterials;

    public static synchronized Configuration getInstance(){
        if(instance == null)
            instance = new Configuration();
        
        return instance;
    }

    @SneakyThrows
    private Configuration() {
        java.util.Properties properties = new java.util.Properties();
        InputStream stream = Configuration.class.getResourceAsStream("/" + FILENAME);

            properties.load(stream);
            tps = Integer.parseInt(properties.getOrDefault("tps",20).toString());
            requImgs = Integer.parseInt(properties.getOrDefault("imgs",3).toString());
            fov = (float) Math.toRadians(Float.parseFloat(properties.getOrDefault("fov", 60.0f).toString()));
            zNear = Float.parseFloat(properties.getOrDefault("zNear", 1.0f).toString());
            zFar = Float.parseFloat(properties.getOrDefault("zFar", 100.0f).toString());
            maxMaterials = Integer.parseInt(properties.getOrDefault("max_materials",500).toString());
            debug = Boolean.parseBoolean(properties.getOrDefault("debug", false).toString());
            texPath = properties.get("TexturePath").toString();
    }
}
