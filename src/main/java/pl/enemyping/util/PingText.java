package pl.enemyping.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import pl.enemyping.config.EnemyPingConfig;

/**
 * Builds the colored ping text (number + optional "ms" unit).
 */
public final class PingText {
        private PingText() {
        }

        public static MutableComponent build(int ping) {
                EnemyPingConfig cfg = EnemyPingConfig.get();
                int color = cfg.colorForPing(ping);
                MutableComponent text = Component.literal(String.valueOf(ping))
                                .withStyle(style -> style.withColor(TextColor.fromRgb(color)));
                if (cfg.general.showMsUnit) {
                        text.append(Component.literal("ms")
                                        .withStyle(style -> style.withColor(TextColor.fromRgb(color))));
                }
                return text;
        }

        /**
         * Like build(), but negative latency (server plugins use it as
         * "no data" on custom TAB rows) renders as a white "--" instead
         * of a misleading 0.
         */
        public static MutableComponent buildWithNoData(int latency) {
                if (latency >= 0) {
                        return build(latency);
                }
                EnemyPingConfig cfg = EnemyPingConfig.get();
                MutableComponent text = Component.literal("--")
                                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFFFF)));
                if (cfg.general.showMsUnit) {
                        text.append(Component.literal("ms")
                                        .withStyle(style -> style.withColor(TextColor.fromRgb(0xFFFFFF))));
                }
                return text;
        }
}
