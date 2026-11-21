package de.resourcebuilder.mixin;

import static de.resourcebuilder.StudioSettings.S;

import com.mojang.blaze3d.vertex.PoseStack;

import de.resourcebuilder.GroundScale;
import de.resourcebuilder.ResourceBuilderMod;
import de.resourcebuilder.pack.LivePackManager;

import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Camera.class)
abstract class ZoomCameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void studioZoom(float delta, CallbackInfoReturnable<Float> result) {
        if (ResourceBuilderMod.zoomFactor != 1)
            result.setReturnValue(
                    (float) (result.getReturnValue() / ResourceBuilderMod.zoomFactor));
    }
}

@Mixin(MouseHandler.class)
abstract class ZoomWheelMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void studioWheel(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (ResourceBuilderMod.isZoomActive() && S.scrollZoom && vertical != 0) {
            S.zoom = Math.clamp(S.zoom * Math.pow(1.15, Math.signum(vertical)), 1, 12);
            ci.cancel();
        }
    }
}

@Mixin(LightmapRenderStateExtractor.class)
abstract class LightMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void studioLight(LightmapRenderState state, float delta, CallbackInfo ci) {
        if (S.fullbright) {
            state.nightVisionEffectIntensity = 1;
            state.brightness = 1;
            state.needsUpdate = true;
        }
    }
}

@Mixin(WeatherEffectRenderer.class)
abstract class WeatherMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void studioWeather(
            ClientLevel level, float delta, Vec3 eye, WeatherRenderState state, CallbackInfo ci) {
        if (S.clearWeather) {
            state.reset();
            ci.cancel();
        }
    }
}

@Mixin(SkyRenderer.class)
abstract class SkyMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void studioSky(
            ClientLevel level, float delta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (S.clearWeather) state.rainBrightness = 1;
        if (S.skyTime != 0) {
            boolean night = S.skyTime == 2;
            state.sunAngle = night ? (float) Math.PI : 0;
            state.moonAngle = state.sunAngle;
            state.starAngle = state.sunAngle;
            state.starBrightness = night ? 1 : 0;
            state.skyColor = night ? 0xff080d24 : 0xff83b5ff;
            state.sunriseAndSunsetColor = 0;
        }
        int tint = LivePackManager.getInstance().skyTint();
        if (tint != 0)
            state.skyColor =
                    net.minecraft.util.ARGB.srgbLerp(.48f, state.skyColor, 0xff000000 | tint);
    }
}

@Mixin(Hud.class)
abstract class ReticleMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"))
    private void studioCrosshair(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        var pose = g.pose();
        pose.pushMatrix();
        float x = g.guiWidth() / 2f, y = g.guiHeight() / 2f, s = (float) S.crosshairSize;
        pose.translate(x, y).scale(s, s).translate(-x, -y);
    }

    @Redirect(
            method = "extractCrosshair",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                            ordinal = 0))
    private void studioReticleColor(
            GuiGraphicsExtractor g,
            com.mojang.blaze3d.pipeline.RenderPipeline pipe,
            net.minecraft.resources.Identifier sprite,
            int x,
            int y,
            int w,
            int h) {
        boolean target =
                S.targetIndicator
                        && Minecraft.getInstance().crosshairPickEntity instanceof LivingEntity;
        if (S.crosshairColor == 0 && !target) {
            g.blitSprite(pipe, sprite, x, y, w, h);
            return;
        }
        int rgb = target ? 0xff6868 : de.resourcebuilder.StudioSettings.HIT[S.crosshairColor];
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h, 0xff000000 | rgb);
    }

    @Inject(method = "extractCrosshair", at = @At("RETURN"))
    private void studioCrosshairEnd(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
        g.pose().popMatrix();
    }
}

@Mixin(net.minecraft.client.model.object.crystal.EndCrystalModel.class)
abstract class CrystalMixin {
    @Shadow @Final public net.minecraft.client.model.geom.ModelPart cube;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void studioCore(net.minecraft.client.model.geom.ModelPart root, CallbackInfo ci) {
        ((de.resourcebuilder.CrystalCore) (Object) cube).rb$markCore();
    }
}

@Mixin(net.minecraft.client.model.geom.ModelPart.class)
abstract class CrystalPartMixin implements de.resourcebuilder.CrystalCore {
    @Unique private boolean rb$core;

    public void rb$markCore() {
        rb$core = true;
    }

    @ModifyVariable(method = "compile", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int studioLight(int light) {
        return rb$core && S.glow > 0
                ? net.minecraft.util.LightCoordsUtil.lightCoordsWithEmission(
                        light, (int) Math.round(S.glow * 15))
                : light;
    }
}

@Mixin(net.minecraft.client.particle.ParticleEngine.class)
abstract class ParticleMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void studioParticle(
            net.minecraft.core.particles.ParticleOptions options,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            CallbackInfoReturnable<net.minecraft.client.particle.Particle> ci) {
        if (!S.hiddenParticles.isEmpty()
                && S.hiddenParticles.contains(
                        BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()).getPath()))
            ci.setReturnValue(null);
    }
}

@Mixin(LivingEntityRenderState.class)
abstract class HurtStateMixin implements de.resourcebuilder.HurtStrength {
    @Unique private float rb$hurt;

    public void rb$setHurt(float value) {
        rb$hurt = value;
    }

    public float rb$hurt() {
        return rb$hurt;
    }
}

@Mixin(LivingEntityRenderer.class)
abstract class HurtMixin {
    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void studioHurt(
            LivingEntity entity, LivingEntityRenderState state, float dt, CallbackInfo ci) {
        float amount =
                state.hasRedOverlay
                        ? (S.fadeHit ? Math.clamp((entity.hurtTime - dt) / 10f, 0, 1) : 1)
                        : 0;
        if (!S.selfHit && entity == Minecraft.getInstance().player) amount = 0;
        ((de.resourcebuilder.HurtStrength) state).rb$setHurt(amount);
    }

