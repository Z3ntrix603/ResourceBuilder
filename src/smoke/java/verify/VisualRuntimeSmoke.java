package verify;

import de.resourcebuilder.profile.VisualProfile;
import de.resourcebuilder.render.VisualRuntime;

final class VisualRuntimeSmoke {
  static void run() {
    disabledColorIsIdentity();
    zeroSaturationIsGrayscale();
    particleColorsAreMapped();
    particleCopiesAreBounded();
  }

  private static void disabledColorIsIdentity() {
    var profile = new VisualProfile();
    if (VisualRuntime.transformRgb(0x2a80e0, profile) != 0x2a80e0)
      throw new AssertionError("Disabled color filter changed RGB");
  }

  private static void zeroSaturationIsGrayscale() {
    var profile = new VisualProfile();
    profile.colorEnabled = true;
    profile.saturation = 0;
    int rgb = VisualRuntime.transformRgb(0xff0000, profile);
    if ((rgb >> 16 & 255) != (rgb >> 8 & 255) || (rgb >> 8 & 255) != (rgb & 255))
      throw new AssertionError("Zero saturation did not produce grayscale");
  }

  private static void particleColorsAreMapped() {
    var profile = new VisualProfile();
    profile.critColor = 0x123456;
    profile.totemColor = 0xabcdef;
    if (VisualRuntime.particleColor("crit", profile) != 0x123456
        || VisualRuntime.particleColor("totem_of_undying", profile) != 0xabcdef)
      throw new AssertionError("Particle colors were not mapped");
  }

  private static void particleCopiesAreBounded() {
    if (VisualRuntime.extraCopies(2.25, .5) != 1
        || VisualRuntime.extraCopies(2.25, .1) != 2
        || VisualRuntime.extraCopies(99, 0) != 2)
      throw new AssertionError("Particle multiplier bounds failed");
  }
}
