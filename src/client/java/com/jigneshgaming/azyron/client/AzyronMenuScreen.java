package com.jigneshgaming.azyron.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Branded settings hub for profiles, HUD, and the installed performance stack. */
public final class AzyronMenuScreen extends Screen {
    private static final int COLOR_PURPLE = 0xFF9C78FF;
    private static final int COLOR_BLUE = 0xFF5CD9FF;
    private static final int COLOR_TEXT = 0xFFF0F2FA;
    private static final int COLOR_MUTED = 0xFF9AA3BB;
    private static final int COLOR_PANEL = 0xFF111522;
    private static final int COLOR_CARD = 0xFF171C2B;

    private static final ModEntry[] PERFORMANCE_MODS = {
            new ModEntry("Sodium", "sodium", "0.8.14", "Rendering engine"),
            new ModEntry("Lithium", "lithium", "0.21.4", "Game logic"),
            new ModEntry("FerriteCore", "ferritecore", "8.2.0", "Memory use"),
            new ModEntry("ImmediatelyFast", "immediatelyfast", "1.14.3", "GUI and immediate rendering"),
            new ModEntry("Entity Culling", "entityculling", "1.11.3", "Hidden entities"),
            new ModEntry("More Culling", "moreculling", "1.6.2", "Additional visibility culling"),
            new ModEntry("MixinSquared", "mixinsquared", "0.3.7", "More Culling mixin support"),
            new ModEntry("ModernFix-mVUS", "modernfix", "5.26.3-build.2", "Memory and loading fixes"),
            new ModEntry("Dynamic FPS", "dynamic_fps", "3.11.6", "Background resource use"),
            new ModEntry("Fabric API", "fabric-api", "0.141.6", "Required Fabric API"),
            new ModEntry("Cloth Config", "cloth-config", "21.11.153", "More Culling dependency")
    };

    private final Screen parent;
    private final Page page;
    private final boolean[] modLoaded = new boolean[PERFORMANCE_MODS.length];
    private AzyronConfig.HudModule selectedHud;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentWidth;
    private int contentY;

    public AzyronMenuScreen(Screen parent, Page page) {
        this(parent, page, AzyronConfig.HudModule.FPS);
    }

    public AzyronMenuScreen(Screen parent, Page page, AzyronConfig.HudModule selectedHud) {
        super(Text.literal("AZYRON CLIENT"));
        this.parent = parent;
        this.page = page == null ? Page.HOME : page;
        this.selectedHud = selectedHud == null ? AzyronConfig.HudModule.FPS : selectedHud;
        for (int index = 0; index < PERFORMANCE_MODS.length; index++) {
            modLoaded[index] = FabricLoader.getInstance().isModLoaded(PERFORMANCE_MODS[index].modId());
        }
    }

    @Override
    protected void init() {
        calculateLayout();
        addNavigationButtons();
        addButton("×", panelX + panelWidth - 48, panelY + 18, 32, 22, this::close);

        switch (page) {
            case HOME -> addHomeButtons();
            case PERFORMANCE -> addPerformanceButtons();
            case HUD -> addHudButtons();
            case MODS, ABOUT -> {
                // Informational pages do not need additional child widgets.
            }
        }
    }

    private void calculateLayout() {
        panelWidth = Math.max(300, Math.min(960, width - 20));
        panelHeight = Math.max(250, Math.min(580, height - 20));
        panelX = Math.max(10, (width - panelWidth) / 2);
        panelY = Math.max(10, (height - panelHeight) / 2);
        contentX = panelX + Math.min(176, Math.max(132, panelWidth / 5));
        contentWidth = Math.max(120, panelWidth - (contentX - panelX) - 22);
        contentY = panelY + 101;
    }

