package com.jigneshgaming.azyron.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void azyron$drawBranding(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int width = Math.min(236, Math.max(150, screenWidth - 24));
        int x = 12;
        int y = Math.max(8, screenHeight - 48);

        context.fill(x, y, x + width, y + 36, 0xD5090B16);
        context.fill(x, y, x + 3, y + 36, 0xFF9C78FF);
        context.fill(x + 3, y + 35, x + width, y + 36, 0x775CD9FF);
        context.fill(x + 11, y + 8, x + 33, y + 30, 0xFF17152A);
        context.fill(x + 11, y + 8, x + 13, y + 30, 0xFF5CD9FF);
        context.drawCenteredTextWithShadow(client.textRenderer, "A", x + 22, y + 14, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, "AZYRON CLIENT", x + 42, y + 7, 0xFFF4F2FF);
        context.drawTextWithShadow(client.textRenderer, "Preparing world...", x + 42, y + 20, 0xFF9AA3BB);

        int segmentStart = x + width - 44;
        int activeSegment = (int) ((System.currentTimeMillis() / 220L) % 4L);
        for (int index = 0; index < 4; index++) {
            int segmentX = segmentStart + index * 8;
            int color = index == activeSegment ? 0xFF5CD9FF : 0xFF343B51;
            context.fill(segmentX, y + 13, segmentX + 5, y + 20, color);
        }
    }
}
