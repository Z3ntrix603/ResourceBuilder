package de.resourcebuilder.pack;

import com.google.gson.Gson;

import de.resourcebuilder.category.PvPItem;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.imageio.ImageIO;

public final class StyleAssets {
    public static final String ROOT = "assets/minecraft/textures/";
    public static final Map<String, String[]> CATALOG;
    private static final Map<String, byte[]> BASE = new HashMap<>();
    private static final String[] NAMES = {
        "Lavender",
        "Ice Blue",
        "Light Pink",
        "Mint",
        "Obsidian",
        "Silver",
        "Ruby",
        "Peach",
        "Turquoise",
        "Gold",
        "Indigo",
        "Emerald",
        "Cobalt",
        "Rose",
        "Pearl",
        "Amber",
        "Lilac"
    };
    private static final int[] COLORS = {
        0xb798f0, 0x72c7ff, 0xfb9ec8, 0x79e4bb, 0x685887, 0xb8c8da, 0xeb405a, 0xffb88e, 0x37dee1,
        0xffcf58, 0x8163f9, 0x34d478, 0x4878eb, 0xe88699, 0xeaf3ff, 0xec914d, 0xca9bf5
    };
    private static final String[] FINISH = {"", " Frost", " Prism", " Neon", " Vein", " Dusk"};

