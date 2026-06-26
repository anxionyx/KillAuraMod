package com.killaura.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class KillAuraConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("killaura.json").toFile();

    @Expose public boolean enabled = false;
    @Expose public double range = 3.2;
    @Expose public double cps = 12.0;
    @Expose public double aimSpeed = 4.5;
    @Expose public boolean autoBlock = true;
    @Expose public boolean autoCrit = true;
    @Expose public boolean autoSprint = true;
    @Expose public boolean autoTotem = true;

    private static KillAuraConfig INSTANCE = new KillAuraConfig();

    public static KillAuraConfig getInstance() {
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, KillAuraConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (INSTANCE == null) INSTANCE = new KillAuraConfig();
        save();
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
