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
            }
        } catch (IOException | IllegalStateException e) {
            System.err.println("[optiframes] Failed to load config: " + e.getMessage());
        }
    }
}
