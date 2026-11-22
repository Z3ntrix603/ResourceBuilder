package verify;

import de.resourcebuilder.profile.ImageImportService;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public final class ImageImportSmoke {
  public static void run() {
    try {
      var dir = Files.createTempDirectory("rb-image-import");
      var source = dir.resolve("sky.png");
      ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB), "png", source.toFile());
      var imported = ImageImportService.importPng(source, dir.resolve("imports"), "sky");
      if (!Files.isRegularFile(imported) || !imported.getFileName().toString().equals("sky.png"))
        throw new AssertionError("PNG was not imported");
      var bad = dir.resolve("bad.jpg");
      Files.writeString(bad, "not an image");
      try {
        ImageImportService.importPng(bad, dir.resolve("imports"), "sky");
        throw new AssertionError("Non-PNG import was accepted");
      } catch (java.io.IOException expected) {
        if (!Files.isRegularFile(imported)) throw new AssertionError("Failed import removed old image");
      }
    } catch (Exception error) {
      throw new AssertionError("Image import smoke failed", error);
    }
  }
}
