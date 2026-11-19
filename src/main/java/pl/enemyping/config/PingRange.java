package pl.enemyping.config;

/**
 * A single color range: pings from {@code min} to {@code max} (inclusive)
 * are rendered with color {@code color} ("#RRGGBB").
 */
public class PingRange {
        public int min;
        public int max;
        public String color;

        public PingRange() {
        }

        public PingRange(int min, int max, String color) {
                this.min = min;
                this.max = max;
                this.color = color;
        }

        public boolean matches(int ping) {
                return ping >= min && ping <= max;
        }

        public int colorInt() {
                return parseColor(color);
        }

        /** Parses "#RRGGBB" (with or without '#'); invalid/missing values fall back to white. */
        public static int parseColor(String hex) {
                try {
                        String h = hex == null ? "" : hex.trim();
                        if (h.startsWith("#")) {
                                h = h.substring(1);
                        }
                        if (h.length() != 6) {
                                return 0xFFFFFFFF;
                        }
                        return 0xFF000000 | Integer.parseInt(h, 16);
                } catch (NumberFormatException e) {
                        return 0xFFFFFFFF;
                }
        }
}
