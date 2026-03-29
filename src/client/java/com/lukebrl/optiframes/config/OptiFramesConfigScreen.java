package com.lukebrl.optiframes.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
            .setTitle(Component.literal("OptiFrames"))
            .setSavingRunnable(OptiFramesConfigScreen::saveConfig);

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Use OptiFrames Renderer"),
                OptiFramesManager.isEnabled())
            .setDefaultValue(true)
            .setSaveConsumer(OptiFramesManager::setEnabled)
            .setTooltip(Component.literal("Toggle OptiFrames Renderer"))
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Use Default Item Frame Model"),
                OptiFramesManager.useDefaultModel())
            .setDefaultValue(false)
            .setSaveConsumer(OptiFramesManager::setUseDefaultModel)
            .setTooltip(Component.literal("Use vanilla item frame model instead of OptiFrames optimized one.\nDisables custom back face and border rendering."))
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Render Frames Border"),
                OptiFramesManager.isFrameRendered())
            .setDefaultValue(true)
            .setSaveConsumer(OptiFramesManager::setRenderFrame)
            .setTooltip(Component.literal("Toggle rendering of item frame borders"))
            .build());

        general.addEntry(entryBuilder.startBooleanToggle(
                Component.literal("Render Item Frames Back Face"),
                OptiFramesManager.isBackRendered())
            .setDefaultValue(false)
            .setSaveConsumer(OptiFramesManager::setRenderBackFrame)
            .setTooltip(Component.literal("Toggle rendering of item frame back face.\nOnly works with OptiFrames optimized model."))
            .build());

        int maxSize = OptiFramesManager.getMaxAtlasSize();
        java.util.List<Integer> sizeOptions = new java.util.ArrayList<>();
        for (int s = 1024; s <= maxSize; s *= 2) {
            sizeOptions.add(s);
        }
        Integer[] sizeArray = sizeOptions.toArray(new Integer[0]);

        general.addEntry(entryBuilder.startSelector(
                Component.literal("Atlas Size"),
                sizeArray,
                (Integer) OptiFramesManager.getAtlasSize())
            .setDefaultValue(4096)
            .setSaveConsumer(val -> {
                OptiFramesManager.setAtlasSize(val);
            })
            .setNameProvider(val -> {
                return Component.literal(val + "x" + val + (val == 4096 ? " [Recommended]" : ""));
            })
            .setTooltip(Component.literal("Atlas texture size.\nBigger = more maps per draw call.\nRequires world rejoin to apply.\nmax: " + maxSize + "x" + maxSize))
            .build());

        return builder.build();
    }

    private static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_DIR);

            JsonObject json = new JsonObject();
            json.addProperty("enabled", OptiFramesManager.isEnabled());
            json.addProperty("useDefaultModel", OptiFramesManager.useDefaultModel());
            json.addProperty("renderFrames", OptiFramesManager.isFrameRendered());
            json.addProperty("renderBackFrame", OptiFramesManager.isBackRendered());
            json.addProperty("atlasSize", OptiFramesManager.getAtlasSize());

            Files.writeString(CONFIG_FILE, json.toString());
        } catch (IOException e) {
            System.err.println("[optiframes] Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