    private void addNavigationButtons() {
        Page[] pages = Page.values();
        int navX = panelX + 14;
        int navY = panelY + 105;
        int navWidth = Math.max(108, Math.min(148, contentX - panelX - 28));
        for (int index = 0; index < pages.length; index++) {
            Page target = pages[index];
            String label = (target == page ? "●  " : "   ") + target.navLabel;
            addButton(label, navX, navY + index * 36, navWidth, 28, () -> navigate(target));
        }
    }

    private void addHomeButtons() {
        int buttonY = contentY + 223;
        int buttonWidth = Math.max(118, Math.min(230, (contentWidth - 12) / 2));
        addButton("ONE-CLICK OPTIMIZE", contentX + 10, buttonY, buttonWidth, 28,
                () -> applyProfile(AzyronConfig.Profile.POTATO));
        addButton("FPS BOOST PROFILES", contentX + 20 + buttonWidth, buttonY,
                buttonWidth, 28, () -> navigate(Page.PERFORMANCE));
    }

    private void addPerformanceButtons() {
        AzyronConfig.Profile[] profiles = AzyronConfig.Profile.values();
        int gap = 10;
        int cardWidth = Math.max(88, (contentWidth - 28 - gap * 2) / 3);
        int cardX = contentX + 10;
        int buttonY = contentY + 177;
        for (int index = 0; index < profiles.length; index++) {
            AzyronConfig.Profile selected = profiles[index];
            int x = cardX + index * (cardWidth + gap);
            addButton("APPLY", x, buttonY, cardWidth, 24, () -> applyProfile(selected));
        }
        int lowerY = contentY + 220;
        addButton("ONE-CLICK OPTIMIZE", contentX + 10, lowerY,
                Math.max(130, Math.min(220, contentWidth / 2 - 6)), 26,
                () -> applyProfile(AzyronConfig.Profile.POTATO));
    }

    private void addHudButtons() {
        AzyronConfig.HudModule[] modules = AzyronConfig.HudModule.values();
        int gap = 10;
        int cardWidth = Math.max(110, (contentWidth - 30) / 2);
        int startX = contentX + 10;
        int startY = contentY + 48;
        for (int index = 0; index < modules.length; index++) {
            AzyronConfig.HudModule module = modules[index];
            int column = index % 2;
            int row = index / 2;
            int x = startX + column * (cardWidth + gap);
            int y = startY + row * 36;
            String state = AzyronConfig.isHudEnabled(module) ? "ON" : "OFF";
            addButton(module.displayName() + " HUD  ·  " + state,
                    x, y, cardWidth, 27, () -> {
                        AzyronConfig.toggleHud(module);
                        AzyronConfig.save();
                        refresh();
                    });
        }

        int controlsY = contentY + 177;
        addButton("EDIT: " + selectedHud.displayName(), contentX + 10, controlsY,
                Math.max(140, Math.min(210, contentWidth - 20)), 26, this::selectNextHud);

        int nudgeY = controlsY + 38;
        int nudgeWidth = Math.max(44, Math.min(78, (contentWidth - 64) / 5));
        int x = contentX + 10;
        addButton("X −", x, nudgeY, nudgeWidth, 23, () -> nudgeSelectedHud(-0.035f, 0.0f));
        x += nudgeWidth + 5;
        addButton("X +", x, nudgeY, nudgeWidth, 23, () -> nudgeSelectedHud(0.035f, 0.0f));
        x += nudgeWidth + 5;
        addButton("Y −", x, nudgeY, nudgeWidth, 23, () -> nudgeSelectedHud(0.0f, -0.035f));
        x += nudgeWidth + 5;
        addButton("Y +", x, nudgeY, nudgeWidth, 23, () -> nudgeSelectedHud(0.0f, 0.035f));
        x += nudgeWidth + 5;
        addButton("RESET", x, nudgeY, nudgeWidth, 23, () -> {
            AzyronConfig.resetHudPosition(selectedHud);
            AzyronConfig.save();
            refresh();
        });
    }

