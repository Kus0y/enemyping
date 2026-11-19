package pl.enemyping.gui;

import pl.enemyping.config.EnemyPingConfig;
import pl.enemyping.config.PingRange;
import pl.enemyping.lang.ModLang;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

/**
 * Color picker opened by clicking a range's color button on the
 * GENERAL tab. Shows a live preview, three RGB sliders (0-255), a hex
 * field and the quick palette (2 x 8 swatches).
 * <p>
 * Done applies the color and saves the config, Esc / Cancel reverts
 * to the color the target had when the picker was opened.
 * <p>
 * Can edit a PingRange's color (ranges list) or an arbitrary color via
 * the callback constructor (used for the out-of-range color).
 */
public class EnemyPingColorPickerScreen extends Screen {

        private static final int SWATCH = 20;
        private static final int SWATCH_GAP = 4;

        private final Screen parent;
        private final PingRange range;
        private final Consumer<String> applier;
        private final String originalColor;

        private int rgb;
        private boolean syncing = false;

        private EditBox hexBox;
        private IntSlider redSlider;
        private IntSlider greenSlider;
        private IntSlider blueSlider;

        public EnemyPingColorPickerScreen(Screen parent, PingRange range) {
                this(parent, range, hex -> range.color = hex);
        }

        private EnemyPingColorPickerScreen(Screen parent, PingRange range, Consumer<String> applier) {
                super(ModLang.tr("picker.title"));
                this.parent = parent;
                this.range = range;
                this.applier = applier;
                this.originalColor = range.color;
                this.rgb = range.colorInt() & 0xFFFFFF;
        }

        /** Callback variant (e.g. out-of-range color): Done calls applier with "#RRGGBB". */
        public EnemyPingColorPickerScreen(Screen parent, int initialRgb, String originalColor, Consumer<String> applier) {
                super(ModLang.tr("picker.title"));
                this.parent = parent;
                this.range = null;
                this.applier = applier;
                this.originalColor = originalColor;
                this.rgb = initialRgb & 0xFFFFFF;
        }

        private int cx() {
                return this.width / 2;
        }

        @Override
        protected void init() {
                this.hexBox = new EditBox(this.font, cx() - 60, 64, 120, 20, Component.literal("hex"));
                this.hexBox.setMaxLength(7);
                this.hexBox.setFilter(s -> s.matches("[#]?[0-9a-fA-F]{0,6}"));
                this.hexBox.setValue(hex());
                this.hexBox.setResponder(s -> {
                        if (this.syncing) {
                                return;
                        }
                        Integer parsed = parseHex(s);
                        if (parsed != null) {
                                setRgb(parsed, false, true);
                        }
                });
                addRenderableWidget(this.hexBox);

                this.redSlider = addRenderableWidget(new IntSlider(cx() - 120, 92, 240, 20, "picker.red",
                                this.rgb >> 16 & 0xFF, v -> setChannel(16, v)));
                this.greenSlider = addRenderableWidget(new IntSlider(cx() - 120, 116, 240, 20, "picker.green",
                                this.rgb >> 8 & 0xFF, v -> setChannel(8, v)));
                this.blueSlider = addRenderableWidget(new IntSlider(cx() - 120, 140, 240, 20, "picker.blue",
                                this.rgb & 0xFF, v -> setChannel(0, v)));

                // quick palette: 2 rows of 8 clickable swatches (filled in render())
                int x0 = cx() - 94;
                int gridTop = this.height - 76;
                for (int i = 0; i < EnemyPingConfigScreen.PALETTE.length; i++) {
                        final int index = i;
                        int sx = x0 + (i % 8) * (SWATCH + SWATCH_GAP);
                        int sy = gridTop + (i / 8) * (SWATCH + SWATCH_GAP);
                        addRenderableWidget(Button.builder(Component.empty(), b -> setRgb(
                                                        EnemyPingConfigScreen.PALETTE[index].rgb(), true, true))
                                        .bounds(sx, sy, SWATCH, SWATCH)
                                        .build());
                }

                addRenderableWidget(Button.builder(ModLang.tr("picker.cancel"), b -> cancel())
                                .bounds(cx() - 104, this.height - 26, 100, 20)
                                .build());
                addRenderableWidget(Button.builder(ModLang.tr("menu.done"), b -> apply())
                                .bounds(cx() + 4, this.height - 26, 100, 20)
                                .build());
        }

        private String hex() {
                return String.format(Locale.ROOT, "#%06X", this.rgb);
        }

