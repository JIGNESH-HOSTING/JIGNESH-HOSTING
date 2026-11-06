package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.client.HudOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void azyron$recordCps(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS || input.button() < GLFW.GLFW_MOUSE_BUTTON_1
                || input.button() > GLFW.GLFW_MOUSE_BUTTON_2) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.currentScreen == null) {
            HudOverlay.recordClick(input.button());
        }
    }
}
