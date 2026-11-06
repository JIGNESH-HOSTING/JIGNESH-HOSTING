package com.jigneshgaming.azyron.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AzyronClient implements ClientModInitializer {
    public static final String MOD_ID = "azyron_client";
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        AzyronConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.azyron_client.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        // Attach after the vanilla HUD so the client elements remain legible.
        // This relative layer inherits vanilla's HUD-hidden render condition.
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.SUBTITLES,
                Identifier.of(MOD_ID, "client_hud"),
                (context, tickCounter) -> HudOverlay.render(context)
        );

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                int buttonY = Math.max(8, scaledHeight - 36);
                Screens.getButtons(screen).add(ButtonWidget.builder(
                                Text.literal("AZYRON CLIENT"),
                                button -> client.setScreen(new AzyronMenuScreen(screen, AzyronMenuScreen.Page.HOME))
                        )
                        .dimensions(8, buttonY, 144, 20)
                        .build());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        // Apply the requested low-impact defaults only once. Later manual video
        // option changes are left alone; optimization can be re-applied from the UI.
        if (!AzyronConfig.areDefaultsApplied()) {
            PerformanceProfiles.apply(client, AzyronConfig.Profile.POTATO);
        }

        if (openMenuKey != null) {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AzyronMenuScreen(null, AzyronMenuScreen.Page.HOME));
                }
            }
        }
        HudOverlay.tick(client);
    }
}
