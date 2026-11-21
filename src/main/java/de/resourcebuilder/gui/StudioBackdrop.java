package de.resourcebuilder.gui;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.*;
import java.nio.file.*;

final class StudioBackdrop {
    static final Path CUSTOM = Path.of("config", "resourcebuilder", "background.png");
    private static final Identifier CUSTOM_ID =
            Identifier.fromNamespaceAndPath("resourcebuilder", "studio/custom");
    private static boolean loaded;

    static void importImage(Path path) throws IOException {
        if (Files.size(path) > 16_000_000L) throw new IOException("Max. 16 MB");
        try (var in = javax.imageio.ImageIO.createImageInputStream(path.toFile())) {
            if (in == null) throw new IOException("PNG / JPG");
            var readers = javax.imageio.ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw new IOException("PNG / JPG");
            var reader = readers.next();
            try {
                reader.setInput(in);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000L)
                    throw new IOException("Max. 16 MP");
                var image = reader.read(0);
                Files.createDirectories(CUSTOM.getParent());
                javax.imageio.ImageIO.write(image, "png", CUSTOM.toFile());
                loaded = false;
            } finally {
                reader.dispose();
            }
        }
    }

    static void draw(GuiGraphicsExtractor g, int x, int y, int right, int bottom, int which) {
        Identifier id;
        if (which == 4) {
            if (!Files.isRegularFile(CUSTOM)) return;
            if (!loaded)
                try (var in = Files.newInputStream(CUSTOM)) {
                    Minecraft.getInstance()
                            .getTextureManager()
                            .register(
                                    CUSTOM_ID,
                                    new DynamicTexture(
                                            () -> "Studio background", NativeImage.read(in)));
                    loaded = true;
                } catch (IOException ex) {
                    return;
                }
            id = CUSTOM_ID;
        } else
            id =
                    Identifier.fromNamespaceAndPath(
                            "resourcebuilder", "textures/gui/background_" + which + ".png");
        g.blit(id, x, y, right, bottom, 0, 1, 0, 1);
        g.fill(
                x,
                y,
                right,
                bottom,
                ((int) (255 * de.resourcebuilder.StudioSettings.S.darken) << 24));
    }
}
