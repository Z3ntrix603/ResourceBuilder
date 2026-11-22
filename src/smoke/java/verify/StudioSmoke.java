package verify;

import de.resourcebuilder.category.*;
import de.resourcebuilder.gui.TextureStudioScreen;
import de.resourcebuilder.pack.*;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.input.KeyEvent;

import java.nio.file.*;

public class StudioSmoke implements ClientModInitializer {
    int ticks, stage;
    boolean launched;
    java.util.concurrent.CompletableFuture<Void> killCheck;
    TextureStudioScreen screen;

    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(
                mc -> {
                    if (++ticks < 100 || ticks % 24 != 0 || mc.getOverlay() != null) return;
                    try {
                        if (!launched) {
                            launched = true;
                            Files.deleteIfExists(Path.of("PASSED"));
                            Files.deleteIfExists(Path.of("FAILED"));
                            mc.createWorldOpenFlows()
                                    .createFreshLevel(
                                            "RenderCheck-" + System.currentTimeMillis(),
                                            new net.minecraft.world.level.LevelSettings(
                                                    "StudioTest",
                                                    net.minecraft.world.level.GameType.CREATIVE,
                                                    false,
                                                    net.minecraft.world.Difficulty.NORMAL,
                                                    true,
                                                    new net.minecraft.world.level.gamerules
                                                            .GameRules(
                                                            net.minecraft.world.flag.FeatureFlags
                                                                    .DEFAULT_FLAGS),
                                                    net.minecraft.world.level.WorldDataConfiguration
                                                            .DEFAULT),
                                            new net.minecraft.world.level.levelgen.WorldOptions(
                                                    42, false, false),
                                            net.minecraft.world.level.levelgen.presets.WorldPresets
                                                    ::createNormalWorldDimensions,
                                            mc.screen);
                            return;
                        }
                        if (mc.level == null
                                || mc.player == null
                                || (stage == 0 && mc.screen != null)) return;
                        var pm = LivePackManager.getInstance();
                        switch (stage++) {
                            case 0 -> {
                                mc.player.connection.sendCommand(
                                        "summon minecraft:end_crystal ~3 ~1 ~");
                                mc.player.connection.sendCommand("give @s minecraft:diamond_sword");
                                mc.player.connection.sendCommand("rb");
                            }
                            case 1 -> {
                                if (!(mc.screen instanceof TextureStudioScreen studio))
                                    throw new IllegalStateException("Command did not open studio");
                                screen = studio;
                            }
                            case 2 -> {
                                cap(mc, "crystal");
                                var list = pm.getVariants(PvPItem.CRYSTALS);
                                if (list.size() < 102)
                                    throw new IllegalStateException("missing crystal variants");
                                pm.selectVariant(PvPItem.CRYSTALS, list.get(3), true);
                            }
                            case 3 -> {
                                pressTab("Armor");
                                var id =
                                        net.minecraft.resources.Identifier.withDefaultNamespace(
                                                "textures/entity/end_crystal/end_crystal.png");
                                try (var in = mc.getResourceManager().open(id)) {
                                    if (!java.util.Arrays.equals(
                                            in.readAllBytes(),
                                            pm.getActiveSelection(PvPItem.CRYSTALS)
                                                    .texture("entity/end_crystal/end_crystal")))
                                        throw new IllegalStateException("World texture mismatch");
                                }
                            }
                            case 4 -> {
                                cap(mc, "armor");
                                pm.selectVariant(
                                        PvPItem.DIAMOND_ARMOR,
                                        pm.getVariants(PvPItem.DIAMOND_ARMOR).get(2),
                                        true);
                            }
                            case 5 -> {
                                var id =
                                        net.minecraft.resources.Identifier.withDefaultNamespace(
                                                "textures/entity/equipment/humanoid_leggings/diamond.png");
                                try (var in = mc.getResourceManager().open(id)) {
                                    if (!java.util.Arrays.equals(
                                            in.readAllBytes(),
                                            pm.getActiveSelection(PvPItem.DIAMOND_ARMOR)
                                                    .texture(
                                                            "entity/equipment/humanoid_leggings/diamond")))
                                        throw new IllegalStateException("Leggings mismatch");
                                }
                                cap(mc, "applied");
                                var exported = pm.exportMix();
                                try (var zip = new java.util.zip.ZipFile(exported.toFile())) {
                                    if (zip.getEntry(
                                                    "assets/minecraft/textures/entity/equipment/humanoid/diamond.png")
                                            == null)
                                        throw new AssertionError("Armor missing from export");
                                }
                                var button =
                                        screen.children().stream()
                                                .filter(
                                                        o ->
                                                                o
                                                                                instanceof
                                                                                net.minecraft.client
                                                                                                .gui
                                                                                                .components
                                                                                                .Button
                                                                                        b
                                                                        && b.getMessage()
                                                                                .getString()
                                                                                .equals("Settings"))
                                                .map(
                                                        o ->
                                                                (net.minecraft.client.gui.components
                                                                                .Button)
                                                                        o)
                                                .findFirst()
                                                .orElseThrow();
                                button.onPress(new KeyEvent(257, 0, 0));
                            }
                            case 6 -> {
                                cap(mc, "settings");
                                if (mc.screen == screen)
                                    throw new AssertionError("Settings navigation failed");
                                mc.screen.onClose();
                                pm.selectVariant(
                                        PvPItem.SOUND_CRYSTAL,
                                        pm.getVariants(PvPItem.SOUND_CRYSTAL).get(1),
                                        true);
                            }
                            case 7 -> {
                                var id =
                                        net.minecraft.resources.Identifier.withDefaultNamespace(
                                                "sounds.json");
                                try (var in = mc.getResourceManager().open(id)) {
                                    if (!new String(in.readAllBytes())
                                            .contains("amethyst_block.chime"))
                                        throw new AssertionError("Sound alias missing");
                                }
                                pressTab("Sky");
                            }
                            case 8 -> {
                                cap(mc, "sky");
                                pressTab("Hotbar");
                            }
                            case 9 -> {
                                cap(mc, "hotbar");
                                pressTab("Effects");
                                pressTab("Sounds");
                                pressTab("Items");
                                pressTab("Crosshair");
                                pressTab("Font");
                                pressTab("Vitals");
                            }
                            case 10 -> {
                                cap(mc, "hearts");
                                HudChecks.apply(pm);
                                screen.keyPressed(
                                        new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
                            }
                            case 11 -> {
                                HudChecks.verify(mc, pm);
                                cap(mc, "hunger");
                                screen.keyPressed(
                                        new KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
                            }
                            case 12 -> {
                                cap(mc, "xp-bar");
                                pressTab("Kill Effects");
                                pressTab("Lightning");
                                if (!"Lightning"
                                        .equals(de.resourcebuilder.StudioSettings.S.killEffect))
                                    throw new AssertionError("Lightning selection failed");
                                pressTab("Ring");
                                if (!"Ring".equals(de.resourcebuilder.StudioSettings.S.killEffect))
                                    throw new AssertionError("Ring selection failed");
                                if (mc.options.getMenuBackgroundBlurriness() < 8)
                                    throw new AssertionError("Studio blur missing");
                            }
                            case 13 -> {
                                cap(mc, "kill-effects");
                                pressTab("Off");
                                if (!"Off".equals(de.resourcebuilder.StudioSettings.S.killEffect))
                                    throw new AssertionError("Off selection failed");
                                pressTab("Hotbar");
                                pressTab("Kill Effects");
                                int fullWidth = screen.width, fullHeight = screen.height;
                                screen.resize(320, 240);
                                for (String section :
                                        new String[] {
                                            "Items",
                                            "Armor",
                                            "Hotbar",
                                            "Vitals",
                                            "Sky",
                                            "Font",
                                            "Crosshair",
                                            "Effects",
                                            "Mace FX",
                                            "Sounds",
                                            "Kill Effects"
                                        }) {
                                    pressTab(section);
                                }
                                pressTab("Ring");
                                pressTab("Off");
                                screen.resize(fullWidth, fullHeight);
                                screen.onClose();
                                if (!java.util.Objects.equals(
                                        mc.options.getMenuBackgroundBlurriness(),
                                        mc.options.menuBackgroundBlurriness().get()))
                                    throw new AssertionError("Studio blur leaked into gameplay");
                                killCheck = KillEffectSmoke.run(mc);
                            }
                            case 14 -> {
                                if (!killCheck.isDone()) {
                                    stage--;
                                    return;
                                }
                                killCheck.join();
                                screen = new TextureStudioScreen();
                                mc.setScreen(screen);
                            }
                            case 15 -> {
                                pressTab("Mace FX");
                                pressTab("Fracture");
                                if (!"Fracture"
                                        .equals(de.resourcebuilder.StudioSettings.S.maceEffect))
                                    throw new AssertionError("Mace effect selection failed");
                                mc.getToastManager().clear();
                            }
                            case 16 -> {
                                cap(mc, "mace-effects");
                            }
                            case 17 -> {
                                pressTab("Impact");
                                pressTab("Off");
                                screen.onClose();
                                MaceEffectSmoke.run(mc);
                                Files.writeString(
                                        Path.of("PASSED"),
                                        "Verified English studio navigation, HUD galleries,"
                                                + " isolated blur, kill and mace effects, armor and"
                                                + " crystal textures, settings, sound and export.");
                                mc.stop();
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        try {
                            Files.writeString(Path.of("FAILED"), t.toString());
                        } catch (Exception e) {
                        }
                        mc.stop();
                    }
                });
    }

    void pressTab(String label) {
        var button =
                screen.children().stream()
                        .filter(net.minecraft.client.gui.components.Button.class::isInstance)
                        .map(net.minecraft.client.gui.components.Button.class::cast)
                        .filter(b -> b.getMessage().getString().equals(label))
                        .findFirst()
                        .orElseThrow();
        var mouse =
                new net.minecraft.client.input.MouseButtonEvent(
                        button.getX() + button.getWidth() / 2,
                        button.getY() + button.getHeight() / 2,
                        new net.minecraft.client.input.MouseButtonInfo(0, 0));
        if (!screen.mouseClicked(mouse, false))
            throw new AssertionError("Tab click failed: " + label);
    }

    void cap(Minecraft mc, String name) {
        Screenshot.takeScreenshot(
                mc.getMainRenderTarget(),
                img -> {
                    try (img) {
                        img.writeToFile(Path.of(name + ".png"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
