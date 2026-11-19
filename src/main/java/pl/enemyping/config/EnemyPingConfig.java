package pl.enemyping.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Config stored in config/enemyping.json (v2, grouped in tabs).
 * <p>
 * general  - master switch, language, ms unit, shared color ranges
 * nametag  - ping next to player nicknames (nametags)
 * personal - your own ping rendered on the HUD
 * <p>
 * The TAB list ping has no options - it is always on (master switch only)
 * and uses the fixed right-aligned layout (BetterPingDisplay style).
 * <p>
 * Legacy (v1) flat keys are read once and migrated automatically.
 */
public class EnemyPingConfig {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private static EnemyPingConfig instance;

        public General general = new General();
        public Nametag nametag = new Nametag();
        public Personal personal = new Personal();

        /** Shared ping -> color ranges, edited on the GENERAL tab. */
        public List<PingRange> ranges = defaultRanges();

        // ---- legacy v1 keys (migrated on load, dropped on next save) ----
        @SerializedName("showPing") public Boolean legacyShowPing;
        @SerializedName("position") public PingPosition legacyPosition;
        @SerializedName("showMsUnit") public Boolean legacyShowMsUnit;
        @SerializedName("language") public String legacyLanguage;

        public static class General {
                public boolean masterEnabled = true;
                public String language = "en"; // auto | pl | en (fresh-install default: English)
                public boolean showMsUnit = true;
                /** Color for pings that match no range (fresh-install default: white, same as before 1.3.4). */
                public String outOfRangeColor = "#FFFFFF";
        }

        public static class Nametag {
                public boolean enabled = true;
                public PingPosition position = PingPosition.ABOVE;
                /** 0 = no limit (fresh-install default); otherwise pings are hidden beyond this many blocks. */
                public int distanceLimit = 0;
                /** Vanilla-style plate behind the ping line (ABOVE/BELOW positions). */
                public boolean background = true;
        }

        public static class Personal {
                public boolean enabled = true;
                /** Widget position in percent of the screen (0..100). */
                public int xPercent = 94;
                public int yPercent = 96;
                /** Text scale in percent (50..300). */
                public int scalePercent = 200;
        }

        public static List<PingRange> defaultRanges() {
                List<PingRange> list = new ArrayList<>();
                list.add(new PingRange(0, 59, "#55FF55"));
                list.add(new PingRange(60, 99, "#FFFF55"));
                list.add(new PingRange(100, 100000, "#FF5555"));
                return list;
        }

        public static EnemyPingConfig get() {
                if (instance == null) {
                        load();
                }
                return instance;
        }

        public static void load() {
                Path path = path();
                if (Files.exists(path)) {
                        try {
                                EnemyPingConfig read = GSON.fromJson(Files.readString(path), EnemyPingConfig.class);
                                if (read != null) {
                                        instance = read;
                                        normalize();
                                        return;
                                }
                        } catch (Exception e) {
                                System.err.println("[Enemy Ping] Failed to read config, using defaults: " + e);
                        }
                }
                instance = new EnemyPingConfig();
                save();
        }

        public static void save() {
                if (instance == null) {
                        return;
                }
                try {
                        normalize();
                        Files.createDirectories(path().getParent());
                        Files.writeString(path(), GSON.toJson(instance));
                } catch (IOException e) {
                        System.err.println("[Enemy Ping] Failed to save config: " + e);
                }
        }

        private static Path path() {
                return FabricLoader.getInstance().getConfigDir().resolve("enemyping.json");
        }

        public static void normalize() {
                EnemyPingConfig c = instance;
                if (c == null) {
                        return;
                }
                if (c.general == null) {
                        c.general = new General();
                }
                if (c.nametag == null) {
                        c.nametag = new Nametag();
                }
                if (c.personal == null) {
                        c.personal = new Personal();
                }

                // migrate v1 flat keys once
                if (c.legacyPosition != null) {
                        c.nametag.position = c.legacyPosition;
                        c.legacyPosition = null;
                }
                if (c.legacyShowPing != null) {
                        c.nametag.enabled = c.legacyShowPing;
                        c.legacyShowPing = null;
                }
                if (c.legacyShowMsUnit != null) {
                        c.general.showMsUnit = c.legacyShowMsUnit;
                        c.legacyShowMsUnit = null;
                }
                if (c.legacyLanguage != null) {
                        c.general.language = c.legacyLanguage;
                        c.legacyLanguage = null;
                }

                if (c.ranges == null) {
                        c.ranges = defaultRanges();
                }
                c.ranges.removeIf(r -> r == null || r.min < 0 || r.max < 0);
                for (PingRange r : c.ranges) {
                        if (r.min > r.max) {
                                int t = r.min;
                                r.min = r.max;
                                r.max = t;
                        }
                }
                if (c.ranges.isEmpty()) {
                        c.ranges = defaultRanges();
                }
                c.ranges.sort(Comparator.comparingInt(r -> r.min));

                if (c.general.language == null) {
                        c.general.language = "en";
                }
                if (c.general.outOfRangeColor == null) {
                        c.general.outOfRangeColor = "#FFFFFF";
                }
                if (c.nametag.position == null) {
                        c.nametag.position = PingPosition.ABOVE;
                }
                c.nametag.distanceLimit = clamp(c.nametag.distanceLimit, 0, 128);
                c.personal.xPercent = clamp(c.personal.xPercent, 0, 100);
                c.personal.yPercent = clamp(c.personal.yPercent, 0, 100);
                c.personal.scalePercent = clamp(c.personal.scalePercent, 50, 300);
        }

        private static int clamp(int v, int min, int max) {
                return Math.max(min, Math.min(max, v));
        }

        /** Master switch AND feature switch. */
        public boolean nametagActive() {
                return general.masterEnabled && nametag.enabled;
        }

        public boolean personalActive() {
                return general.masterEnabled && personal.enabled;
        }

        public int colorForPing(int ping) {
                for (PingRange range : ranges) {
                        if (range.matches(ping)) {
                                return range.colorInt();
                        }
                }
                return PingRange.parseColor(general.outOfRangeColor);
        }
}