    private void addButton(String label, int x, int y, int buttonWidth, int buttonHeight, Runnable action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> action.run())
                .dimensions(x, y, buttonWidth, buttonHeight)
                .build());
    }

    private void navigate(Page target) {
        if (client != null) {
            client.setScreen(new AzyronMenuScreen(parent, target, selectedHud));
        }
    }

    private void refresh() {
        if (client != null) {
            client.setScreen(new AzyronMenuScreen(parent, page, selectedHud));
        }
    }

    private void applyProfile(AzyronConfig.Profile profile) {
        PerformanceProfiles.apply(client, profile);
        refresh();
    }

    private void selectNextHud() {
        AzyronConfig.HudModule[] modules = AzyronConfig.HudModule.values();
        selectedHud = modules[(selectedHud.ordinal() + 1) % modules.length];
        refresh();
    }

    private void nudgeSelectedHud(float deltaX, float deltaY) {
        AzyronConfig.nudgeHudPosition(selectedHud, deltaX, deltaY);
        AzyronConfig.save();
        refresh();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xF5080A12);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 2, COLOR_PURPLE);
        context.fill(panelX, panelY + 72, panelX + panelWidth, panelY + 73, 0xFF282E42);
        context.fill(panelX + 164, panelY + 73, panelX + 165, panelY + panelHeight - 18, 0xFF252B3D);

        drawBrand(context);
        drawSidebar(context);
        drawPage(context);
        drawFooter(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawBrand(DrawContext context) {
        int logoX = panelX + 18;
        int logoY = panelY + 19;
        context.fill(logoX, logoY, logoX + 34, logoY + 34, 0xFF18172B);
        context.fill(logoX, logoY, logoX + 3, logoY + 34, COLOR_BLUE);
        context.drawCenteredTextWithShadow(textRenderer, "A", logoX + 18, logoY + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "AZYRON CLIENT", panelX + 62, panelY + 20, COLOR_TEXT);
        context.drawTextWithShadow(textRenderer, "PERFORMANCE  /  MINECRAFT 1.21.11", panelX + 62, panelY + 39, COLOR_MUTED);
        context.drawTextWithShadow(textRenderer, "1.0", panelX + panelWidth - 87, panelY + 25, COLOR_BLUE);
    }

    private void drawSidebar(DrawContext context) {
        context.drawTextWithShadow(textRenderer, "CLIENT", panelX + 18, panelY + 84, COLOR_MUTED);
        context.drawTextWithShadow(textRenderer, "JIGNESH GAMING", panelX + 18, panelY + panelHeight - 38, 0xFF777F95);
    }

    private void drawPage(DrawContext context) {
        drawText(context, page.title, contentX + 10, panelY + 87, COLOR_TEXT);
        drawText(context, page.subtitle, contentX + 10, panelY + 105, COLOR_MUTED);
        switch (page) {
            case HOME -> drawHome(context);
            case PERFORMANCE -> drawPerformance(context);
            case HUD -> drawHud(context);
            case MODS -> drawMods(context);
            case ABOUT -> drawAbout(context);
        }
    }

    private void drawHome(DrawContext context) {
        int x = contentX + 10;
        int y = contentY + 30;
        int w = contentWidth - 20;
        int h = 174;
        drawCard(context, x, y, w, h, COLOR_BLUE);
        drawText(context, "LESS CLUTTER. MORE CONTROL.", x + 16, y + 17, COLOR_BLUE);
        drawText(context, "Built for smooth, stable play.", x + 16, y + 43, COLOR_TEXT);
        drawText(context, "A low-graphics first-run profile, a customizable six-module HUD,", x + 16, y + 69, COLOR_MUTED);
        drawText(context, "and a pinned Fabric performance stack — all for 1.21.11 only.", x + 16, y + 83, COLOR_MUTED);

        int chipY = y + 112;
        int chipWidth = Math.min(192, Math.max(110, (w - 48) / 3));
        drawChip(context, x + 16, chipY, chipWidth, "ACTIVE PROFILE", AzyronConfig.getProfile().displayName());
        drawChip(context, x + 24 + chipWidth, chipY, chipWidth, "RENDER DISTANCE", AzyronConfig.getProfile().renderDistance() + " chunks");
        drawChip(context, x + 32 + chipWidth * 2, chipY, chipWidth, "FPS CAP", AzyronConfig.getProfile().maxFps() + " FPS");

        drawText(context, "Press Right Shift in-game to open this client.", x + 4, y + h + 18, COLOR_MUTED);
        drawText(context, "Performance varies by device, world, drivers, and installed mods.", x + 4, y + h + 35, 0xFF737C92);
    }

    private void drawPerformance(DrawContext context) {
        AzyronConfig.Profile[] profiles = AzyronConfig.Profile.values();
        int gap = 10;
        int cardWidth = Math.max(88, (contentWidth - 28 - gap * 2) / 3);
        int cardX = contentX + 10;
        int cardY = contentY + 45;
        for (int index = 0; index < profiles.length; index++) {
            AzyronConfig.Profile profile = profiles[index];
            int x = cardX + index * (cardWidth + gap);
            drawCard(context, x, cardY, cardWidth, 117,
                    profile == AzyronConfig.getProfile() ? COLOR_PURPLE : 0xFF3C4660);
            drawText(context, profile.displayName().toUpperCase(java.util.Locale.ROOT), x + 10, cardY + 13, COLOR_TEXT);
            drawText(context, profile.summary(), x + 10, cardY + 37, COLOR_MUTED);
            drawText(context, "Cap  " + profile.maxFps() + " FPS", x + 10, cardY + 63, COLOR_BLUE);
            drawText(context, profile == AzyronConfig.getProfile() ? "CURRENT PROFILE" : "READY TO APPLY",
                    x + 10, cardY + 88, profile == AzyronConfig.getProfile() ? COLOR_PURPLE : COLOR_MUTED);
        }

        drawCard(context, contentX + 10, contentY + 221, contentWidth - 20, 45, COLOR_PURPLE);
        drawText(context, "ONE-CLICK OPTIMIZE applies the Potato profile and saves vanilla video options.",
                contentX + 22, contentY + 236, COLOR_TEXT);
        drawText(context, "You can restore or change these options at any time in Minecraft Video Settings.",
                contentX + 10, contentY + 281, COLOR_MUTED);
    }

    private void drawHud(DrawContext context) {
        drawText(context, "Positions are screen-relative and stored separately for each module.",
                contentX + 10, contentY + 163, COLOR_MUTED);
        AzyronConfig.HudPosition position = AzyronConfig.getHudPosition(selectedHud);
        String xy = String.format(java.util.Locale.ROOT, "X %.2f    Y %.2f", position.x(), position.y());
        drawText(context, "SELECTED: " + selectedHud.displayName(), contentX + 10, contentY + 211, COLOR_BLUE);
        drawText(context, xy + "    (use X/Y buttons to nudge the anchor)", contentX + 10, contentY + 243, COLOR_MUTED);
    }

    private void drawMods(DrawContext context) {
        drawText(context, "Pinned Fabric builds for Minecraft 1.21.11 only", contentX + 10, contentY + 24, COLOR_BLUE);
        int rowY = contentY + 48;
        int rowHeight = 26;
        for (int index = 0; index < PERFORMANCE_MODS.length; index++) {
            ModEntry mod = PERFORMANCE_MODS[index];
            int y = rowY + index * rowHeight;
            context.fill(contentX + 10, y, contentX + contentWidth - 10, y + 23,
                    index % 2 == 0 ? 0xFF171C2A : 0xFF131826);
            drawText(context, mod.name(), contentX + 17, y + 7, COLOR_TEXT);
            drawText(context, mod.version(), contentX + 160, y + 7, COLOR_MUTED);
            drawText(context, mod.role(), contentX + 235, y + 7, 0xFF8791A9);
            boolean loaded = modLoaded[index];
            String status = loaded ? "ACTIVE" : "MISSING";
            int statusColor = loaded ? 0xFF69D9A2 : 0xFFFF7D8B;
            int statusX = contentX + contentWidth - 72;
            drawText(context, status, statusX, y + 7, statusColor);
        }
        drawText(context, "More Culling needs Cloth Config and MixinSquared; Entity Culling and Dynamic FPS need Fabric API.",
                contentX + 10, rowY + PERFORMANCE_MODS.length * rowHeight + 5, COLOR_MUTED);
        drawText(context, "ModernFix-mVUS is the 1.21.11-compatible continuation build in this pinned stack.",
                contentX + 10, rowY + PERFORMANCE_MODS.length * rowHeight + 21, COLOR_MUTED);
    }

    private void drawAbout(DrawContext context) {
        int x = contentX + 10;
        int y = contentY + 35;
        drawCard(context, x, y, contentWidth - 20, 194, COLOR_BLUE);
        drawText(context, "AZYRON CLIENT", x + 18, y + 20, COLOR_TEXT);
        drawText(context, "Client version   1.0", x + 18, y + 49, COLOR_MUTED);
        drawText(context, "Minecraft        Java Edition 1.21.11 only", x + 18, y + 70, COLOR_MUTED);
        drawText(context, "Loader           Fabric 0.20.1+", x + 18, y + 91, COLOR_MUTED);
        drawText(context, "Developer        JIGNESH GAMING", x + 18, y + 112, COLOR_MUTED);
        drawText(context, "Focus            low overhead, stability, and practical settings", x + 18, y + 133, COLOR_MUTED);
        drawText(context, "No fixed FPS promise: results depend on hardware, drivers, mods, and world load.",
                x + 18, y + 165, COLOR_BLUE);
    }

    private void drawChip(DrawContext context, int x, int y, int chipWidth, String title, String value) {
        context.fill(x, y, x + chipWidth, y + 37, 0xCC0D111E);
        context.fill(x, y, x + 2, y + 37, COLOR_PURPLE);
        drawText(context, title, x + 8, y + 6, COLOR_MUTED);
        drawText(context, value, x + 8, y + 20, COLOR_TEXT);
    }

    private void drawCard(DrawContext context, int x, int y, int cardWidth, int cardHeight, int accent) {
        context.fill(x, y, x + cardWidth, y + cardHeight, COLOR_CARD);
        context.fill(x, y, x + 2, y + cardHeight, accent);
        context.fill(x + 2, y + cardHeight - 1, x + cardWidth, y + cardHeight, 0xFF252B3B);
    }

    private void drawFooter(DrawContext context) {
        int y = panelY + panelHeight - 16;
        drawText(context, "CLIENT 1.0  ·  MINECRAFT JAVA 1.21.11 ONLY",
                panelX + panelWidth - 252, y, 0xFF747D94);
    }

    private void drawText(DrawContext context, String value, int x, int y, int color) {
        context.drawTextWithShadow(textRenderer, value, x, y, color);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public enum Page {
        HOME("OVERVIEW", "A fast, quiet base for Minecraft Java 1.21.11"),
        PERFORMANCE("FPS BOOST", "Pick a practical profile; apply it instantly."),
        HUD("HUD", "Toggle modules and adjust each overlay position."),
        MODS("MODS", "Performance stack status for this Fabric instance."),
        ABOUT("ABOUT", "Built by JIGNESH GAMING for version 1.21.11 only.");

        private final String navLabel;
        private final String title;
        private final String subtitle;

        Page(String navLabel, String subtitle) {
            this.navLabel = navLabel;
            this.title = navLabel;
            this.subtitle = subtitle;
        }
    }

    private record ModEntry(String name, String modId, String version, String role) {
    }
}