        /** Slider drag: update rgb + hex text, leave the sliders alone. */
        private void setChannel(int shift, int value) {
                int mask = 0xFF << shift;
                setRgb((this.rgb & ~mask) | (value << shift), true, false);
        }

        private void setRgb(int newRgb, boolean updateHex, boolean updateSliders) {
                this.rgb = newRgb & 0xFFFFFF;
                this.syncing = true;
                if (updateHex && this.hexBox != null) {
                        this.hexBox.setValue(hex());
                }
                if (updateSliders && this.redSlider != null && this.greenSlider != null
                                && this.blueSlider != null) {
                        this.redSlider.setFromValue(this.rgb >> 16 & 0xFF);
                        this.greenSlider.setFromValue(this.rgb >> 8 & 0xFF);
                        this.blueSlider.setFromValue(this.rgb & 0xFF);
                }
                this.syncing = false;
        }

        private void apply() {
                this.applier.accept(hex());
                EnemyPingConfig.save();
                if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                }
        }

        private void cancel() {
                this.applier.accept(this.originalColor);
                if (this.minecraft != null) {
                        this.minecraft.setScreen(this.parent);
                }
        }

        @Override
        public void onClose() {
                // Esc = cancel, keep the original color
                cancel();
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                renderBackground(graphics, mouseX, mouseY, delta);
                graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

                // live preview block with a dark border
                graphics.fill(cx() - 62, 26, cx() + 62, 52, 0xFF000000);
                graphics.fill(cx() - 60, 28, cx() + 60, 50, 0xFF000000 | this.rgb);
                graphics.drawString(this.font, ModLang.tr("picker.hex"), cx() - 60, 54, 0xFFA0A0A0, false);

                super.render(graphics, mouseX, mouseY, delta);

                // swatch colors drawn over the (empty) palette buttons
                int x0 = cx() - 94;
                int gridTop = this.height - 76;
                for (int i = 0; i < EnemyPingConfigScreen.PALETTE.length; i++) {
                        int sx = x0 + (i % 8) * (SWATCH + SWATCH_GAP);
                        int sy = gridTop + (i / 8) * (SWATCH + SWATCH_GAP);
                        graphics.fill(sx + 2, sy + 2, sx + SWATCH - 2, sy + SWATCH - 2,
                                        0xFF000000 | EnemyPingConfigScreen.PALETTE[i].rgb());
                        if (mouseX >= sx && mouseX < sx + SWATCH && mouseY >= sy && mouseY < sy + SWATCH) {
                                graphics.fill(sx, sy, sx + SWATCH, sy + 1, 0xFFFFFFFF);
                                graphics.fill(sx, sy + SWATCH - 1, sx + SWATCH, sy + SWATCH, 0xFFFFFFFF);
                                graphics.fill(sx, sy, sx + 1, sy + SWATCH, 0xFFFFFFFF);
                                graphics.fill(sx + SWATCH - 1, sy, sx + SWATCH, sy + SWATCH, 0xFFFFFFFF);
                        }
                }
        }

        private static Integer parseHex(String s) {
                if (s == null) {
                        return null;
                }
                String h = s.trim();
                if (h.startsWith("#")) {
                        h = h.substring(1);
                }
                if (h.length() != 6) {
                        return null;
                }
                try {
                        int v = Integer.parseInt(h, 16);
                        return (v >= 0 && v <= 0xFFFFFF) ? v : null;
                } catch (NumberFormatException e) {
                        return null;
                }
        }

        /** Slider for plain integer values (RGB channels), message "Label: value". */
        private static final class IntSlider extends AbstractSliderButton {

                private static final int CHANNEL_MAX = 255;

                private final String labelKey;
                private final IntConsumer onChange;

                IntSlider(int x, int y, int w, int h, String labelKey, int initial, IntConsumer onChange) {
                        super(x, y, w, h, Component.empty(), initial / (double) CHANNEL_MAX);
                        this.labelKey = labelKey;
                        this.onChange = onChange;
                        updateMessage();
                }

                private int current() {
                        return (int) Math.round(this.value * CHANNEL_MAX);
                }

                void setFromValue(int v) {
                        this.value = Math.max(0.0, Math.min(1.0, v / (double) CHANNEL_MAX));
                        updateMessage();
                }

                @Override
                protected void updateMessage() {
                        setMessage(Component.literal("")
                                        .append(ModLang.tr(this.labelKey))
                                        .append(": " + this.current()));
                }

                @Override
                protected void applyValue() {
                        this.onChange.accept(this.current());
                }
        }
}
