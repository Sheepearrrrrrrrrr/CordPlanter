package eu.sheepearrr.cordplanter.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.sheepearrr.cordplanter.CordPlanter;
import eu.sheepearrr.cordplanter.CordPlanterBootstrap;
import eu.sheepearrr.cordplanter.util.GsonUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Config {
    public final String namespace;
    public final List<JsonObject> conDefs;
    public static final File configFolder = new File(CordPlanterBootstrap.dataDirectory, "workspaces/configs");
    public Map<String, JsonObject> configs;
    public Map<String, String> shortcuts;

    public Config(String namespace, List<JsonObject> conDefs) {
        this.namespace = namespace;
        this.conDefs = conDefs;
    }

    public static void createConfigFolder() {
        if (!configFolder.isDirectory()) {
            configFolder.mkdir();
        }
    }

    public Object getValueFromShortcut(String shortcut, String configName) {
        return getValue(this.shortcuts.get(shortcut), configName);
    }

    public Object getValue(String key, String configName) {
        JsonObject config = configs.get(configName);
        List<String> paths;
        StringBuilder prev = new StringBuilder();
        if (key.contains("%")) {
            paths = new ArrayList<>();
            for (char c : key.toCharArray()) {
                if (c == '%') {
                    paths.add(prev.toString());
                    prev = new StringBuilder();
                    continue;
                }
                prev.append(c);
            }
        } else {
            paths = List.of(key);
        }
        JsonElement prevElement = config;
        int i = 0;
        for (String path : paths) {
            if (i >= paths.size() - 1) {
                if (prevElement.isJsonObject()) {
                    return prevElement.getAsJsonObject().get(path);
                } else {
                    return prevElement.getAsJsonArray().get(Integer.parseInt(path));
                }
            }
            if (prevElement.isJsonObject()) {
                prevElement = prevElement.getAsJsonObject().get(path);
            } else {
                prevElement = prevElement.getAsJsonArray().get(Integer.parseInt(path));
            }
        }
        return GsonUtils.getValue(prevElement);
    }

    public void createConfigs() {
        for (JsonObject config : this.conDefs) {
            try {
                File namespaceFolder = new File(configFolder, namespace);
                if (!namespaceFolder.isDirectory()) {
                    namespaceFolder.mkdir();
                }
                File configFile = new File(namespaceFolder, config.get("name").getAsString() + ".json");
                JsonObject values;
                if (!configFile.exists()) {
                    configFile.createNewFile();
                    values = new JsonObject();
                    for (JsonElement e : config.get("values").getAsJsonArray()) {
                        JsonObject obj = e.getAsJsonObject();
                        values.add(obj.get("name").getAsString(), obj.get("value"));
                    }
                } else {
                    FileReader r = new FileReader(configFile);
                    values = new Gson().fromJson(r, JsonObject.class);
                    r.close();
                }
                if (config.has("shortcuts")) {
                    for (Map.Entry<String, JsonElement> shortcut : config.get("shortcuts").getAsJsonObject().entrySet()) {
                        if (shortcut.getValue() instanceof JsonPrimitive jp && jp.isString()) {
                            this.shortcuts.put(shortcut.getKey(), jp.getAsString());
                        }
                    }
                }
                configs.put(config.get("name").getAsString(), values);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
