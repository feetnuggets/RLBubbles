package com.rlbubbles.client;

import com.rlbubbles.common.config.RLBubblesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Turns a raw dialogue string into a styled Component for rendering.
 *
 * Supported, all optional and config-gated:
 *  - Vanilla legacy codes via '&' (e.g. "&cdanger" -> red) when allowFormatting is on.
 *  - A gradient directive at the very start: "{gradient:RRGGBB,RRGGBB} text..." when
 *    allowGradients is on, interpolating per-character between the two hex colors.
 *  - Plain Unicode/emoji pass straight through (the font handles glyphs it has).
 */
public final class BubbleTextFormatter {

    private BubbleTextFormatter() {}

    public static Component format(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        if (RLBubblesConfig.allowGradients() && raw.startsWith("{gradient:")) {
            int end = raw.indexOf('}');
            if (end > 0) {
                String spec = raw.substring("{gradient:".length(), end);
                String body = raw.substring(end + 1).stripLeading();
                String[] parts = spec.split(",");
                if (parts.length == 2) {
                    try {
                        int c1 = Integer.parseInt(parts[0].trim().replace("#", ""), 16);
                        int c2 = Integer.parseInt(parts[1].trim().replace("#", ""), 16);
                        return gradient(body, c1, c2);
                    } catch (NumberFormatException ignored) {
                        // fall through to normal formatting
                    }
                }
            }
        }

        if (RLBubblesConfig.allowFormatting() && raw.indexOf('&') >= 0) {
            return legacy(raw);
        }
        return Component.literal(raw);
    }

    /** Interpolate a two-color gradient across the visible characters. */
    private static Component gradient(String text, int c1, int c2) {
        MutableComponent out = Component.empty();
        int len = text.length();
        if (len == 0) return out;
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        for (int i = 0; i < len; i++) {
            float t = len == 1 ? 0f : (float) i / (len - 1);
            int r = Math.round(r1 + (r2 - r1) * t);
            int g = Math.round(g1 + (g2 - g1) * t);
            int b = Math.round(b1 + (b2 - b1) * t);
            int rgb = (r << 16) | (g << 8) | b;
            out.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return out;
    }

    /** Parse '&'-prefixed legacy formatting codes. */
    private static Component legacy(String text) {
        MutableComponent out = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                ChatFormatting fmt = byCode(code);
                if (fmt != null) {
                    if (buf.length() > 0) {
                        out.append(Component.literal(buf.toString()).setStyle(style));
                        buf.setLength(0);
                    }
                    style = applyFormat(style, fmt);
                    i++;
                    continue;
                }
            }
            buf.append(ch);
        }
        if (buf.length() > 0) out.append(Component.literal(buf.toString()).setStyle(style));
        return out;
    }

    private static Style applyFormat(Style style, ChatFormatting fmt) {
        if (fmt == ChatFormatting.RESET) return Style.EMPTY;
        if (fmt.isColor()) return style.withColor(fmt);
        switch (fmt) {
            case BOLD: return style.withBold(true);
            case ITALIC: return style.withItalic(true);
            case UNDERLINE: return style.withUnderlined(true);
            case STRIKETHROUGH: return style.withStrikethrough(true);
            case OBFUSCATED: return style.withObfuscated(true);
            default: return style;
        }
    }

    private static ChatFormatting byCode(char code) {
        for (ChatFormatting f : ChatFormatting.values()) {
            if (f.getChar() == code) return f;
        }
        return null;
    }
}
