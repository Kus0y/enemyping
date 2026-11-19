package pl.enemyping.mixin;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.enemyping.config.EnemyPingConfig;
import pl.enemyping.config.PingPosition;
import pl.enemyping.util.PingLookup;
import pl.enemyping.util.PingText;

import net.minecraft.world.phys.Vec3;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.MutableComponent;

/**
 * Two-line modes: ABOVE (ping above the nick) and BELOW (ping below the nick).
 * The extra line is submitted exactly like vanilla's scoreboard score line
 * (same spacing: 9 * 1.15 * 0.025 units per line).
 * <p>
 * distanceLimit > 0 hides the ping beyond that many blocks (camera -> player).
 * background = false submits a plain text line without the vanilla plate
 * (and without the see-through ghost) - same transform as vanilla's
 * NameTagFeatureRenderer.Storage.add, but with backgroundColor = 0.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

        @Inject(method = "submitNameTag", at = @At("TAIL"))
        private void enemyping$extraLine(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
                EnemyPingConfig cfg = EnemyPingConfig.get();
                if (!cfg.nametagActive()) {
                        return;
                }
                PingPosition pos = cfg.nametag.position;
                if (pos != PingPosition.ABOVE && pos != PingPosition.BELOW) {
                        return;
                }
                // respect vanilla hiding of the name tag (spectators, hidden custom names, etc.)
                if (state.nameTag == null) {
                        return;
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                        return;
                }
                Entity entity = mc.level.getEntity(state.id);
                if (!(entity instanceof Player player)) {
                        return;
                }
                Integer latency = PingLookup.latencyOf(player);
                if (latency == null) {
                        return;
                }
                // distance limit (0 = off): camera -> player, squared
                int limit = cfg.nametag.distanceLimit;
                if (limit > 0 && state.distanceToCameraSq > (double) limit * limit) {
                        return;
                }

                // vanilla line spacing for the score line: 9.0F * 1.15F * 0.025F
                float line = 9.0F * 1.15F * 0.025F;
                // if a scoreboard score line is shown, the nick itself is shifted one line up
                float delta = (state.scoreText != null ? line : 0.0F) + (pos == PingPosition.ABOVE ? line : -line);

                poseStack.pushPose();
                poseStack.translate(0.0F, delta, 0.0F);
                if (cfg.nametag.background) {
                        collector.submitNameTag(
                                        poseStack,
                                        state.nameTagAttachment,
                                        0,
                                        PingText.build(latency),
                                        !state.isDiscrete,
                                        state.lightCoords,
                                        state.distanceToCameraSq,
                                        camera
                        );
                } else {
                        // plain text, no plate, no see-through ghost
                        Vec3 attachment = state.nameTagAttachment;
                        if (attachment != null) {
                                MutableComponent text = PingText.build(latency);
                                poseStack.translate(attachment.x, attachment.y + 0.5, attachment.z);
                                poseStack.mulPose(camera.orientation);
                                poseStack.scale(0.025F, -0.025F, 0.025F);
                                collector.submitText(
                                                poseStack,
                                                -Minecraft.getInstance().font.width(text) / 2.0F,
                                                0.0F,
                                                text.getVisualOrderText(),
                                                false,
                                                Font.DisplayMode.NORMAL,
                                                LightTexture.lightCoordsWithEmission(state.lightCoords, 2),
                                                -1,
                                                0,
                                                0
                                );
                        }
                }
                poseStack.popPose();
        }
}