    @Inject(method = "getOverlayCoords", at = @At("RETURN"), cancellable = true)
    private static void studioOverlay(
            LivingEntityRenderState state, float white, CallbackInfoReturnable<Integer> result) {
        if (S.hitColor == 0 || !state.hasRedOverlay) return;
        float amount = ((de.resourcebuilder.HurtStrength) state).rb$hurt();
        result.setReturnValue(
                S.hitMode == 2 || amount == 0
                        ? OverlayTexture.pack(white, false)
                        : OverlayTexture.pack(amount, true));
    }
}

@Mixin(net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer.class)
abstract class ArmorTintMixin {
    @ModifyArgs(
            method =
                    "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void studioArmor(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
        if (S.hitColor == 0
                || S.hitMode == 1
                || !(args.get(1) instanceof de.resourcebuilder.HurtStrength hurt)
                || hurt.rb$hurt() == 0) return;
        args.set(
                6,
                net.minecraft.util.ARGB.srgbLerp(
                        (float) (S.hitStrength * hurt.rb$hurt()),
                        (int) args.get(6),
                        0xff000000 | S.hitRgb()));
    }
}

@Mixin(OverlayTexture.class)
abstract class HitMixin {
    @Shadow @Final private DynamicTexture texture;
    @Unique private int lastTint = Integer.MIN_VALUE;

    @Inject(method = "getTextureView", at = @At("HEAD"))
    private void studioTint(
            CallbackInfoReturnable<com.mojang.blaze3d.textures.GpuTextureView> result) {
        int color =
                S.hitColor == 0
                        ? 0xb2ff0000
                        : ((int) ((1 - S.hitStrength) * 255) << 24) | S.hitRgb();
        if (lastTint == color) return;
        lastTint = color;
        var image = texture.getPixels();
        if (image != null) {
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 16; x++)
                    image.setPixel(
                            x,
                            y,
                            S.hitColor == 0
                                    ? color
                                    : ((int) (255 * (1 - S.hitStrength * x / 15d)) << 24)
                                            | (color & 0xffffff));
            texture.upload();
        }
    }
}

@Mixin(ItemInHandRenderer.class)
abstract class HandMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void studioItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack pose,
            SubmitNodeCollector out,
            int light,
            CallbackInfo ci) {
        pose.pushPose();
        if (context.firstPerson()) {
            double direction = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
            double size =
                    S.handSize
                            * S.handItems.getOrDefault(
                                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 1d);
            pose.translate(direction * S.handX, S.handY, S.handZ);
            pose.scale((float) size, (float) size, (float) size);
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void studioEnd(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack pose,
            SubmitNodeCollector out,
            int light,
            CallbackInfo ci) {
        pose.popPose();
    }

    @ModifyVariable(method = "swingArm", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float studioSwing(float progress) {
        progress = (float) Math.min(1, progress * S.swingSpeed);
        return switch (S.swing) {
            case 1 -> progress * progress * (3 - 2 * progress);
            case 2 -> (float) Math.sqrt(progress);
            case 5 -> progress * .35f;
            default -> progress;
        };
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void studioPose(
            float progress, PoseStack pose, int sign, HumanoidArm arm, CallbackInfo ci) {
        float t = (float) Math.sin(Math.PI * Math.min(1, progress * S.swingSpeed));
        if (S.swing == 3) {
            pose.translate(0, 0, -.48f * t);
            ci.cancel();
        }
        if (S.swing == 4) {
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(360 * progress * sign));
            ci.cancel();
        }
    }
}

@Mixin(ItemEntityRenderState.class)
abstract class GroundStateMixin implements GroundScale {
    @Unique private float rb$size = 1;

    public void rb$set(float value) {
        rb$size = value;
    }

    public float rb$get() {
        return rb$size;
    }
}

@Mixin(ItemEntityRenderer.class)
abstract class GroundMixin {
    @Inject(
            method =
                    "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
            at = @At("TAIL"))
    private void studioExtract(
            ItemEntity entity, ItemEntityRenderState state, float dt, CallbackInfo ci) {
        String key = BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).toString();
        ((GroundScale) state).rb$set((float) (S.groundSize * S.groundItems.getOrDefault(key, 1d)));
    }

    @Inject(
            method =
                    "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                            shift = At.Shift.AFTER))
    private void studioScale(
            ItemEntityRenderState state,
            PoseStack pose,
            SubmitNodeCollector out,
            CameraRenderState camera,
            CallbackInfo ci) {
        float size = ((GroundScale) state).rb$get();
        pose.scale(size, size, size);
    }
}

@Mixin(ScreenEffectRenderer.class)
abstract class FireMixin {
    @ModifyConstant(method = "buildFireQuad", constant = @Constant(intValue = -436207617))
    private static int studioOpacity(int color) {
        return ((int) Math.round(S.fireOpacity * 255) << 24) | 0xffffff;
    }

    @Inject(method = "submitFire", at = @At("HEAD"))
    private static void studioFire(
            PoseStack pose, SubmitNodeCollector out, TextureAtlasSprite sprite, CallbackInfo ci) {
        pose.pushPose();
        pose.translate(0, -(1 - S.fireHeight) * .75, 0);
    }

    @Inject(method = "submitFire", at = @At("RETURN"))
    private static void studioFireEnd(
            PoseStack pose, SubmitNodeCollector out, TextureAtlasSprite sprite, CallbackInfo ci) {
        pose.popPose();
    }
}
