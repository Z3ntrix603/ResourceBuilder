package verify;

import de.resourcebuilder.StudioSettings;
import de.resourcebuilder.profile.ShareCode;
import de.resourcebuilder.profile.VisualProfile;

final class ProfileSmoke {
  static void run() {
    defaultsAreSafe();
    valuesRoundTrip();
    invalidInputIsAtomic();
    oversizedInputIsRejected();
  }

  private static void defaultsAreSafe() {
    VisualProfile defaults = new VisualProfile().normalized();
    if (defaults.saturation != 1 || defaults.particleAmount != 1 || defaults.fireOpacity != .9)
      throw new AssertionError("Unsafe v3 defaults");
  }

  private static void valuesRoundTrip() {
    StudioSettings source = new StudioSettings();
    source.visual.saturation = 1.37;
    source.visual.critColor = 0x12abef;
    source.visual.particleAmount = 2.25;
    StudioSettings target = new StudioSettings();
    if (!ShareCode.apply(ShareCode.encode(source), target))
      throw new AssertionError("Valid share code rejected");
    if (target.visual.saturation != 1.37
        || target.visual.critColor != 0x12abef
        || target.visual.particleAmount != 2.25)
      throw new AssertionError("Share code changed values");
  }

  private static void invalidInputIsAtomic() {
    StudioSettings target = new StudioSettings();
    target.visual.saturation = .75;
    if (ShareCode.apply("RB3:not-base64", target))
      throw new AssertionError("Malformed share code accepted");
    if (target.visual.saturation != .75)
      throw new AssertionError("Malformed share code mutated settings");
  }

  private static void oversizedInputIsRejected() {
    StudioSettings target = new StudioSettings();
    if (ShareCode.apply("RB3:" + "A".repeat(70_000), target))
      throw new AssertionError("Oversized share code accepted");
  }
}
