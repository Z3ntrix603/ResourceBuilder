package de.resourcebuilder.gui;

import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.pack.TextureVariant;

import org.joml.Matrix3d;
import org.joml.Vector3d;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

/** Orthographic catalogue portraits. All UVs sample the same PNGs written to the live pack. */
public final class ModelPreview {
    private static final int SIZE = 112;

    private record Face(Vector3d a, Vector3d b, Vector3d d, BufferedImage tex, double depth) {}

    private final java.util.List<Face> faces = new ArrayList<>();
    private final TextureVariant variant;
    private final Map<String, BufferedImage> images = new HashMap<>();
    private double scale, originY;
    private Matrix3d rotation;
    private double yaw;

    private ModelPreview(TextureVariant variant) {
        this.variant = variant;
    }

    static ModelPreview scene(TextureVariant variant) {
        return new ModelPreview(variant);
    }

    BufferedImage frame(PvPItem item, double angle) throws IOException {
        faces.clear();
        yaw = angle;
        return paint(item);
    }

    static boolean rotates(PvPItem item) {
        return item.name().endsWith("_ARMOR")
                || switch (item) {
                    case CRYSTALS,
                            OBSIDIAN,
                            CRYING_OBSIDIAN,
                            GLOWSTONE,
                            RESPAWN_ANCHOR,
                            TNT,
                            BLOCKS ->
                            true;
                    default -> false;
                };
    }

    private BufferedImage load(String path) throws IOException {
        if (!images.containsKey(path))
            images.put(path, ImageIO.read(new ByteArrayInputStream(variant.texture(path))));
        return images.get(path);
    }

