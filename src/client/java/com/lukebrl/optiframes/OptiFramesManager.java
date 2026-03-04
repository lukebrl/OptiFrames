package com.lukebrl.optiframes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class OptiFramesManager {
    private static boolean enabled = true;
    private static boolean renderFrame = true;
    private static boolean renderTexture = true;
    private static boolean renderDecorations = true;
    private static int atlasSize = 4096; // default
    private static int maxAtlasSize = 8192;
    
    private static final Path CONFIG_DIR = Paths.get("config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("optiframes.json");

    public static void toggleEnabled() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isFrameRendered() {
        return renderFrame;
    }

    public static void setRenderFrame(boolean value) {
        renderFrame = value;
    }

    public static boolean isTextureRendered() {
        return renderTexture;
    }

    public static void setRenderTexture(boolean value) {
        renderTexture = value;
    }

    public static boolean isDecorationsRendered() {
        return renderDecorations;
    }

    public static void setRenderDecorations(boolean value) {
        renderDecorations = value;
    }

    public static int getAtlasSize() {
        return atlasSize;
    }

    public static void setAtlasSize(int value) {
        value = Math.max(1024, Math.min(value, maxAtlasSize));
        atlasSize = value;
    }

    public static int getMaxAtlasSize() {
        return maxAtlasSize;
    }

    public static void setMaxAtlasSize(int value) {
        maxAtlasSize = value;
        if (atlasSize > maxAtlasSize) {
            atlasSize = maxAtlasSize;
        }
    }

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String content = Files.readString(CONFIG_FILE);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();

                // load values
                if (json.has("enabled")) {
                    enabled = json.get("enabled").getAsBoolean();
                }
                if (json.has("renderFrames")) {
                    renderFrame = json.get("renderFrames").getAsBoolean();
                }
                if (json.has("renderTexture")) {
                    renderTexture = json.get("renderTexture").getAsBoolean();
                }
                if (json.has("renderDecorations")) {
                    renderDecorations = json.get("renderDecorations").getAsBoolean();
                }
                if (json.has("atlasSize")) {
                    atlasSize = json.get("atlasSize").getAsInt();
                    atlasSize = Math.max(1024, Math.min(atlasSize, maxAtlasSize));
                }
            }
        } catch (IOException | IllegalStateException e) {
            System.err.println("[optiframes] Failed to load config: " + e.getMessage());
        }
    }
}
