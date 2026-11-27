package de.resourcebuilder.equipment;

import java.util.List;

public record EquipmentSetDefinition(
    String id,
    String displayName,
    int primaryColor,
    int accentColor,
    String armorTexture,
    String spearModel,
    List<Piece> pieces) {

  public enum Anchor { HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }

  public record Cube(float x, float y, float z, float width, float height, float depth, float inflate, int u, int v) {}

  public record Piece(
      String name,
      Anchor anchor,
      float pivotX,
      float pivotY,
      float pivotZ,
      float xRot,
      float yRot,
      float zRot,
      Cube cube) {}

  public EquipmentSetDefinition {
    pieces = List.copyOf(pieces);
  }

  public boolean isOff() {
    return "off".equals(id);
  }
}
