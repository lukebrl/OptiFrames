package com.lukebrl.optiframes.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import com.lukebrl.optiframes.OptiFramesManager;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;


public final class OptiFramesConfigScreen {
    private static final Path CONFIG_DIR = Paths.get("config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("optiframes.json");

    private OptiFramesConfigScreen() {}

    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("OptiFrames"))
            .setSavingRunnable(OptiFramesConfigScreen::saveConfig);

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Enabled"),
                OptiFramesManager.isEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(OptiFramesManager::setEnabled)
            .setTooltip(Text.literal("Enable/disable the map art optimization"))
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Render Frames Border"),
                OptiFramesManager.isFrameRendered())
            .setDefaultValue(true)
            .setSaveConsumer(OptiFramesManager::setRenderFrame)
            .setTooltip(Text.literal("Enable/disable rendering of item frame borders"))
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Render Border Texture"),
                OptiFramesManager.isTextureRendered())
            .setDefaultValue(true)
            .setSaveConsumer(OptiFramesManager::setRenderTexture)
            .setTooltip(Text.literal("Enable/disable texture on frame borders"))
            .build());

        return builder.build();
    }

    private static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_DIR);

            JsonObject json = new JsonObject();
            json.addProperty("enabled", OptiFramesManager.isEnabled());
            json.addProperty("renderFrames", OptiFramesManager.isFrameRendered());
            json.addProperty("renderTexture", OptiFramesManager.isTextureRendered());

            Files.writeString(CONFIG_FILE, json.toString());
        } catch (IOException e) {
            System.err.println("[optiframes] Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

