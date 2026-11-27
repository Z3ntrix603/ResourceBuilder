package de.resourcebuilder.render;

import de.resourcebuilder.equipment.EquipmentSetDefinition;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class EquipmentSetModel extends HumanoidModel<AvatarRenderState> {
  private static final Map<String, EquipmentSetModel> CACHE = new ConcurrentHashMap<>();

  private EquipmentSetModel(EquipmentSetDefinition definition) {
    super(bake(definition));
  }

  public static EquipmentSetModel forDefinition(EquipmentSetDefinition definition) {
    if (definition == null || definition.isOff()) throw new IllegalArgumentException("Set is off");
    return CACHE.computeIfAbsent(definition.id(), ignored -> new EquipmentSetModel(definition));
  }

  public void setEquipmentVisibility(AvatarRenderState state) {
    head.visible = !state.headEquipment.isEmpty();
    hat.visible = false;
    body.visible = leftArm.visible = rightArm.visible = !state.chestEquipment.isEmpty();
    boolean legs = !state.legsEquipment.isEmpty() || !state.feetEquipment.isEmpty();
    leftLeg.visible = rightLeg.visible = legs;
  }

  private static net.minecraft.client.model.geom.ModelPart bake(EquipmentSetDefinition definition) {
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    addAnchor(root, "head", 0, 0, 0);
    root.getChild("head").addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
    addAnchor(root, "body", 0, 0, 0);
    addAnchor(root, "right_arm", -5, 2, 0);
    addAnchor(root, "left_arm", 5, 2, 0);
    addAnchor(root, "right_leg", -1.9f, 12, 0);
    addAnchor(root, "left_leg", 1.9f, 12, 0);
    for (var piece : definition.pieces()) {
      PartDefinition anchor = root.getChild(anchorName(piece.anchor()));
      var cube = piece.cube();
      var builder = CubeListBuilder.create().texOffs(cube.u(), cube.v())
          .addBox(cube.x(), cube.y(), cube.z(), cube.width(), cube.height(), cube.depth(),
              new CubeDeformation(cube.inflate()));
      anchor.addOrReplaceChild(piece.name(), builder,
          PartPose.offsetAndRotation(piece.pivotX(), piece.pivotY(), piece.pivotZ(),
              piece.xRot(), piece.yRot(), piece.zRot()));
    }
    return root.bake(64, 32);
  }

  private static void addAnchor(PartDefinition root, String name, float x, float y, float z) {
    root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, y, z));
  }

  private static String anchorName(EquipmentSetDefinition.Anchor anchor) {
    return switch (anchor) {
      case HEAD -> "head";
      case BODY -> "body";
      case LEFT_ARM -> "left_arm";
      case RIGHT_ARM -> "right_arm";
      case LEFT_LEG -> "left_leg";
      case RIGHT_LEG -> "right_leg";
    };
  }
}
