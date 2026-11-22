package verify;

import de.resourcebuilder.gui.VisualsScreen;

final class VisualsScreenSmoke {
  static void run() {
    if (!VisualsScreen.PAGE_LABELS.equals(
        java.util.List.of("Global Color", "Particles", "Fire", "Glint", "Sky", "Menu")))
      throw new AssertionError("Visual editor pages changed");
  }
}
