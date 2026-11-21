package de.resourcebuilder.gui;

import com.mojang.blaze3d.platform.NativeImage;

import de.resourcebuilder.ResourceBuilderMod;
import de.resourcebuilder.StudioSettings;
import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.pack.TextureVariant;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

final class PreviewCache implements AutoCloseable {
    private static final int FRAMES = 24, COLS = 6, ROWS = 4, TILE = 80;
    private static final ExecutorService BAKER =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "Resource Builder portraits");
                        t.setDaemon(true);
                        return t;
                    });
    private static long next;
    private final Map<TextureVariant, Entry> images = new LinkedHashMap<>(32, .75f, true);

    private static final class Entry {
        Identifier still, atlas;
        volatile boolean discarded;
        CompletableFuture<BufferedImage> pending;

        void close() {
            discarded = true;
            if (pending != null) pending.cancel(false);
            var tm = Minecraft.getInstance().getTextureManager();
            if (still != null) tm.release(still);
            if (atlas != null) tm.release(atlas);
        }
    }

    boolean draw(
            GuiGraphicsExtractor g,
            PvPItem item,
            TextureVariant variant,
            int x,
            int y,
            int width,
            int height) {
        var e = images.get(variant);
        if (e == null) {
            if (images.size() >= 96) {
                var i = images.entrySet().iterator();
                i.next().getValue().close();
                i.remove();
            }
            e = create(item, variant);
            images.put(variant, e);
        }
        if (e.pending != null && e.pending.isDone()) {
            try {
                e.atlas = upload(e.pending.join());
            } catch (RuntimeException ex) {
                ResourceBuilderMod.LOGGER.warn("Animated preview failed", ex);
            }
            e.pending = null;
        }
        if (e.atlas != null && StudioSettings.S.animations) {
            int frame = (int) ((System.nanoTime() / 180_000_000L) % FRAMES),
                    u = frame % COLS,
                    v = frame / COLS;
            g.blit(
                    e.atlas,
                    x,
                    y,
                    x + width,
                    y + height,
                    (float) u / COLS,
                    (float) (u + 1) / COLS,
                    (float) v / ROWS,
                    (float) (v + 1) / ROWS);
            return true;
        }
        if (e.still == null) return false;
        float crop = (1f - (float) height / width) / 2f;
        g.blit(e.still, x, y, x + width, y + height, 0, 1, crop, 1 - crop);
        return true;
    }

    void portrait(
            GuiGraphicsExtractor g,
            PvPItem item,
            TextureVariant variant,
            int x,
            int y,
            int size,
            int color) {
        Entry entry = images.computeIfAbsent(variant, v -> create(item, v));
        if (entry.still != null)
            g.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    entry.still,
                    x,
                    y,
                    0,
                    0,
                    size,
                    size,
                    112,
                    112,
                    112,
                    112,
                    color);
    }

    private Entry create(PvPItem item, TextureVariant variant) {
        Entry e = new Entry();
        try {
            var scene = ModelPreview.scene(variant);
            // The first frame resolves all game resources on the render thread.
            var first = scene.frame(item, 0);
            e.still = upload(first);
            if (StudioSettings.S.animations && ModelPreview.rotates(item)) {
                e.pending =
                        CompletableFuture.supplyAsync(
                                () -> {
                                    var sheet =
                                            new BufferedImage(
                                                    COLS * TILE,
                                                    ROWS * TILE,
                                                    BufferedImage.TYPE_INT_ARGB);
                                    var graphics = sheet.createGraphics();
                                    try {
                                        for (int i = 0; i < FRAMES && !e.discarded; i++) {
                                            var image =
                                                    i == 0
                                                            ? first
                                                            : scene.frame(
                                                                    item, 2 * Math.PI * i / FRAMES);
                                            graphics.drawImage(
                                                    image,
                                                    (i % COLS) * TILE,
                                                    (i / COLS) * TILE,
                                                    TILE,
                                                    TILE,
                                                    null);
                                        }
                                    } catch (IOException ex) {
                                        throw new CompletionException(ex);
                                    } finally {
                                        graphics.dispose();
                                    }
                                    return sheet;
                                },
                                BAKER);
            }
        } catch (IOException | RuntimeException ex) {
            ResourceBuilderMod.LOGGER.warn("Preview failed for {}", item, ex);
        }
        return e;
    }

    private static Identifier upload(BufferedImage image) {
        var pixels = new NativeImage(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++) pixels.setPixel(x, y, image.getRGB(x, y));
        var texture = new DynamicTexture(() -> "Studio portrait", pixels);
        var id = Identifier.fromNamespaceAndPath("resourcebuilder", "preview/" + (++next));
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return id;
    }

    @Override
    public void close() {
        images.values().forEach(Entry::close);
        images.clear();
    }
}
