package com.killaura;

import com.killaura.command.KillAuraCommand;
import com.killaura.config.KillAuraConfig;
import com.killaura.module.KillAura;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KillAuraMod implements ClientModInitializer {
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        // Load config
        KillAuraConfig.load();

        // Register keybinding
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.killaura.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.killaura"
        ));

        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && toggleKey.wasPressed()) {
                KillAura.getInstance().toggle();
            }
            KillAura.getInstance().tick(client);
        });

        // Register client command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            KillAuraCommand.register(dispatcher);
        });
    }
}
