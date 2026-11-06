package com.jigneshgaming.azyron.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/** Lightweight, in-process HUD overlays. No render thread, polling worker, or retained world references. */
public final class HudOverlay {
    private static final int PURPLE = 0xFF9C78FF;
    private static final int BLUE = 0xFF5CD9FF;
    private static final int MUTED = 0xFF9AA3BB;
    private static final int PANEL = 0xD90A0C17;
    private static final long CPS_WINDOW_MS = 1_000L;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>(16);
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>(16);

    private HudOverlay() {
    }

    public static void recordClick(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) {
            synchronized (LEFT_CLICKS) {
                LEFT_CLICKS.addLast(now);
                trim(LEFT_CLICKS, now);
            }
        } else if (button == 1) {
            synchronized (RIGHT_CLICKS) {
                RIGHT_CLICKS.addLast(now);
                trim(RIGHT_CLICKS, now);
            }
        }
    }

    public static void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        synchronized (LEFT_CLICKS) {
            trim(LEFT_CLICKS, now);
        }
        synchronized (RIGHT_CLICKS) {
            trim(RIGHT_CLICKS, now);
        }
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        TextRenderer font = client.textRenderer;

        if (AzyronConfig.isHudEnabled(AzyronConfig.HudModule.FPS)) {
            drawMetric(context, font, screenWidth, screenHeight, AzyronConfig.HudModule.FPS,
                    "FPS", Integer.toString(client.getCurrentFps()), PURPLE);
        }
        if (AzyronConfig.isHudEnabled(AzyronConfig.HudModule.CPS)) {
            drawMetric(context, font, screenWidth, screenHeight, AzyronConfig.HudModule.CPS,
                    "CPS", "L " + clickCount(LEFT_CLICKS) + "   R " + clickCount(RIGHT_CLICKS), BLUE);
        }
        if (AzyronConfig.isHudEnabled(AzyronConfig.HudModule.PING)) {
            drawMetric(context, font, screenWidth, screenHeight, AzyronConfig.HudModule.PING,
                    "PING", getLatency(client), BLUE);
        }
        if (AzyronConfig.isHudEnabled(AzyronConfig.HudModule.COORDINATES)) {
            BlockPos pos = client.player.getBlockPos();
            String coordinates = pos.getX() + "  " + pos.getY() + "  " + pos.getZ();
            drawMetric(context, font, screenWidth, screenHeight, AzyronConfig.HudModule.COORDINATES,
                    "XYZ", coordinates, PURPLE);
        }
        if (AzyronConfig.isHudEnabled(AzyronConfig.HudModule.KEYSTROKES)) {
            drawKeystrokes(context, font, client, screenWidth, screenHeight);
        }
        if (AzyronConfig.isHudEnabled(AzyronConfig.HudModule.ARMOR)) {
            drawArmor(context, font, client, screenWidth, screenHeight);
        }
    }

    private static void drawMetric(DrawContext context, TextRenderer font, int screenWidth, int screenHeight,
                                   AzyronConfig.HudModule module, String label, String value, int accent) {
        int panelWidth = Math.max(94, font.getWidth(label) + font.getWidth(value) + 27);
        panelWidth = Math.min(panelWidth, Math.max(52, screenWidth));
        int panelHeight = 22;
        int x = resolveX(module, screenWidth, panelWidth);
        int y = resolveY(module, screenHeight, panelHeight);

        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        context.fill(x, y, x + 2, y + panelHeight, accent);
        context.fill(x + 2, y + panelHeight - 1, x + panelWidth, y + panelHeight, 0x664B5B7A);
        context.drawTextWithShadow(font, label, x + 8, y + 6, MUTED);
        int valueX = Math.max(x + 8 + font.getWidth(label) + 7,
                x + panelWidth - font.getWidth(value) - 8);
        context.drawTextWithShadow(font, value, valueX, y + 6, 0xFFF4F5FF);
    }

    private static void drawKeystrokes(DrawContext context, TextRenderer font, MinecraftClient client,
                                       int screenWidth, int screenHeight) {
        AzyronConfig.HudModule module = AzyronConfig.HudModule.KEYSTROKES;
        int panelWidth = 76;
        int panelHeight = 67;
        int x = resolveX(module, screenWidth, panelWidth);
        int y = resolveY(module, screenHeight, panelHeight);
        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        context.fill(x, y, x + 2, y + panelHeight, BLUE);
        context.drawTextWithShadow(font, "KEYS", x + 8, y + 4, MUTED);

        var options = client.options;
        drawKey(context, font, options.forwardKey.getBoundKeyLocalizedText().getString(),
                options.forwardKey.isPressed(), x + 27, y + 16, 22, 17);
        drawKey(context, options.leftKey.getBoundKeyLocalizedText().getString(),
                options.leftKey.isPressed(), x + 5, y + 36, 22, 17);
        drawKey(context, options.backKey.getBoundKeyLocalizedText().getString(),
                options.backKey.isPressed(), x + 27, y + 36, 22, 17);
        drawKey(context, options.rightKey.getBoundKeyLocalizedText().getString(),
                options.rightKey.isPressed(), x + 49, y + 36, 22, 17);
    }

    private static void drawKey(DrawContext context, TextRenderer font, String key, boolean pressed,
                                int x, int y, int width, int height) {
        int fill = pressed ? 0xFF3C2C6A : 0xFF151927;
        int border = pressed ? PURPLE : 0xFF353B50;
        context.fill(x, y, x + width, y + height, border);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        String clippedKey = key.length() > 3 ? key.substring(0, 3) : key;
        context.drawTextWithShadow(font, clippedKey,
                x + (width - font.getWidth(clippedKey)) / 2,
                y + (height - 9) / 2,
                pressed ? 0xFFFFFFFF : 0xFFCFD5E8);
    }

    private static void drawArmor(DrawContext context, TextRenderer font, MinecraftClient client,
                                 int screenWidth, int screenHeight) {
        AzyronConfig.HudModule module = AzyronConfig.HudModule.ARMOR;
        int panelWidth = 112;
        int panelHeight = 40;
        int x = resolveX(module, screenWidth, panelWidth);
        int y = resolveY(module, screenHeight, panelHeight);
        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        context.fill(x, y, x + 2, y + panelHeight, PURPLE);
        context.drawTextWithShadow(font, "ARMOR", x + 8, y + 3, MUTED);

        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };
        for (int index = 0; index < slots.length; index++) {
            int itemX = x + 7 + index * 25;
            int itemY = y + 17;
            ItemStack stack = client.player.getEquippedStack(slots[index]);
            context.fill(itemX, itemY, itemX + 20, itemY + 19, 0xAA171B2B);
            if (stack.isEmpty()) {
                context.drawTextWithShadow(font, "-", itemX + 8, itemY + 5, 0xFF6F7890);
                continue;
            }

            context.drawItem(stack, itemX + 2, itemY + 1);
            int maxDamage = stack.getMaxDamage();
            if (maxDamage > 0) {
                double remaining = (double) (maxDamage - stack.getDamage()) / maxDamage;
                int barWidth = (int) Math.round(16.0 * Math.max(0.0, Math.min(1.0, remaining)));
                int barColor = remaining > 0.5 ? 0xFF58D68D : remaining > 0.2 ? 0xFFFFC857 : 0xFFFF6B7A;
                context.fill(itemX + 2, itemY + 17, itemX + 18, itemY + 19, 0xFF30364A);
                if (barWidth > 0) {
                    context.fill(itemX + 2, itemY + 17, itemX + 2 + barWidth, itemY + 19, barColor);
                }
            }
        }
    }

    private static String getLatency(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null || client.player == null) {
            return "--";
        }
        PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
        return entry == null ? "--" : entry.getLatency() + " ms";
    }

    private static int clickCount(Deque<Long> clicks) {
        synchronized (clicks) {
            return clicks.size();
        }
    }

    private static void trim(Deque<Long> clicks, long now) {
        long oldestAllowed = now - CPS_WINDOW_MS;
        Iterator<Long> iterator = clicks.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() < oldestAllowed) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    private static int resolveX(AzyronConfig.HudModule module, int screenWidth, int width) {
        int maxX = Math.max(0, screenWidth - width - 2);
        return 1 + Math.round(AzyronConfig.getHudPosition(module).x() * maxX);
    }

    private static int resolveY(AzyronConfig.HudModule module, int screenHeight, int height) {
        int maxY = Math.max(0, screenHeight - height - 2);
        return 1 + Math.round(AzyronConfig.getHudPosition(module).y() * maxY);
    }
}
