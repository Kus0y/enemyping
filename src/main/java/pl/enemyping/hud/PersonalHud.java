package pl.enemyping.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.joml.Matrix3x2fStack;
import pl.enemyping.EnemyPing;
import pl.enemyping.config.EnemyPingConfig;

/**
 * PERSONAL widget: your own ping rendered anywhere on the screen
 * (position in % of the screen, adjustable text scale).
 */
public final class PersonalHud {
        private PersonalHud() {
        }

        public static void register() {
                HudElementRegistry.addLast(
                                Identifier.fromNamespaceAndPath(EnemyPing.MOD_ID, "personal_ping"),
                                PersonalHud::render);
        }

        private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
                EnemyPingConfig cfg = EnemyPingConfig.get();
                if (!cfg.personalActive()) {
                        return;
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.getConnection() == null || mc.options.hideGui) {
                        return;
                }
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info == null) {
                        return;
                }
                int latency = info.getLatency();

                MutableComponent text;
                if (latency < 0) {
                        text = Component.literal("--").withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        if (cfg.general.showMsUnit) {
                                text.append(Component.literal("ms")
                                                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFFFFFF))));
                        }
                } else {
                        int color = cfg.colorForPing(latency);
                        text = Component.literal(String.valueOf(latency))
                                        .withStyle(style -> style.withColor(TextColor.fromRgb(color)));
                        if (cfg.general.showMsUnit) {
                                text.append(Component.literal("ms")
                                                .withStyle(style -> style.withColor(TextColor.fromRgb(color))));
                        }
                }

                float scale = cfg.personal.scalePercent / 100.0F;
                int x = Math.round(graphics.guiWidth() * cfg.personal.xPercent / 100.0F);
                int y = Math.round(graphics.guiHeight() * cfg.personal.yPercent / 100.0F);

                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(x, y);
                pose.scale(scale, scale);
                graphics.drawString(mc.font, text, 0, 0, 0xFFFFFFFF);
                pose.popMatrix();
        }
}
