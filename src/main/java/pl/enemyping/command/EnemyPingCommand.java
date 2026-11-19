package pl.enemyping.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import pl.enemyping.lang.ModLang;
import pl.enemyping.util.PingLookup;
import pl.enemyping.util.PingText;

import java.util.stream.Collectors;

/**
 * /enemyping - shows your own ping
 * /enemyping <nick> - shows the ping of any player on the server
 */
public final class EnemyPingCommand {
	private EnemyPingCommand() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("enemyping")
						.executes(ctx -> showOwnPing(ctx.getSource()))
						.then(ClientCommandManager.argument("nick", StringArgumentType.word())
								.suggests((ctx, builder) -> {
									Minecraft mc = Minecraft.getInstance();
									if (mc.getConnection() == null) {
										return builder.buildFuture();
									}
									return SharedSuggestionProvider.suggest(
											mc.getConnection().getOnlinePlayers().stream()
													.map(info -> info.getProfile().name())
													.collect(Collectors.toList()),
											builder);
								})
								.executes(ctx -> showPlayerPing(ctx.getSource(), StringArgumentType.getString(ctx, "nick")))
						)
		));
	}

	private static int showOwnPing(FabricClientCommandSource source) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			source.sendError(ModLang.tr("command.needworld"));
			return 0;
		}
		Integer latency = PingLookup.latencyOf(mc.player.getUUID());
		if (latency == null) {
			source.sendError(ModLang.tr("command.noping", mc.player.getGameProfile().name()));
			return 0;
		}
		source.sendFeedback(ModLang.tr("command.own").append(PingText.build(latency)));
		return 1;
	}

	private static int showPlayerPing(FabricClientCommandSource source, String nick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) {
			source.sendError(ModLang.tr("command.needworld"));
			return 0;
		}
		PlayerInfo info = mc.getConnection().getPlayerInfoIgnoreCase(nick);
		if (info == null) {
			source.sendError(ModLang.tr("command.notfound", nick));
			return 0;
		}
		Integer latency = PingLookup.latencyOf(info.getProfile().id());
		if (latency == null) {
			source.sendError(ModLang.tr("command.noping", info.getProfile().name()));
			return 0;
		}
		source.sendFeedback(ModLang.tr("command.player", info.getProfile().name()).append(PingText.build(latency)));
		return 1;
	}
}
