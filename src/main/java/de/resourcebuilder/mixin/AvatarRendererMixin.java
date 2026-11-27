package de.resourcebuilder.mixin;

import de.resourcebuilder.render.EquipmentSetLayer;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
  @Inject(method = "<init>", at = @At("TAIL"))
  private void rb$addEquipmentSetLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
    @SuppressWarnings("unchecked")
    var parent = (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this;
    ((LivingRendererAccess) this).rb$addLayer(new EquipmentSetLayer(parent));
  }
}