    static {
        try (var r =
                new InputStreamReader(
                        Objects.requireNonNull(
                                StyleAssets.class.getResourceAsStream(
                                        "/assets/resourcebuilder/catalog.json")),
                        StandardCharsets.UTF_8)) {
            CATALOG =
                    new Gson()
                            .fromJson(
                                    r,
                                    new com.google.gson.reflect.TypeToken<
                                            Map<String, String[]>>() {}.getType());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static int color(int style) {
        return COLORS[style % COLORS.length];
    }

    public static String name(PvPItem item, int index) {
        if (item == PvPItem.FONT)
            return new String[] {
                        "Bold", "Italic", "Bold Italic", "Fine", "Shadow", "Outline", "Round"
                    }
                    [index];
        if (item == PvPItem.SKY)
            return new String[] {
                        "Dusk",
                        "Polar Sky",
                        "Rose Dawn",
                        "Aurora",
                        "Midnight",
                        "Overcast",
                        "Blood Moon",
                        "Apricot",
                        "Lagoon",
                        "Golden Hour",
                        "Nebula",
                        "Boreal",
                        "Blue Hour",
                        "Petal",
                        "Cloudbreak",
                        "Amber Haze",
                        "Lilac Night"
                    }
                    [index];
        return NAMES[index % NAMES.length] + FINISH[index / NAMES.length];
    }

    public static int count(PvPItem item) {
        return switch (item) {
            case CRYSTALS -> 101;
            case TOTEM -> 77;
            case ENDER_PEARL -> 95;
            case GOLDEN_APPLE -> 61;
            case BLOCKS -> 48;
            case DIAMOND_SWORD, NETHERITE_SWORD -> 90;
            case HUD -> 35;
            case OBSIDIAN -> 65;
            case GLOWSTONE -> 73;
            case RESPAWN_ANCHOR -> 55;
            case DIAMOND_ARMOR -> 38;
            case NETHERITE_ARMOR -> 39;
            case ELYTRA -> 19;
            case FONT -> 7;
            default -> 17;
        };
    }

    public static byte[] vanilla(String full) {
        return BASE.computeIfAbsent(
                full,
                key -> {
                    try {
                        var id =
                                Identifier.withDefaultNamespace(
                                        key.substring("assets/minecraft/".length()));
                        var source =
                                Minecraft.getInstance()
                                        .getVanillaPackResources()
                                        .getResource(PackType.CLIENT_RESOURCES, id);
                        if (source != null)
                            try (var stream = source.get()) {
                                return stream.readAllBytes();
                            }
                        try (var stream = StyleAssets.class.getResourceAsStream("/" + key)) {
                            return stream == null ? new byte[0] : stream.readAllBytes();
                        }
                    } catch (Exception e) {
                        return new byte[0];
                    }
                });
    }

    public static Map<String, byte[]> generate(PvPItem item, int index) {
        var map = new LinkedHashMap<String, byte[]>();
        for (String texture : CATALOG.getOrDefault(item.name(), new String[0])) {
            String key = ROOT + texture + ".png";
            byte[] data = vanilla(key);
            if (item == PvPItem.ENCHANTED_GOLDEN_APPLE)
                data = vanilla(ROOT + "item/golden_apple.png");
            if (data.length == 0) continue;
            if (index >= 17) {
                String alternative =
                        switch (item) {
                            case TOTEM ->
                                    new String[] {
                                                "",
                                                "item/poppy",
                                                "item/nether_star",
                                                "item/ghast_tear",
                                                "item/golden_carrot",
                                                "item/echo_shard"
                                            }
                                            [index / 17];
                            case GOLDEN_APPLE ->
                                    new String[] {
                                                "",
                                                "item/apple",
                                                "item/cookie",
                                                "item/sweet_berries"
                                            }
                                            [index / 17];
                            case ENDER_PEARL ->
                                    new String[] {
                                                "",
                                                "item/ender_eye",
                                                "item/fire_charge",
                                                "item/snowball",
                                                "item/heart_of_the_sea",
                                                "item/slime_ball"
                                            }
                                            [index / 17];
                            case OBSIDIAN ->
                                    new String[] {
                                                "",
                                                "block/polished_blackstone",
                                                "block/crying_obsidian",
                                                "block/quartz_block_side"
                                            }
                                            [index / 17];
                            case GLOWSTONE ->
                                    new String[] {
                                                "",
                                                "block/pearlescent_froglight_side",
                                                "block/shroomlight",
                                                "block/ochre_froglight_side",
                                                "block/copper_bulb_lit"
                                            }
                                            [index / 17];
                            default -> "";
                        };
                if (!alternative.isEmpty()) {
                    byte[] other = vanilla(ROOT + alternative + ".png");
                    if (other.length > 0) data = other;
                }
            }
            map.put(key, tint(data, index, item, texture));
            byte[] metadata = vanilla(key + ".mcmeta");
            if (metadata.length > 0) map.put(key + ".mcmeta", metadata);
        }
        return map;
    }

    private static byte[] tint(byte[] png, int index, PvPItem item, String texture) {
        try {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(png));
            int w = input.getWidth(),
                    h = input.getHeight(),
                    rgb = COLORS[index % COLORS.length],
                    finish = index / COLORS.length;
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            boolean meter =
                    item == PvPItem.HEARTS || item == PvPItem.HUNGER || item == PvPItem.XP_BAR;
            boolean statusEffect =
                    texture.contains("/poisoned_")
                            || texture.contains("/withered_")
                            || texture.contains("/absorbing_")
                            || texture.contains("/frozen_")
                            || texture.endsWith("_hunger");
            int low = 255, high = 0;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int c = input.getRGB(x, y);
                    if ((c >>> 24) == 0) continue;
                    int l = luma(c);
                    low = Math.min(low, l);
                    high = Math.max(high, l);
                }
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int c = input.getRGB(x, y);
                    int pixelColor = rgb;
                    boolean glass =
                            item == PvPItem.CRYSTALS
                                    && texture.equals("entity/end_crystal/end_crystal")
                                    && y < h / 2
                                    && x < w / 2;
                    if (glass) {
                        int unit = w / 8, px = x % unit, py = y % unit;
                        boolean face = y >= unit || (x >= unit && x < unit * 3);
                        if (!face) continue;
                        int edge =
                                Math.min(Math.min(px, py), Math.min(unit - 1 - px, unit - 1 - py));
                        int pattern = (index + finish) % 6;
                        boolean diagonal = (px == py || px + py == unit - 1);
                        boolean draw =
                                switch (pattern) {
                                    case 0 -> edge < 1;
                                    case 1 -> edge < 2 || (diagonal && edge < 4);
                                    case 2 ->
                                            edge < 1
                                                    || Math.abs(px - unit / 2)
                                                                    + Math.abs(py - unit / 2)
                                                            == unit / 2;
                                    case 3 -> edge == 1 || (diagonal && edge < 3);
                                    case 4 -> edge == 0 || (diagonal && ((px + py) % 3 != 1));
                                    default -> (c >>> 24) > 0;
                                };
                        if (!draw) continue;
                        c = 0xff000000 | ((px < 2 || py < 2) ? 0xf8f8f8 : 0xbbbbbb);
                        if (pattern == 0 || pattern == 3) pixelColor = 0xe5f4ff;
                        if (pattern == 2) pixelColor = COLORS[(index + 4) % COLORS.length];
                    }
                    if ((c >>> 24) == 0) continue;
                    if (meter) {
                        out.setRGB(x, y, meterTint(c, rgb, statusEffect));
                        continue;
                    }
                    double l = (luma(c) - low) / (double) Math.max(1, high - low);
                    double light =
                            switch (finish) {
                                case 1 -> Math.sqrt(l) * .76 + .20;
                                case 2 -> Math.floor((l + .12 * Math.sin((x - y) * .5)) * 5) / 5;
                                case 3 -> l > .55 ? 1 : l * .43;
                                case 4 ->
                                        l
                                                + .26
                                                        * Math.pow(
                                                                Math.max(
                                                                        0,
                                                                        Math.cos(
                                                                                x * .9
                                                                                        + Math.sin(
                                                                                                        y
                                                                                                                * .7)
                                                                                                * 2)),
                                                                8);
                                case 5 -> l * .74 + .12 * Math.sin(y * .3);
                                default -> l;
                            };
                    if (item == PvPItem.FONT) {
                        // Keep the atlas white so chat formatting colours remain usable.
                        int unit = w / 16, tileX = x / unit * unit, tileY = y / unit * unit;
                        int dx =
                                (index == 1 || index == 2)
                                        ? (unit - 1 - (y - tileY)) / (Math.max(1, unit / 3))
                                        : 0;
                        int nx = x + dx;
                        if (nx >= tileX + unit) continue;
                        if (index == 5 || index == 4) {
                            for (int oy = 0; oy < 2; oy++)
                                for (int ox = 0; ox < 2; ox++) {
                                    int xx = nx + ox, yy = y + oy;
                                    if (xx < tileX + unit
                                            && yy < tileY + unit
                                            && (input.getRGB(xx, yy) >>> 24) == 0)
                                        out.setRGB(xx, yy, index == 5 ? 0xffb3b3b3 : 0xff777777);
                                }
                        }
                        if (index == 3
                                && x > tileX
                                && x < tileX + unit - 1
                                && (input.getRGB(x - 1, y) >>> 24) > 0
                                && (input.getRGB(x + 1, y) >>> 24) > 0) continue;
                        out.setRGB(nx, y, c);
                        if ((index == 0 || index == 2 || index == 6 && y % unit == unit / 2)
                                && nx < tileX + unit - 1) out.setRGB(nx + 1, y, c);
                    } else {
                        if (item.name().endsWith("_ARMOR")
                                && texture.startsWith("entity/equipment/")) {
                            int ux = x * 64 / w, uy = y * 32 / h;
                            if (index % 3 == 0 && (uy % 8 == 6 || (ux + uy) % 13 == 0))
                                pixelColor = 0xe7d09c;
                            if (index % 3 == 1 && (uy % 8 == 0 || ux % 12 == 0))
                                pixelColor = 0xc8e7ff;
                            if (index % 3 == 2 && (ux + 2 * uy) % 11 < 2)
                                pixelColor = COLORS[(index + 5) % COLORS.length];
                            light = .17 + .85 * light;
                        }
                        int color = 0;
                        for (int shift : new int[] {16, 8, 0}) {
                            double channel = (pixelColor >> shift) & 255;
                            double value =
                                    channel * (.17 + .86 * light) + Math.max(0, light - .66) * 155;
                            color |= Math.clamp((int) value, 0, 255) << shift;
                        }
                        out.setRGB(x, y, (c & 0xff000000) | color);
                    }
                }
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int luma(int c) {
        return (((c >> 16) & 255) * 3 + ((c >> 8) & 255) * 6 + (c & 255)) / 10;
    }

    private static int meterTint(int source, int palette, boolean statusEffect) {
        // Preserve dim empty slots, bright blinking overlays and recognizable effect colors.
        int r = (source >> 16) & 255, g = (source >> 8) & 255, b = source & 255;
        double brightness = Math.max(r, Math.max(g, b)) / 255.0;
        double highlight = Math.min(r, Math.min(g, b)) / 255.0;
        int result = source & 0xff000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = (palette >> shift) & 255;
            double value = brightness * channel + highlight * highlight * (255 - channel);
            if (statusEffect) value = .35 * value + .65 * ((source >> shift) & 255);
            result |= (int) Math.round(value) << shift;
        }
        return result;
    }

    private StyleAssets() {}
}
