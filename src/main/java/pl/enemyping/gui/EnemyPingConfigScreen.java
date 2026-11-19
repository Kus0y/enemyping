package pl.enemyping.gui;

import pl.enemyping.config.EnemyPingConfig;
import pl.enemyping.config.PingPosition;
import pl.enemyping.config.PingRange;
import pl.enemyping.lang.ModLang;
import pl.enemyping.util.PingLookup;
import pl.enemyping.util.PingText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.CycleButton;

/**
 * Tabbed config screen.
 * <p>
 * GENERAL - master switch, language and the shared color ranges
 * NAMETAG - ping next to nicknames (position, ms unit)
 * HUD     - your own ping on the screen (position, size)
 * <p>
 * Tab order: GENERAL, NAMETAG, HUD. Tab names follow the mod language
 * (ModLang) and refresh live when the language is changed; the NAMETAG
 * tab is displayed as "Player" / "Gracz".
 * <p>
 * Clicking a range's color button opens EnemyPingColorPickerScreen
 * (RGB sliders + hex + quick palette) instead of cycling the palette.
 * <p>
 * The TAB list ping has no options (always on with the master switch),
 * so there is no TABLIST tab anymore.
 * <p>
 * Everything saves automatically.
 */
public class EnemyPingConfigScreen extends Screen {

        private enum Tab {
                GENERAL("tab.general"),
                NAMETAG("tab.nametag"),
                HUD("tab.hud");

                final String labelKey;

                Tab(String labelKey) {
                        this.labelKey = labelKey;
                }
        }

        record ColorOption(int rgb, String en, String pl) {
                String hex() {
                        return String.format(Locale.ROOT, "#%06X", rgb);
                }
        }

        static final ColorOption[] PALETTE = {
                        new ColorOption(0x55FF55, "Green", "Zielony"),
                        new ColorOption(0xFFFF55, "Yellow", "Żółty"),
                        new ColorOption(0xFF5555, "Red", "Czerwony"),
                        new ColorOption(0xFFAA00, "Orange", "Pomarańczowy"),
                        new ColorOption(0x55FFFF, "Cyan", "Cyjan"),
                        new ColorOption(0x5555FF, "Blue", "Niebieski"),
                        new ColorOption(0xFF55FF, "Magenta", "Różowy"),
                        new ColorOption(0xFFFFFF, "White", "Biały"),
                        new ColorOption(0xAAAAAA, "Light gray", "Jasnoszary"),
                        new ColorOption(0x555555, "Dark gray", "Ciemnoszary"),
                        new ColorOption(0x00AA00, "Dark green", "Ciemnozielony"),
                        new ColorOption(0x00AAAA, "Dark cyan", "Ciemny cyjan"),
                        new ColorOption(0x0000AA, "Navy", "Granatowy"),
                        new ColorOption(0xAA00AA, "Purple", "Fioletowy"),
                        new ColorOption(0xAA0000, "Dark red", "Ciemnoczerwony"),
                        new ColorOption(0x000000, "Black", "Czarny")
        };

        private static final int ROW_HEIGHT = 24;
        private static final int RANGES_TOP = 140;
        private static final int WIDGET_W = 240;

        private final Screen parent;
        private Tab tab = Tab.GENERAL;
        private Button outOfRangeButton;

        // live ping lookup box (NAMETAG tab) - typed nick is kept across rebuilds
        private EditBox nickBox;
        private String nick = "";

        // ranges editor state (GENERAL tab)
        private int scroll = 0;
        private int visibleRows = 3;
        private final List<RowGui> rows = new ArrayList<>();

        private static final class RowGui {
                final PingRange range;
                final Button colorButton;
                final int y;

                RowGui(PingRange range, Button colorButton, int y) {
                        this.range = range;
                        this.colorButton = colorButton;
                        this.y = y;
                }
        }

        public EnemyPingConfigScreen(Screen parent) {
                super(ModLang.tr("menu.title"));
                this.parent = parent;
        }

        private int left() {
                return this.width / 2 - 136;
        }

        private int cx() {
                return this.width / 2;
        }

