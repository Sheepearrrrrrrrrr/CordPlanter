package eu.sheepearrr.cordplanter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import eu.sheepearrr.cordplanter.util.WorkspaceProperties;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public final class CordPlanter extends JavaPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("cordplanter");
    public static CordPlanter INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.getServer().getPluginManager().registerEvents(new CordPlanterListener(), this);
    }

    @Override
    public void onDisable() {
        File dataDir = this.getDataFolder();
        File dataFile = new File(dataDir.getPath() + "/data.json");
        try {
            FileWriter writer = new FileWriter(dataFile);
            CordPlanterBootstrap.INSTANCE.data.addProperty("warning", "DO NOT TAMPER WITH THESE MANUALLY UNLESS YOU WANT TO DISABLE THE FORMAT WARNING. THE PLUGIN WILL NOT CHECK IF AN OPTION EXISTS, IT WILL AUTOMATICALLY ASSUME SO. IF YOU INSTALL A NEW VERSION OF THE PLUGIN WITH DIFFERENT DEFAULT SETTINGS CONSIDER RESETTING THE SETTINGS IN-GAME WITH /workspace settings reset. USE THE IN-GAME COMMAND INSTEAD (/workspace ...).");
            for (int i = 0; i < CordPlanterBootstrap.INSTANCE.data.get("disabled").getAsJsonArray().size(); i++) {
                CordPlanterBootstrap.INSTANCE.data.get("disabled").getAsJsonArray().remove(i);
            }
            for (Map.Entry<String, WorkspaceProperties> entry : CordPlanterBootstrap.INSTANCE.disabledWorkspaces.entrySet()) {
                if (!CordPlanterBootstrap.INSTANCE.data.get("disabled").getAsJsonArray().contains(new Gson().fromJson(entry.getKey(), JsonElement.class))) CordPlanterBootstrap.INSTANCE.data.get("disabled").getAsJsonArray().add(entry.getKey());
            }
            for (Map.Entry<String, Boolean> entry : CordPlanterBootstrap.INSTANCE.settings.entrySet()) {
                CordPlanterBootstrap.INSTANCE.data.getAsJsonObject("settings").addProperty(entry.getKey(), entry.getValue());
            }
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting();
            writer.write(gsonBuilder.create().toJson(CordPlanterBootstrap.INSTANCE.data));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
