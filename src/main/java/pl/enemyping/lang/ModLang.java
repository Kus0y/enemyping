package pl.enemyping.lang;

import pl.enemyping.config.EnemyPingConfig;
import pl.enemyping.config.PingPosition;

import java.util.Locale;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

/**
 * Own translations with an override option (auto / pl / en),
 * independent from the vanilla language files.
 */
public final class ModLang {
        private static final Map<String, String> EN = Map.ofEntries(
                        Map.entry("menu.title", "Enemy Ping - Settings"),
                        Map.entry("menu.on", "ON"),
                        Map.entry("menu.off", "OFF"),
                        Map.entry("menu.master", "Mod enabled"),
                        Map.entry("menu.language", "Language"),
                        Map.entry("lang.auto", "Auto"),
                        Map.entry("menu.ranges", "Color ranges (min - max):"),
                        Map.entry("menu.ranges.add", "+ Add range"),
                        Map.entry("menu.done", "Done"),
                        // tabs (screen categories)
                        Map.entry("tab.general", "General"),
                        Map.entry("tab.nametag", "Player"),
                        Map.entry("tab.hud", "HUD"),
                        Map.entry("menu.hint", "Changes save automatically"),
                        // nametag tab
                        Map.entry("nametag.enabled", "Ping on Player Head"),
                        Map.entry("menu.position", "Ping position"),
                        Map.entry("pos.right", "Right of nick"),
                        Map.entry("pos.left", "Left of nick"),
                        Map.entry("pos.above", "Above nick"),
                        Map.entry("pos.below", "Below nick"),
                        Map.entry("pos.only", "Ping only"),
                        Map.entry("menu.msunit", "Show ms unit"),
                        Map.entry("menu.outofrange", "Out of range color"),
                        Map.entry("nametag.background", "Ping background"),
                        Map.entry("nametag.distance", "Render distance"),
                        Map.entry("distance.unlimited", "No limit"),
                        Map.entry("nametag.check.nickhint", "Type a nick..."),
                        Map.entry("nametag.check.notfound", "Player not found"),
                        Map.entry("nametag.check.hint", "There's also a command: /enemyping <nick>"),
                        // personal tab
                        Map.entry("personal.enabled", "On-screen ping"),
                        Map.entry("personal.x", "Position X"),
                        Map.entry("personal.y", "Position Y"),
                        Map.entry("personal.scale", "Size"),
                        // color picker
                        Map.entry("picker.title", "Pick a color"),
                        Map.entry("picker.hex", "Hex (e.g. #55FF55)"),
                        Map.entry("picker.red", "Red"),
                        Map.entry("picker.green", "Green"),
                        Map.entry("picker.blue", "Blue"),
                        Map.entry("picker.cancel", "Cancel"),
                        // commands
                        Map.entry("command.own", "Your ping: "),
                        Map.entry("command.player", "Ping of {0}: "),
                        Map.entry("command.notfound", "Player not found: {0}"),
                        Map.entry("command.noping", "{0} has no ping data yet"),
                        Map.entry("command.needworld", "Join a world or server first")
        );

        private static final Map<String, String> PL = Map.ofEntries(
                        Map.entry("menu.title", "Enemy Ping - Ustawienia"),
                        Map.entry("menu.on", "WŁ."),
                        Map.entry("menu.off", "WYŁ."),
                        Map.entry("menu.master", "Mod włączony"),
                        Map.entry("menu.language", "Język"),
                        Map.entry("lang.auto", "Auto"),
                        Map.entry("menu.ranges", "Zakresy kolorów (od - do):"),
                        Map.entry("menu.ranges.add", "+ Dodaj zakres"),
                        Map.entry("menu.done", "Gotowe"),
                        // zakladki (kategorie ekranu)
                        Map.entry("tab.general", "Ogólne"),
                        Map.entry("tab.nametag", "Gracz"),
                        Map.entry("tab.hud", "HUD"),
                        Map.entry("menu.hint", "Zmiany zapisują się automatycznie"),
                        // nametag
                        Map.entry("nametag.enabled", "Ping nad głową gracza"),
                        Map.entry("menu.position", "Pozycja pingu"),
                        Map.entry("pos.right", "Po prawo"),
                        Map.entry("pos.left", "Po lewo"),
                        Map.entry("pos.above", "Nad nickiem"),
                        Map.entry("pos.below", "Pod nickiem"),
                        Map.entry("pos.only", "Tylko ping"),
                        Map.entry("menu.msunit", "Jednostka ms"),
                        Map.entry("menu.outofrange", "Kolor poza zakresami"),
                        Map.entry("nametag.background", "Tło za pingiem"),
                        Map.entry("nametag.distance", "Limit dystansu"),
                        Map.entry("distance.unlimited", "Bez limitu"),
                        Map.entry("nametag.check.nickhint", "Wpisz nick..."),
                        Map.entry("nametag.check.notfound", "Nie ma takiego gracza"),
                        Map.entry("nametag.check.hint", "Jest też komenda: /enemyping <nick>"),
                        // personal
                        Map.entry("personal.enabled", "Ping na ekranie"),
                        Map.entry("personal.x", "Pozycja X"),
                        Map.entry("personal.y", "Pozycja Y"),
                        Map.entry("personal.scale", "Rozmiar"),
                        // color picker
                        Map.entry("picker.title", "Wybierz kolor"),
                        Map.entry("picker.hex", "Hex (np. #55FF55)"),
                        Map.entry("picker.red", "Czerwony"),
                        Map.entry("picker.green", "Zielony"),
                        Map.entry("picker.blue", "Niebieski"),
                        Map.entry("picker.cancel", "Anuluj"),
                        // commands
                        Map.entry("command.own", "Twój ping: "),
                        Map.entry("command.player", "Ping gracza {0}: "),
                        Map.entry("command.notfound", "Nie znaleziono gracza: {0}"),
                        Map.entry("command.noping", "{0} nie ma jeszcze danych o pingu"),
                        Map.entry("command.needworld", "Najpierw połącz się ze światem lub serwerem")
        );

        private ModLang() {
        }

        public static String currentLanguage() {
                String setting = EnemyPingConfig.get().general.language;
                return switch (setting == null ? "auto" : setting) {
                        case "pl" -> "pl";
                        case "en" -> "en";
                        default -> autoDetect();
                };
        }

        public static MutableComponent tr(String key, Object... args) {
                Map<String, String> map = "pl".equals(currentLanguage()) ? PL : EN;
                String tpl = map.getOrDefault(key, EN.getOrDefault(key, key));
                if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                                tpl = tpl.replace("{" + i + "}", String.valueOf(args[i]));
                        }
                }
                return Component.literal(tpl);
        }

        public static Component positionName(PingPosition position) {
                return switch (position) {
                        case RIGHT -> tr("pos.right");
                        case LEFT -> tr("pos.left");
                        case ABOVE -> tr("pos.above");
                        case BELOW -> tr("pos.below");
                        case ONLY -> tr("pos.only");
                };
        }

        public static Component languageName(String code) {
                return switch (code) {
                        case "pl" -> Component.literal("Polski");
                        case "en" -> Component.literal("English");
                        default -> tr("lang.auto");
                };
        }

        private static String autoDetect() {
                try {
                        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
                        if (selected != null && selected.toLowerCase(Locale.ROOT).startsWith("pl")) {
                                return "pl";
                        }
                } catch (Exception ignored) {
                }
                return "en";
        }
}