        @Override
        protected void init() {
                EnemyPingConfig cfg = EnemyPingConfig.get();
                addTabButtons();
                switch (this.tab) {
                        case NAMETAG -> initNametag(cfg);
                        case HUD -> initPersonal(cfg);
                        case GENERAL -> initGeneral(cfg);
                }
                addRenderableWidget(Button.builder(ModLang.tr("menu.done"), b -> this.onClose())
                                .bounds(cx() - 100, this.height - 26, 200, 20)
                                .build());
        }

        private void addTabButtons() {
                Tab[] tabs = Tab.values();
                int total = tabs.length * 74 + (tabs.length - 1) * 4;
                int x = cx() - total / 2;
                for (int i = 0; i < tabs.length; i++) {
                        Tab t = tabs[i];
                        int bx = x + i * 78;
                        addRenderableWidget(Button.builder(ModLang.tr(t.labelKey), b -> {
                                        this.tab = t;
                                        this.rebuildWidgets();
                                })
                                .bounds(bx, 26, 74, 20)
                                .build());
                }
        }

        private void initNametag(EnemyPingConfig cfg) {
                addRenderableWidget(toggle(cx() - WIDGET_W / 2, 56, "nametag.enabled", cfg.nametag.enabled,
                                v -> cfg.nametag.enabled = v));
                addRenderableWidget(CycleButton.<PingPosition>builder(ModLang::positionName, () -> cfg.nametag.position)
                                .withValues(PingPosition.values())
                                .create(cx() - WIDGET_W / 2, 80, WIDGET_W, 20, ModLang.tr("menu.position"),
                                                (btn, val) -> cfg.nametag.position = val));
                addRenderableWidget(toggle(cx() - WIDGET_W / 2, 104, "menu.msunit", cfg.general.showMsUnit,
                                v -> cfg.general.showMsUnit = v));
                // vanilla-style plate behind the ping line (ABOVE/BELOW positions)
                addRenderableWidget(toggle(cx() - WIDGET_W / 2, 128, "nametag.background", cfg.nametag.background,
                                v -> cfg.nametag.background = v));
                // 0 = no limit; pings are hidden beyond this many blocks
                addRenderableWidget(new IntValueSlider(cx() - WIDGET_W / 2, 152, WIDGET_W, 20, "nametag.distance",
                                0, 128, cfg.nametag.distanceLimit, v -> cfg.nametag.distanceLimit = v));
                // type a nick and see their live ping (same data as /enemyping <nick>)
                this.nickBox = new EditBox(this.font, cx() - WIDGET_W / 2, 176, 130, 20, Component.literal("nick"));
                this.nickBox.setMaxLength(16);
                this.nickBox.setValue(this.nick);
                this.nickBox.setHint(ModLang.tr("nametag.check.nickhint"));
                this.nickBox.setResponder(s -> this.nick = s);
                addRenderableWidget(this.nickBox);
                setInitialFocus(this.nickBox);
        }

        private void initPersonal(EnemyPingConfig cfg) {
                int x = cx() - WIDGET_W / 2;
                addRenderableWidget(toggle(x, 56, "personal.enabled", cfg.personal.enabled,
                                v -> cfg.personal.enabled = v));
                addRenderableWidget(new PercentSlider(x, 80, WIDGET_W, 20, "personal.x", 0, 100,
                                cfg.personal.xPercent, v -> cfg.personal.xPercent = v));
                addRenderableWidget(new PercentSlider(x, 104, WIDGET_W, 20, "personal.y", 0, 100,
                                cfg.personal.yPercent, v -> cfg.personal.yPercent = v));
                addRenderableWidget(new PercentSlider(x, 128, WIDGET_W, 20, "personal.scale", 50, 300,
                                cfg.personal.scalePercent, v -> cfg.personal.scalePercent = v));
        }

