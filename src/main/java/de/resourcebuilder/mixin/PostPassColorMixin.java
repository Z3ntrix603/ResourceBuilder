package de.resourcebuilder.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import de.resourcebuilder.StudioSettings;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostPass.class)
abstract class PostPassColorMixin {
  @Shadow @Final private Map<String, GpuBuffer> customUniforms;

  @Inject(method = "addToFrame", at = @At("HEAD"))
  private void rb$colorParameters(CallbackInfo ci) {
    GpuBuffer buffer = customUniforms.get("ColorConfig");
    if (buffer == null) return;
    ByteBuffer values = rb$values();
    if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
      GpuBuffer replacement =
          RenderSystem.getDevice()
              .createBuffer(
                  () -> "Resource Builder color settings",
                  GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                  values);
      customUniforms.put("ColorConfig", replacement);
      buffer.close();
      return;
    }
    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), values);
  }

  private static ByteBuffer rb$values() {
    var profile = StudioSettings.S.visual.normalized();
    ByteBuffer values = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
    values
        .putFloat((float) profile.saturation)
        .putFloat((float) profile.contrast)
        .putFloat((float) profile.brightness)
        .putFloat((float) profile.gamma)
        .putFloat((float) profile.hue)
        .putFloat(0)
        .putFloat(0)
        .putFloat(0)
        .flip();
    return values;
  }
}
