package pl.enemyping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.enemyping.command.EnemyPingCommand;
import pl.enemyping.config.EnemyPingConfig;
import pl.enemyping.config.PingPosition;
import pl.enemyping.gui.EnemyPingConfigScreen;
import pl.enemyping.hud.PersonalHud;
import pl.enemyping.lang.ModLang;

public class EnemyPing implements ClientModInitializer {
        public static final String MOD_ID = "enemyping";
        public static final Logger LOGGER = LoggerFactory.getLogger("Enemy Ping");

        public static KeyMapping openMenuKey;
        private static final KeyMapping.Category CATEGORY =
                        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        private static KeyMapping toggleModKey;
        private static KeyMapping toggleNametagKey;
        private static KeyMapping toggleHudKey;
        private static KeyMapping cyclePositionKey;

        @Override
        public void onInitializeClient() {
                EnemyPingConfig.load();

                openMenuKey = register("key.enemyping.open", GLFW.GLFW_KEY_P);
                // toggle keys start unbound - assign them in the Controls menu
                toggleModKey = register("key.enemyping.togglemod", GLFW.GLFW_KEY_UNKNOWN);
                toggleNametagKey = register("key.enemyping.togglenametag", GLFW.GLFW_KEY_UNKNOWN);
                toggleHudKey = register("key.enemyping.togglehud", GLFW.GLFW_KEY_UNKNOWN);
                cyclePositionKey = register("key.enemyping.cycleposition", GLFW.GLFW_KEY_UNKNOWN);

                ClientTickEvents.END_CLIENT_TICK.register(client -> {
                        while (openMenuKey.consumeClick()) {
                                client.setScreen(new EnemyPingConfigScreen(client.screen));
                        }
                        EnemyPingConfig cfg = EnemyPingConfig.get();
                        while (toggleModKey.consumeClick()) {
                                cfg.general.masterEnabled = !cfg.general.masterEnabled;
                                EnemyPingConfig.save();
                                feedback(ModLang.tr("menu.master").append(": ")
                                                .append(ModLang.tr(cfg.general.masterEnabled ? "menu.on" : "menu.off")));
                        }
                        while (toggleNametagKey.consumeClick()) {
                                cfg.nametag.enabled = !cfg.nametag.enabled;
                                EnemyPingConfig.save();
                                feedback(ModLang.tr("nametag.enabled").append(": ")
                                                .append(ModLang.tr(cfg.nametag.enabled ? "menu.on" : "menu.off")));
                        }
                        while (toggleHudKey.consumeClick()) {
                                cfg.personal.enabled = !cfg.personal.enabled;
                                EnemyPingConfig.save();
                                feedback(ModLang.tr("personal.enabled").append(": ")
                                                .append(ModLang.tr(cfg.personal.enabled ? "menu.on" : "menu.off")));
                        }
                        while (cyclePositionKey.consumeClick()) {
                                PingPosition[] values = PingPosition.values();
                                cfg.nametag.position = values[(cfg.nametag.position.ordinal() + 1) % values.length];
                                EnemyPingConfig.save();
                                feedback(ModLang.tr("menu.position").append(": ")
                                                .append(ModLang.positionName(cfg.nametag.position)));
                        }
                });

                EnemyPingCommand.register();
                PersonalHud.register();

                LOGGER.info("Enemy Ping 1.0.0 loaded - press P for settings");
        }

        private static KeyMapping register(String name, int defaultKey) {
                return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                                name,
                                InputConstants.Type.KEYSYM,
                                defaultKey,
                                CATEGORY
                ));
        }

        /** Short on/off feedback for the toggle keybinds (action bar, not chat). */
        private static void feedback(Component message) {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                        client.player.displayClientMessage(message, true);
                }
        }
}