        private void initGeneral(EnemyPingConfig cfg) {
                int x = cx() - WIDGET_W / 2;
                addRenderableWidget(toggle(x, 56, "menu.master", cfg.general.masterEnabled,
                                v -> cfg.general.masterEnabled = v));
                addRenderableWidget(CycleButton.<String>builder(ModLang::languageName, () -> cfg.general.language)
                                .withValues(List.of("auto", "pl", "en"))
                                .create(x, 80, WIDGET_W, 20, ModLang.tr("menu.language"),
                                                (btn, val) -> {
                                                        cfg.general.language = val;
                                                        // rebuild in the next tick so every label
                                                        // (tabs included) switches language at once
                                                        if (this.minecraft != null) {
                                                                this.minecraft.execute(this::rebuildWidgets);
                                                        }
                                                }));

                // clamp scroll for the (possibly resized) ranges area
                int addY = this.height - 50;
                this.visibleRows = Math.max(2, (addY - RANGES_TOP - 8) / ROW_HEIGHT);
                int count = cfg.ranges.size();
                int maxScroll = Math.max(0, count - this.visibleRows);
                this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));

                rows.clear();
                int left = left();
                int last = Math.min(count, this.scroll + this.visibleRows);
                for (int i = this.scroll; i < last; i++) {
                        PingRange range = cfg.ranges.get(i);
                        int y = RANGES_TOP + (i - this.scroll) * ROW_HEIGHT;

                        EditBox minBox = new EditBox(this.font, left, y, 48, 20, Component.literal("min"));
                        minBox.setMaxLength(6);
                        minBox.setValue(String.valueOf(range.min));
                        minBox.setFilter(s -> s.matches("\\d*"));
                        minBox.setResponder(s -> {
                                Integer v = parseIntOrNull(s);
                                if (v != null) {
                                        range.min = v;
                                }
                        });
                        addRenderableWidget(minBox);

                        EditBox maxBox = new EditBox(this.font, left + 56, y, 48, 20, Component.literal("max"));
                        maxBox.setMaxLength(6);
                        maxBox.setValue(String.valueOf(range.max));
                        maxBox.setFilter(s -> s.matches("\\d*"));
                        maxBox.setResponder(s -> {
                                Integer v = parseIntOrNull(s);
                                if (v != null) {
                                        range.max = v;
                                }
                        });
                        addRenderableWidget(maxBox);

                        Button colorButton = Button.builder(colorButtonLabel(range.color), b -> {
                                                if (this.minecraft != null) {
                                                        this.minecraft.setScreen(new EnemyPingColorPickerScreen(this, range));
                                                }
                                        })
                                        .bounds(left + 108, y, 140, 20)
                                        .build();
                        addRenderableWidget(colorButton);

                        Button deleteButton = Button.builder(Component.literal("X"), b -> {
                                                cfg.ranges.remove(range);
                                                if (cfg.ranges.isEmpty()) {
                                                        cfg.ranges.add(new PingRange(0, 59, "#55FF55"));
                                                }
                                                this.rebuildWidgets();
                                        })
                                        .bounds(left + 252, y, 20, 20)
                                        .build();
                        addRenderableWidget(deleteButton);

                        rows.add(new RowGui(range, colorButton, y));
                }

                if (maxScroll > 0) {
                        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                                                if (this.scroll > 0) {
                                                        this.scroll--;
                                                        this.rebuildWidgets();
                                                }
                                        })
                                        .bounds(left + 278, RANGES_TOP, 20, 16)
                                        .build());
                        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                                                if (this.scroll < maxScroll) {
                                                        this.scroll++;
                                                        this.rebuildWidgets();
                                                }
                                        })
                                        .bounds(left + 278, RANGES_TOP + ROW_HEIGHT, 20, 16)
                                        .build());
                }

                addRenderableWidget(Button.builder(ModLang.tr("menu.ranges.add"), b -> {
                                        int nextMin = cfg.ranges.stream().mapToInt(r -> r.max).max().orElse(0) + 1;
                                        ColorOption color = PALETTE[cfg.ranges.size() % PALETTE.length];
                                        cfg.ranges.add(new PingRange(nextMin, nextMin + 99, color.hex()));
                                        int newMaxScroll = Math.max(0, cfg.ranges.size() - this.visibleRows);
                                        this.scroll = newMaxScroll;
                                        this.rebuildWidgets();
                                })
                                .bounds(left, addY, 150, 20)
                                .build());

                // color for pings matching no range - same picker as the ranges
                this.outOfRangeButton = Button.builder(colorButtonLabel(cfg.general.outOfRangeColor), b -> {
                                                if (this.minecraft != null) {
                                                        this.minecraft.setScreen(new EnemyPingColorPickerScreen(this,
                                                                        PingRange.parseColor(cfg.general.outOfRangeColor),
                                                                        cfg.general.outOfRangeColor,
                                                                        hex -> cfg.general.outOfRangeColor = hex));
                                                }
                                        })
                                        .bounds(left + 156, addY, 142, 20)
                                        .build();
                this.outOfRangeButton.setTooltip(Tooltip.create(ModLang.tr("menu.outofrange")));
                addRenderableWidget(this.outOfRangeButton);
        }

        private CycleButton<Boolean> toggle(int x, int y, String labelKey, boolean initial, Consumer<Boolean> onChange) {
                return CycleButton.<Boolean>builder(
                                v -> (boolean) v ? ModLang.tr("menu.on") : ModLang.tr("menu.off"),
                                () -> initial)
                                .withValues(List.of(true, false))
                                .create(x, y, WIDGET_W, 20, ModLang.tr(labelKey),
                                                (btn, val) -> onChange.accept(val));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                renderBackground(graphics, mouseX, mouseY, delta);
                graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

                // active tab underline
                Tab[] tabs = Tab.values();
                int total = tabs.length * 74 + (tabs.length - 1) * 4;
                int tx = cx() - total / 2;
                for (int i = 0; i < tabs.length; i++) {
                        if (tabs[i] == this.tab) {
                                graphics.fill(tx + i * 78, 45, tx + i * 78 + 74, 47, 0xFFFFFFFF);
                        }
                }

                if (this.tab == Tab.GENERAL) {
                        graphics.drawString(this.font, ModLang.tr("menu.ranges"), left(), 116, 0xFFA0A0A0, false);
                        graphics.drawString(this.font, ModLang.tr("menu.hint"), left(), 128, 0xFF707070, false);
                        for (RowGui row : rows) {
                                graphics.drawCenteredString(this.font, "-", left() + 52, row.y + 6, 0xFFA0A0A0);
                        }
                }

                if (this.tab == Tab.NAMETAG) {
                        // note about the command under the nick box
                        graphics.drawString(this.font, ModLang.tr("nametag.check.hint"),
                                        cx() - WIDGET_W / 2, 200, 0xFF707070, false);
                        // live ping of the typed nick, next to the box
                        if (this.nickBox != null && !this.nick.isBlank()) {
                                Integer latency = PingLookup.latencyOfName(this.nick);
                                if (latency == null) {
                                        graphics.drawString(this.font, ModLang.tr("nametag.check.notfound"),
                                                        cx() + 18, 182, 0xFFFF5555, false);
                                } else {
                                        graphics.drawString(this.font,
                                                        Component.literal(this.nick + ": ").append(PingText.buildWithNoData(latency)),
                                                        cx() + 18, 182, 0xFFFFFFFF, false);
                                }
                        }
                }

                super.render(graphics, mouseX, mouseY, delta);

                if (this.tab == Tab.GENERAL) {
                        for (RowGui row : rows) {
                                graphics.fill(row.colorButton.getX() + 3, row.colorButton.getY() + 3,
                                                row.colorButton.getX() + 17, row.colorButton.getY() + 17,
                                                0xFF000000 | row.range.colorInt());
                        }
                        if (this.outOfRangeButton != null) {
                                EnemyPingConfig cfg = EnemyPingConfig.get();
                                graphics.fill(this.outOfRangeButton.getX() + 3, this.outOfRangeButton.getY() + 3,
                                                this.outOfRangeButton.getX() + 17, this.outOfRangeButton.getY() + 17,
                                                PingRange.parseColor(cfg.general.outOfRangeColor));
                        }
                }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                if (this.tab == Tab.GENERAL) {
                        EnemyPingConfig cfg = EnemyPingConfig.get();
                        int maxScroll = Math.max(0, cfg.ranges.size() - this.visibleRows);
                        if (maxScroll > 0 && mouseY >= RANGES_TOP - 12 && mouseY <= this.height - 56) {
                                int newScroll = this.scroll - (int) Math.signum(scrollY);
                                if (newScroll >= 0 && newScroll <= maxScroll && newScroll != this.scroll) {
                                        this.scroll = newScroll;
                                        this.rebuildWidgets();
                                        return true;
                                }
                        }
                }
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public void onClose() {
                EnemyPingConfig.save();
                if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                }
        }

        private static Integer parseIntOrNull(String s) {
                if (s == null || s.isEmpty()) {
                        return null;
                }
                try {
                        return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                        return null;
                }
        }

        private static int indexOfColor(String hex) {
                int rgb = 0xFFFFFF;
                try {
                        String h = hex == null ? "" : hex.trim();
                        if (h.startsWith("#")) {
                                h = h.substring(1);
                        }
                        if (h.length() == 6) {
                                rgb = Integer.parseInt(h, 16);
                        }
                } catch (NumberFormatException ignored) {
                }
                for (int i = 0; i < PALETTE.length; i++) {
                        if (PALETTE[i].rgb == rgb) {
                                return i;
                        }
                }
                return -1;
        }

        /** Button label: palette name for known colors, the hex string for custom ones. */
        private static Component colorButtonLabel(String hex) {
                int index = indexOfColor(hex);
                if (index >= 0) {
                        return colorLabel(index);
                }
                int rgb = 0xFFFFFF;
                try {
                        String h = hex == null ? "" : hex.trim();
                        if (h.startsWith("#")) {
                                h = h.substring(1);
                        }
                        if (h.length() == 6) {
                                rgb = Integer.parseInt(h, 16);
                        }
                } catch (NumberFormatException ignored) {
                }
                return Component.literal(String.format(Locale.ROOT, "#%06X", rgb));
        }

        private static Component colorLabel(int index) {
                ColorOption option = PALETTE[index];
                boolean polish = "pl".equals(ModLang.currentLanguage());
                return Component.literal(polish ? option.pl() : option.en());
        }

        /** Slider for percent-based integer settings (X, Y, size). */
        private static final class PercentSlider extends AbstractSliderButton {
                private final String labelKey;
                private final int min;
                private final int max;
                private final IntConsumer onChange;

                PercentSlider(int x, int y, int w, int h, String labelKey, int min, int max,
                              int initial, IntConsumer onChange) {
                        super(x, y, w, h, Component.empty(), (initial - min) / (double) (max - min));
                        this.labelKey = labelKey;
                        this.min = min;
                        this.max = max;
                        this.onChange = onChange;
                        updateMessage();
                }

                private int current() {
                        return (int) Math.round(this.min + this.value * (this.max - this.min));
                }

                @Override
                protected void updateMessage() {
                        setMessage(Component.literal("")
                                        .append(ModLang.tr(this.labelKey))
                                        .append(": " + this.current() + "%"));
                }

                @Override
                protected void applyValue() {
                        this.onChange.accept(this.current());
                }
        }

        /** Slider for block-based integer settings (render distance); 0 renders as "No limit". */
        private static final class IntValueSlider extends AbstractSliderButton {
                private final String labelKey;
                private final int min;
                private final int max;
                private final IntConsumer onChange;

                IntValueSlider(int x, int y, int w, int h, String labelKey, int min, int max,
                               int initial, IntConsumer onChange) {
                        super(x, y, w, h, Component.empty(), (initial - min) / (double) (max - min));
                        this.labelKey = labelKey;
                        this.min = min;
                        this.max = max;
                        this.onChange = onChange;
                        updateMessage();
                }

                private int current() {
                        return (int) Math.round(this.min + this.value * (this.max - this.min));
                }

                @Override
                protected void updateMessage() {
                        Component value = this.current() == 0
                                        ? ModLang.tr("distance.unlimited")
                                        : Component.literal(String.valueOf(this.current()));
                        setMessage(Component.literal("")
                                        .append(ModLang.tr(this.labelKey))
                                        .append(": ")
                                        .append(value));
                }

                @Override
                protected void applyValue() {
                        this.onChange.accept(this.current());
                }
        }
}
