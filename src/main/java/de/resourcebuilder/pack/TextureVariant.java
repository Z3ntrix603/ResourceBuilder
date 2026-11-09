package de.resourcebuilder.pack;

import de.resourcebuilder.ResourceBuilderMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TextureVariant {

    private static final AtomicInteger ID_GEN = new AtomicInteger(100);

    private final String packName;
    private final byte[] pngData;
    private final byte[] mcmetaData;
    private final Map<String, byte[]> additionalFiles;
    private final boolean isDefault;
    private final boolean animated;
    private Identifier textureId;
    private boolean loadAttempted = false;
    private boolean loadFailed = false;

    public TextureVariant(String packName, byte[] pngData, boolean isDefault) {
        this(packName, pngData, null, Collections.emptyMap(), isDefault);
    }

    public TextureVariant(String packName, byte[] pngData, byte[] mcmetaData, boolean isDefault) {
        this(packName, pngData, mcmetaData, Collections.emptyMap(), isDefault);
    }

    public TextureVariant(String packName, byte[] pngData, byte[] mcmetaData, Map<String, byte[]> additionalFiles, boolean isDefault) {
        this.packName = packName;
        this.pngData = pngData;
        this.mcmetaData = mcmetaData;
        this.additionalFiles = additionalFiles != null ? additionalFiles : Collections.emptyMap();
        this.isDefault = isDefault;
        this.animated = (mcmetaData != null && mcmetaData.length > 0) || isPngAnimated(pngData);
    }

    public static TextureVariant createDefault() {
        return new TextureVariant("Default", null, null, Collections.emptyMap(), true);
    }

    public static boolean isPngBytes(byte[] data) {
        if (data == null || data.length < 8) return false;
        return (data[0] & 0xFF) == 0x89 &&
               (data[1] & 0xFF) == 0x50 &&
               (data[2] & 0xFF) == 0x4E &&
               (data[3] & 0xFF) == 0x47 &&
               (data[4] & 0xFF) == 0x0D &&
               (data[5] & 0xFF) == 0x0A &&
               (data[6] & 0xFF) == 0x1A &&
               (data[7] & 0xFF) == 0x0A;
    }

    public static boolean isPngAnimated(byte[] data) {
        if (data == null || data.length < 24) return false;
        if ((data[0] & 0xFF) != 0x89 || data[1] != 0x50 || data[2] != 0x4E || data[3] != 0x47) return false;
        int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16) | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
        int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16) | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
        return width > 0 && height > width;
    }

    public static boolean isImageVisible(byte[] data) {
        if (data == null || data.length < 32) return false;
        try (NativeImage image = NativeImage.read(data)) {
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) return false;
            int w = image.getWidth();
            int h = image.getHeight();
            int stepX = Math.max(1, w / 16);
            int stepY = Math.max(1, h / 16);
            int visiblePixels = 0;
            for (int x = 0; x < w; x += stepX) {
                for (int y = 0; y < h; y += stepY) {
                    int pixel = image.getPixel(x, y);
                    int a = (pixel >> 24) & 0xFF;
                    if (a > 25) {
                        visiblePixels++;
                        if (visiblePixels >= 4) return true;
                    }
                }
            }
            return visiblePixels >= 4;
        } catch (Throwable t) {
            return false;
        }
    }

    public String getPackName() {
        return packName;
    }

    public byte[] getPngData() {
        return pngData;
    }

    public byte[] getMcmetaData() {
        return mcmetaData;
    }

    public Map<String, byte[]> getAdditionalFiles() {
        return additionalFiles;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isAnimated() {
        return animated;
    }

    public boolean isValid() {
        return isDefault || (!additionalFiles.isEmpty()) || (!loadFailed && pngData != null && pngData.length > 32);
    }

    public Identifier getTextureId() {
        if (textureId != null) {
            return textureId;
        }
        if (isDefault || pngData == null || loadAttempted) {
            return null;
        }
        loadAttempted = true;
        try {
            NativeImage rawImage;
            try {
                rawImage = NativeImage.read(pngData);
            } catch (Throwable t) {
                rawImage = NativeImage.read(new ByteArrayInputStream(pngData));
            }
            if (rawImage == null) {
                loadFailed = true;
                return null;
            }

            NativeImage imageToUpload;
            if (rawImage.getHeight() > rawImage.getWidth() && rawImage.getWidth() > 0) {
                int size = rawImage.getWidth();
                imageToUpload = new NativeImage(size, size, false);
                rawImage.copyRect(imageToUpload, 0, 0, 0, 0, size, size, false, false);
                rawImage.close();
            } else {
                imageToUpload = rawImage;
            }

            int id = ID_GEN.incrementAndGet();
            DynamicTexture dynamicTexture = new DynamicTexture(() -> "rb_tex_" + id, imageToUpload);
            dynamicTexture.upload();

            textureId = Identifier.fromNamespaceAndPath(ResourceBuilderMod.MOD_ID, "tex_" + id);
            Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
            return textureId;
        } catch (Throwable t) {
            loadFailed = true;
            ResourceBuilderMod.LOGGER.error("Failed to load texture for pack '{}': {}", packName, t.getMessage());
            return null;
        }
    }
}
