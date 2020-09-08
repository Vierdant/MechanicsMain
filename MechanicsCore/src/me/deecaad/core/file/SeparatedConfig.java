package me.deecaad.core.file;

import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static me.deecaad.core.MechanicsCore.debug;

/**
 * This class separates "common" reference
 * types into separate maps to avoid the
 * overhead from typecasting, and to improve
 * the readability.
 *
 * Explanations on each method are provided
 * in the Configuration interface
 *
 * @author cjcrafter
 * @see Configuration
 * @since 1.0
 */
public class SeparatedConfig implements Configuration {

    private List<Map<String, ?>> maps;
    private Map<String, Number> numbers;
    private Map<String, Boolean> booleans;
    private Map<String, String> strings;
    private Map<String, Object> objects;
    private Map<String, List<String>> lists;

    public SeparatedConfig() {

        // Initialize the maps
        numbers = new LinkedHashMap<>();
        booleans = new LinkedHashMap<>();
        strings = new LinkedHashMap<>();
        objects = new LinkedHashMap<>();
        lists = new LinkedHashMap<>();

        // Add the maps to the list
        maps = new ArrayList<>(5);
        maps.add(numbers);
        maps.add(booleans);
        maps.add(strings);
        maps.add(objects);
        maps.add(lists);
    }

    @Override
    public void add(ConfigurationSection config) throws DuplicateKeyException {

        List<String> duplicates = new ArrayList<>();

        for (String key : config.getKeys(true)) {

            // Check for duplicate keys
            if (this.containsKey(key)) {
                duplicates.add(key);
            }

            Object value = config.get(key);
            set(key, value);
        }

        // Report duplicate keys
        if (!duplicates.isEmpty()) {
            throw new DuplicateKeyException(duplicates.toArray(new String[0]));
        }
    }

    @Override
    public void add(Configuration config) throws DuplicateKeyException {

        List<String> duplicates = new ArrayList<>();

        config.forEach("", (key, value) -> {

            // Check for duplicate keys
            if (this.containsKey(key)) {
                duplicates.add(key);
            } else {
                set(key, value);
            }
        }, true);

        // Report duplicate keys
        if (!duplicates.isEmpty()) {
            throw new DuplicateKeyException(duplicates.toArray(new String[0]));
        }
    }

    @Nullable
    @Override
    public Object set(String key, Object value) {
        if (value instanceof Number) {
            return numbers.put(key, (Number) value);
        } else if (value instanceof Boolean) {
            return booleans.put(key, (Boolean) value);
        } else if (value instanceof String) {
            return strings.put(key, StringUtils.color(value.toString()));
        } else if (value instanceof List<?>) {
            List<String> list = ((List<?>) value).stream()
                    .map(Object::toString)
                    .map(StringUtils::color)
                    .collect(Collectors.toList());

            return lists.put(key, list);
        } else {
            return objects.put(key, value);
        }

    }

    @Override
    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>();
        maps.forEach(map -> keys.addAll(map.keySet()));
        return keys;
    }

    @Override
    public int getInt(String key) {
        return numbers.getOrDefault(key, 0).intValue();
    }
    
    @Override
    public int getInt(String key, int def) {
        return numbers.getOrDefault(key, def).intValue();
    }
    
    @Override
    public double getDouble(String key) {
        return numbers.getOrDefault(key, 0.0).doubleValue();
    }
    
    @Override
    public double getDouble(String key, double def) {
        return numbers.getOrDefault(key, def).doubleValue();
    }
    
    @Override
    public boolean getBool(String key) {
        return booleans.getOrDefault(key, false);
    }
    
    @Override
    public boolean getBool(String key, boolean def) {
        return booleans.getOrDefault(key, def);
    }
    
    @Nonnull
    @Override
    public String getString(String key) {
        return strings.getOrDefault(key, "");
    }
    
    @Override
    public String getString(String key, String def) {
        return strings.getOrDefault(key, def);
    }

    @Nullable
    @Override
    public Object getObject(String key) {
        return objects.get(key);
    }

    @Override
    public Object getObject(String key, Object def) {
        return objects.getOrDefault(key, def);
    }

    @Nullable
    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        Object value = objects.get(key);
        return value == null ? null : clazz.cast(value);
    }
    
    @Override
    public <T> T getObject(String key, T def, Class<T> clazz) {
        Object value = objects.get(key);
        return value == null ? def : clazz.cast(value);
    }

    @Nonnull
    @Override
    public List<String> getList(String key) {
        List<String> value = lists.get(key);
        if (value == null) {
            return new ArrayList<>();
        } else {
            return value;
        }
    }

    @Override
    public List<String> getList(String key, List<String> def) {
        List<String> value = lists.get(key);
        if (value == null) {
            return def;
        } else {
            return value;
        }
    }

    @Override
    public boolean containsKey(@Nonnull String key) {
        return maps.stream().anyMatch(map -> map.containsKey(key));
    }

    @Override
    public boolean containsKey(String key, Class<?> clazz) {
        if (clazz == int.class) clazz = Integer.class;
        else if (clazz == double.class) clazz = Double.class;
        else if (clazz == boolean.class) clazz = Boolean.class;

        final Class<?> finalClazz = clazz;

        return maps.stream().anyMatch(map -> {
            Object value = map.get(key);
            if (value == null) {
                return false;
            } else {
                return finalClazz.isInstance(value);
            }
        });
    }

    @Override
    public void clear() {
        maps.forEach(Map::clear);
    }
    
    @Override
    public void forEach(@Nonnull String basePath, @Nonnull BiConsumer<String, Object> consumer, boolean deep) {
        maps.forEach(map -> forEach(map, basePath, consumer, deep));
    }
    
    private void forEach(Map<String, ?> map, String basePath, BiConsumer<String, Object> consumer, boolean deep) {
        int memorySections = StringUtils.countChars('.', basePath);
        if (basePath.isEmpty()) memorySections--;

        // Avoiding lambda for debugging
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!key.startsWith(basePath)) continue;

            int currentMemorySections = StringUtils.countChars('.', key);
            if (!deep && currentMemorySections == memorySections + 1) {
                consumer.accept(key, value);
            } else if (deep && currentMemorySections > memorySections) {
                consumer.accept(key, value);
            }
        }
    }

    public void save(@Nonnull File file) {
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
    
        // basic support for changing keys during the program AND
        // allowing the program to change keys.
        try {
            add(configuration);
        } catch (DuplicateKeyException ignore) {
        }

        // Deletes all keys from config
        configuration.getKeys(true).forEach(key -> configuration.set(key, null));
    
        // Set and save
        maps.forEach(map -> map.forEach(configuration::set));
        try {
            configuration.save(file);
        } catch (IOException ex) {
            debug.log(LogLevel.ERROR, "Could not save file \"" + file.getName() + "\"", ex);
        }
    }
}