package de.resourcebuilder.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.resourcebuilder.equipment.EquipmentSetManager;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public final class EquipmentSetLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
  public EquipmentSetLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
    super(parent);
  }

  @Override
  public void submit(PoseStack pose, SubmitNodeCollector out, int light, AvatarRenderState state,
      float limbAngle, float limbDistance) {
    var definition = EquipmentSetManager.active();
    if (definition.isOff() || state.isInvisible || state.distanceToCameraSq > 4096) return;
    if (state.headEquipment.isEmpty() && state.chestEquipment.isEmpty()
        && state.legsEquipment.isEmpty() && state.feetEquipment.isEmpty()) return;
    var model = EquipmentSetModel.forDefinition(definition);
    model.setupAnim(state);
    model.setEquipmentVisibility(state);
    renderColoredCutoutModel(model, Identifier.parse(definition.armorTexture()), pose, out, light,
        state, 0xff000000 | definition.primaryColor(), 0);
  }
}
