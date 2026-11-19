package pl.enemyping.util;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Reads the latency (ping) the server reports for a player -
 * the same value the Tab list shows.
 */
public final class PingLookup {
        private PingLookup() {
        }

        public static Integer latencyOf(UUID uuid) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() == null || uuid == null) {
                        return null;
                }
                PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
                return info == null ? null : info.getLatency();
        }

        public static Integer latencyOf(net.minecraft.world.entity.player.Player player) {
                return player == null ? null : latencyOf(player.getUUID());
        }

        /** Case-insensitive nick lookup from the TAB list (config screen box). */
        public static Integer latencyOfName(String name) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() == null || name == null || name.isBlank()) {
                        return null;
                }
                PlayerInfo info = mc.getConnection().getPlayerInfoIgnoreCase(name);
                return info == null ? null : info.getLatency();
        }
}