    private BufferedImage paint(PvPItem item) throws IOException {
        BufferedImage canvas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (item.name().endsWith("_ARMOR")) {
            armor(item);
        } else if (item == PvPItem.CRYSTALS) {
            BufferedImage atlas = load("entity/end_crystal/end_crystal");
            scale = 7.4;
            originY = SIZE * .5;
            for (int i = 0; i < 3; i++) {
                rotation =
                        new Matrix3d()
                                .rotateX(.15)
                                .rotateY(.56 + i * .95 + yaw)
                                .rotateZ(.65 + i * .48);
                box(atlas, 64, 32, i == 2 ? 32 : 0, 0, 0, 0, 0, 8, 8, 8, -i * .52);
            }
        } else if (Set.of(
                        PvPItem.OBSIDIAN,
                        PvPItem.CRYING_OBSIDIAN,
                        PvPItem.GLOWSTONE,
                        PvPItem.TNT,
                        PvPItem.RESPAWN_ANCHOR,
                        PvPItem.BLOCKS)
                .contains(item)) {
            scale = 4.45;
            originY = SIZE * .5;
            rotation = new Matrix3d().rotateX(.43).rotateY(.66 + yaw);
            String side =
                    switch (item) {
                        case RESPAWN_ANCHOR -> "block/respawn_anchor_side0";
                        case BLOCKS -> "block/grass_block_side";
                        default -> item.getPrimaryTexturePath();
                    };
            String top = item == PvPItem.TNT ? "block/tnt_top" : item.getPrimaryTexturePath();
            BufferedImage a = load(side), b = load(top);
            if (a != null && b != null) {
                Vector3d[] p = vertices(0, 0, 0, 8, 8, 8);
                face(p, 4, 5, 6, a, 0, 0, a.getWidth(), a.getWidth(), 1);
                face(p, 0, 4, 2, a, 0, 0, a.getWidth(), a.getWidth(), .68);
                face(p, 5, 1, 7, a, 0, 0, a.getWidth(), a.getWidth(), .68);
                face(p, 1, 0, 3, a, 0, 0, a.getWidth(), a.getWidth(), .7);
                face(p, 0, 1, 4, b, 0, 0, b.getWidth(), b.getWidth(), 1);
            }
        } else {
            String path = item.getPrimaryTexturePath();
            if (item == PvPItem.ENCHANTED_GOLDEN_APPLE && variant.isDefault())
                path = "item/golden_apple";
            var im = load(path);
            if (im != null) {
                if (item == PvPItem.HUD) {
                    g.drawImage(im, 2, 49, 108, 14, null);
                    String[] icons = {
                        "diamond_sword",
                        "end_crystal",
                        "totem_of_undying",
                        "ender_pearl",
                        "golden_apple",
                        "diamond_pickaxe",
                        "bow",
                        "arrow",
                        "water_bucket"
                    };
                    for (int n = 0; n < icons.length; n++) {
                        var icon = load("item/" + icons[n]);
                        if (icon != null) g.drawImage(icon, 4 + n * 12, 51, 9, 9, null);
                    }
                } else if (item == PvPItem.HEARTS || item == PvPItem.HUNGER) {
                    boolean hearts = item == PvPItem.HEARTS;
                    var empty =
                            load(
                                    hearts
                                            ? "gui/sprites/hud/heart/container"
                                            : "gui/sprites/hud/food_empty");
                    var half =
                            load(
                                    hearts
                                            ? "gui/sprites/hud/heart/half"
                                            : "gui/sprites/hud/food_half");
                    for (int slot = 0; slot < 4; slot++) {
                        int x = 7 + 24 * (hearts ? slot : 3 - slot);
                        g.drawImage(empty, x, 43, 27, 27, null);
                        if (slot < 3) g.drawImage(slot == 2 ? half : im, x, 43, 27, 27, null);
                    }
                } else if (item == PvPItem.XP_BAR) {
                    var background = load("gui/sprites/hud/experience_bar_background");
                    g.drawImage(background, 2, 52, 108, 8, null);
                    Shape clip = g.getClip();
                    g.clipRect(2, 52, 72, 8);
                    g.drawImage(im, 2, 52, 108, 8, null);
                    g.setClip(clip);
                } else if (item == PvPItem.SKY) {
                    int rgb = variant.paletteColor();
                    if (rgb == 0) rgb = 0x86baff;
                    g.setPaint(
                            new GradientPaint(
                                    0,
                                    0,
                                    new Color(rgb).darker(),
                                    0,
                                    SIZE,
                                    new Color(rgb).brighter()));
                    g.fillRect(0, 0, SIZE, SIZE);
                    var cloud = load("environment/clouds");
                    if (cloud != null) {
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .32f));
                        g.drawImage(cloud, 0, 47, SIZE, 78, 23, 11, 73, 31, null);
                        g.setComposite(AlphaComposite.SrcOver);
                    }
                    var sun = load("environment/celestial/sun");
                    if (sun != null) {
                        // Minecraft blends celestial sprites additively against the sky.
                        for (int y = 0; y < 22; y++) {
                            for (int x = 0; x < 22; x++) {
                                int base = canvas.getRGB(73 + x, 29 + y);
                                int glow =
                                        sun.getRGB(
                                                x * sun.getWidth() / 22, y * sun.getHeight() / 22);
                                int alpha = glow >>> 24;
                                int result = 0xff000000;
                                for (int shift = 0; shift <= 16; shift += 8) {
                                    int channel =
                                            ((base >> shift) & 255)
                                                    + ((glow >> shift) & 255) * alpha / 255;
                                    result |= Math.min(255, channel) << shift;
                                }
                                canvas.setRGB(73 + x, 29 + y, result);
                            }
                        }
                    }
                } else if (item == PvPItem.FONT) {
                    for (int i = 0; i < 6; i++) {
                        int ch = "Abc123".charAt(i), unit = im.getWidth() / 16;
                        int x = 14 + i % 3 * 29,
                                y = 21 + i / 3 * 33,
                                u = ch % 16 * unit,
                                v = ch / 16 * unit;
                        g.drawImage(im, x, y, x + 30, y + 30, u, v, u + unit, v + unit, null);
                    }
                } else {
                    int sw = im.getWidth(), sh = Math.min(im.getHeight(), sw);
                    if (item == PvPItem.SHIELD) {
                        int unit = sw / 64;
                        g.drawImage(im, 27, 5, 85, 106, unit, unit, 13 * unit, 23 * unit, null);
                    } else {
                        double fit = 94d / Math.max(sw, sh);
                        int w = (int) (sw * fit), h = (int) (sh * fit);
                        g.drawImage(
                                im,
                                (SIZE - w) / 2,
                                (SIZE - h) / 2,
                                (SIZE + w) / 2,
                                (SIZE + h) / 2,
                                0,
                                0,
                                sw,
                                sh,
                                null);
                    }
                }
            }
        }
        faces.sort(Comparator.comparingDouble(Face::depth));
        for (Face f : faces) {
            int w = f.tex.getWidth(), h = f.tex.getHeight();
            g.drawImage(
                    f.tex,
                    new AffineTransform(
                            (f.b.x - f.a.x) / w,
                            (f.b.y - f.a.y) / w,
                            (f.d.x - f.a.x) / h,
                            (f.d.y - f.a.y) / h,
                            f.a.x,
                            f.a.y),
                    null);
        }
        if (item == PvPItem.CRYSTALS) {
            var icon = load("item/end_crystal");
            if (icon != null) {
                g.setColor(new Color(24, 28, 36));
                g.fillRect(85, 85, 26, 26);
                g.drawImage(icon, 87, 87, 22, 22, null);
            }
        }
        g.dispose();
        return canvas;
    }

    private void armor(PvPItem item) throws IOException {
        String material = item.name().split("_")[0].toLowerCase(Locale.ROOT);
        var skin = load("entity/player/wide/steve");
        var outer = load("entity/equipment/humanoid/" + material);
        var inner = load("entity/equipment/humanoid_leggings/" + material);
        scale = 3.02;
        originY = 8;
        rotation = new Matrix3d().rotateX(.10).rotateY(-.35 + .92 * Math.sin(yaw));
        box(skin, 64, 64, 0, 0, 0, 4, 0, 8, 8, 8, 0);
        box(skin, 64, 64, 16, 16, 0, 14, 0, 8, 12, 4, 0);
        box(outer, 64, 32, 0, 0, 0, 4, 0, 8, 8, 8, .8);
        box(outer, 64, 32, 16, 16, 0, 14, 0, 8, 12, 4, .8);
        box(inner, 64, 32, 16, 16, 0, 14, 0, 8, 12, 4, .4);
        for (int side : new int[] {-1, 1}) {
            box(skin, 64, 64, side < 0 ? 40 : 32, side < 0 ? 16 : 48, side * 6, 14, 0, 4, 12, 4, 0);
            box(outer, 64, 32, 40, 16, side * 6, 14, 0, 4, 12, 4, .6);
            box(
                    skin,
                    64,
                    64,
                    side < 0 ? 0 : 16,
                    side < 0 ? 16 : 48,
                    side * 2.1,
                    26,
                    0,
                    4,
                    12,
                    4,
                    0);
            box(inner, 64, 32, 0, 16, side * 2.1, 26, 0, 4, 12, 4, .35);
            box(outer, 64, 32, 0, 16, side * 2.1, 26, 0, 4, 12, 4, .65);
        }
    }

    private Vector3d[] vertices(double x, double y, double z, double a, double b, double c) {
        Vector3d[] p = new Vector3d[8];
        for (int i = 0; i < 8; i++) {
            var v =
                    new Vector3d(
                            x + ((i & 1) == 0 ? -a : a),
                            y + ((i & 2) == 0 ? -b : b),
                            z + ((i & 4) == 0 ? -c : c));
            rotation.transform(v);
            v.mul(scale);
            v.add(SIZE * .5, originY, 0);
            p[i] = v;
        }
        return p;
    }

    private void box(
            BufferedImage tex,
            int tw,
            int th,
            int u,
            int v,
            double x,
            double y,
            double z,
            int w,
            int h,
            int d,
            double inflate) {
        if (tex == null) return;
        Vector3d[] p = vertices(x, y, z, w * .5 + inflate, h * .5 + inflate, d * .5 + inflate);
        int sx = tex.getWidth() / tw, sy = tex.getHeight() / th;
        if (sx < 1 || sy < 1) return;
        face(p, 4, 5, 6, tex, (u + d) * sx, (v + d) * sy, w * sx, h * sy, 1);
        face(p, 1, 0, 3, tex, (u + 2 * d + w) * sx, (v + d) * sy, w * sx, h * sy, .77);
        face(p, 0, 4, 2, tex, u * sx, (v + d) * sy, d * sx, h * sy, .77);
        face(p, 5, 1, 7, tex, (u + d + w) * sx, (v + d) * sy, d * sx, h * sy, .83);
        face(p, 0, 1, 4, tex, (u + d) * sx, v * sy, w * sx, d * sy, 1);
        face(p, 6, 7, 2, tex, (u + d + w) * sx, v * sy, w * sx, d * sy, .68);
    }

    private void face(
            Vector3d[] p,
            int a,
            int b,
            int d,
            BufferedImage atlas,
            int u,
            int v,
            int w,
            int h,
            double shade) {
        if (w <= 0 || h <= 0 || u + w > atlas.getWidth() || v + h > atlas.getHeight()) return;
        // Keep back faces for transparent crystal glass. Their depth places them behind the core.
        var patch = atlas.getSubimage(u, v, w, h);
        if (shade != 1) {
            var toned = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = toned.createGraphics();
            g.drawImage(patch, 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop);
            g.setColor(new Color(0, 0, 0, (int) (255 * (1 - shade))));
            g.fillRect(0, 0, w, h);
            g.dispose();
            patch = toned;
        }
        faces.add(new Face(p[a], p[b], p[d], patch, (p[b].z + p[d].z) / 2));
    }
}
