package de.resourcebuilder.pack;

import de.resourcebuilder.category.PvPItem;

import java.util.Map;

/** A catalogue entry. Built-in styles are generated only when first previewed or applied. */
public final class TextureVariant {
    private final String name;
    private final PvPItem item;
    private final int palette;
    private final boolean standard;
    private Map<String, byte[]> files;

    private TextureVariant(
            PvPItem item, String name, int palette, boolean standard, Map<String, byte[]> files) {
        this.item = item;
        this.name = name;
        this.palette = palette;
        this.standard = standard;
        this.files = files;
    }

    static TextureVariant standard(PvPItem item) {
        return new TextureVariant(item, "Default", -1, true, Map.of());
    }

    static TextureVariant builtin(PvPItem item, int style) {
        return new TextureVariant(item, StyleAssets.name(item, style), style, false, null);
    }

    static TextureVariant imported(PvPItem item, String name, Map<String, byte[]> files) {
        return new TextureVariant(item, name, -1, false, files);
    }

    public int paletteColor() {
        return palette < 0 ? 0 : StyleAssets.color(palette);
    }

    public String getPackName() {
        return name;
    }

    private String soundAlias;

    static TextureVariant sound(PvPItem item, String name, String event) {
        var v = new TextureVariant(item, name, -1, false, Map.of());
        v.soundAlias = event;
        return v;
    }

    public String soundAlias() {
        return soundAlias;
    }

    public boolean isDefault() {
        return standard;
    }

    public Map<String, byte[]> files() {
        if (files == null) files = StyleAssets.generate(item, palette);
        return files;
    }

    public byte[] texture(String path) {
        String full = StyleAssets.ROOT + path + ".png";
        byte[] found = files().get(full);
        return found == null ? StyleAssets.vanilla(full) : found;
    }
}
