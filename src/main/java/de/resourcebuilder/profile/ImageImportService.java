package de.resourcebuilder.profile;

import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;

public final class ImageImportService {
  private static final long MAX_BYTES = 16_000_000L;
  private static final long MAX_PIXELS = 16_777_216L;

  private ImageImportService() {}

  public static Path importPng(Path source, Path directory, String slot) throws IOException {
    if (source == null || !Files.isRegularFile(source) || Files.size(source) > MAX_BYTES)
      throw new IOException("Choose a PNG up to 16 MB");
    if (!source.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
      throw new IOException("Only PNG images are supported");
    var image = ImageIO.read(source.toFile());
    if (image == null || (long) image.getWidth() * image.getHeight() > MAX_PIXELS)
      throw new IOException("Invalid PNG or larger than 16 megapixels");
    String safeSlot = slot == null ? "image" : slot.replaceAll("[^a-z0-9_-]", "");
    if (safeSlot.isBlank()) safeSlot = "image";
    Files.createDirectories(directory);
    Path target = directory.resolve(safeSlot + ".png");
    Path temporary = Files.createTempFile(directory, safeSlot + "-", ".tmp");
    try {
      if (!ImageIO.write(image, "png", temporary.toFile())) throw new IOException("PNG encoder unavailable");
      try {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return target.toAbsolutePath().normalize();
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
