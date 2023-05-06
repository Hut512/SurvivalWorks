package de.survivalworkers.core.common.option;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

@Slf4j
public abstract class OptionGroup implements Closeable {
    private static final String FILE_EXTENSION = ".properties";
    private static final Map<Class<?>, Function<String, ?>> typeMappers = Map.of(
            String.class, s -> s,
            Integer.class, Integer::parseInt,
            Long.class, Long::parseLong,
            Boolean.class, Boolean::parseBoolean
    );

    private final String id;

    public OptionGroup(String id) {
        this.id = id;
        load(getFileName());
    }

    public String getFileName() {
        return id.replace("[^a-zA-Z0-9._-]", "_") + FILE_EXTENSION;
    }

    public void load(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists())
                load(new BufferedInputStream(new FileInputStream(file)));
        } catch (IOException e) {
            log.warn("Failed to load options file " + fileName, e);
        }
    }

    public void load(InputStream inputStream) {
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            Arrays.stream(this.getClass().getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Option.class))
                    .filter(AccessibleObject::trySetAccessible)
                    .forEach(f -> {
                        try {
                            if (!typeMappers.containsKey(f.getType())) {
                                throw new RuntimeException("No type mapper for found for type: " + f.getType().getName());
                            }
                            String strDefaultValue = f.getAnnotation(Option.class).defaultValue();
                            String strValue = properties.getProperty(f.getName(), strDefaultValue);
                            f.set(this, typeMappers.get(f.getType()).apply(strValue));
                        } catch (IllegalAccessException | RuntimeException e) {
                            log.warn("Failed to set Field of option group: " + id, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to load options file: " + id, e);
        }
    }

    public void store(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists())
                file.createNewFile();
            store(new BufferedOutputStream(new FileOutputStream(file)));
        } catch (IOException e) {
            log.warn("Failed to load options file " + fileName, e);
        }
    }

    public void store(OutputStream outputStream) {
        try {
            Properties properties = new Properties();
            Arrays.stream(this.getClass().getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Option.class))
                    .filter(AccessibleObject::trySetAccessible)
                    .forEach(f -> {
                        try {
                            String strDefaultValue = f.getAnnotation(Option.class).defaultValue();
                            Object value = f.get(this);
                            if (value != null && !String.valueOf(value).equals(strDefaultValue)) {
                                properties.setProperty(f.getName(), String.valueOf(value));
                            }
                        } catch (IllegalAccessException e) {
                            log.warn("Failed to get Field of option group: " + id, e);
                        }
                    });
            properties.store(outputStream, null);
        } catch (IOException e) {
            log.warn("Failed to store options file: " + id, e);
        }
    }

    @Override
    public void close() throws IOException {
        store(getFileName());
    }
}
