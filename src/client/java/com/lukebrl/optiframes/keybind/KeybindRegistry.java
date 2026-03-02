package com.lukebrl.optiframes.keybind;

import com.lukebrl.optiframes.OptiFramesManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

public class KeybindRegistry {
    private static KeyBinding toggleKey = null;

    public static void register() {
        try {
            toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "Toggle Optiframes",
                    net.minecraft.client.util.InputUtil.Type.KEYSYM,
                    org.lwjgl.glfw.GLFW.GLFW_KEY_Q,
                    KeyBinding.Category.MISC
            ));
        } catch (Throwable t) {
            toggleKey = null;
            System.out.println("[optiframes] KeyBinding registration failed: " + t.getMessage());
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null) return;
            if (client.currentScreen != null) return;
            if (toggleKey == null) return;

            while (toggleKey.wasPressed()) {
                OptiFramesManager.toggleEnabled();
                if (client.player != null) client.player.sendMessage(Text.literal("OptiFrames: " + (OptiFramesManager.isEnabled() ? "enabled" : "disabled")), true);
            }
        });
    }
}
