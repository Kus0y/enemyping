package pl.enemyping.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.enemyping.config.EnemyPingConfig;
import pl.enemyping.config.PingPosition;
import pl.enemyping.util.PingLookup;
import pl.enemyping.util.PingText;

/**
 * Single-line modes: the ping is composed directly into the name tag text.
 * Handles: RIGHT ("Nick 67ms"), LEFT ("67ms Nick"), ONLY ("67ms").
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

        @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
        private void enemyping$singleLine(Entity entity, CallbackInfoReturnable<Component> cir) {
                if (!(entity instanceof Player player)) {
                        return;
                }
                EnemyPingConfig cfg = EnemyPingConfig.get();
                if (!cfg.nametagActive()) {
                        return;
                }
                PingPosition pos = cfg.nametag.position;
                if (pos != PingPosition.LEFT && pos != PingPosition.RIGHT && pos != PingPosition.ONLY) {
                        return;
                }
                Integer latency = PingLookup.latencyOf(player);
                if (latency == null) {
                        return;
                }
                // distance limit (0 = off): camera -> player, squared
                int limit = cfg.nametag.distanceLimit;
                if (limit > 0) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
                                double dsq = mc.gameRenderer.getMainCamera().position()
                                                .distanceToSqr(player.position());
                                if (dsq > (double) limit * limit) {
                                        return; // keep the vanilla name, hide the ping
                                }
                        }
                }

                MutableComponent ping = PingText.build(latency);
                if (pos == PingPosition.ONLY) {
                        cir.setReturnValue(ping);
                        return;
                }
                Component name = cir.getReturnValue();
                if (name == null) {
                        cir.setReturnValue(ping);
                        return;
                }
                MutableComponent combined = Component.empty();
                if (pos == PingPosition.LEFT) {
                        combined.append(ping).append(Component.literal(" ")).append(name);
                } else {
                        combined.append(name).append(Component.literal(" ")).append(ping);
                }
                cir.setReturnValue(combined);
        }
}
