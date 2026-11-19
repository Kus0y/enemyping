package pl.enemyping.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import pl.enemyping.gui.EnemyPingConfigScreen;

/**
 * Mod Menu integration - shows the "Configure" button for Enemy Ping.
 */
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return EnemyPingConfigScreen::new;
	}
}
