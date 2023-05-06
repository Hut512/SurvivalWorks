package de.survivalworkers.core.engine.io.keys;

import de.survivalworkers.core.engine.io.Configuration;
import lombok.SneakyThrows;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This class is used to save and update Key binds
 */
public class KeyConfig{
    private HashMap<Integer,String> keys;
    private Map<Integer,String> defaultKeys;

    /**
     * The constructor is used to load the keys from the config
     * @param defaultKeys the keys to be used if the key bind was not changed yet
     */
    @SneakyThrows
    public KeyConfig(Map<Integer,String> defaultKeys){
        this.defaultKeys = defaultKeys;
        keys = new HashMap<>();
        Properties properties = new Properties();
        if(!new File(Configuration.KEY_CONFIG_NAME).exists())new File(Configuration.KEY_CONFIG_NAME).createNewFile();
        properties.load(new FileInputStream(Configuration.KEY_CONFIG_NAME));
        for (int i : defaultKeys.keySet()) {
            keys.put(Integer.parseInt(properties.getOrDefault(defaultKeys.get(i),i).toString()),defaultKeys.get(i));
        }
    }

    /**
     * @param key the key that needs processing by other classes
     * @return the name that is currently used by the Key
     */
    public String get(int key){
        return keys.get(key);
    }


    /**
     * @param key The name that needs processing
     * @return the button key that can be used for such processing
     */
    public int get(String key){
        for (Map.Entry<Integer, String> entry : keys.entrySet()) {
            Integer keyI = entry.getKey();
            String name = entry.getValue();
            if (name.equals(key)) return keyI;
        }
        return -1;
    }

    /**
     * This method is used to set a new Key bind
     * @param name the name of the key that is to be replaced
     * @param newKey the new Key that is associated with the Key bind
     */
    @SneakyThrows
    public void setKey(String name,int newKey){
        for (Integer i : keys.keySet()) {
            if(keys.get(i).equals(name)){
                keys.remove(i);
                break;
            }
        }
        keys.put(newKey,name);
        Properties properties = new Properties();
        properties.load(new FileInputStream(Configuration.KEY_CONFIG_NAME));
        properties.setProperty(name,String.valueOf(newKey));
        properties.store(new FileOutputStream(Configuration.KEY_CONFIG_NAME),null);
    }

    /**
     * this method resets all key binds that are set by a user thus resulting in the use of default keybindings
     */
    @SneakyThrows
    public void reset(){
        Properties properties = new Properties();
        properties.load(new FileInputStream(Configuration.KEY_CONFIG_NAME));
        properties.clear();
        properties.store(new FileOutputStream(Configuration.KEY_CONFIG_NAME),null);
    }
}
