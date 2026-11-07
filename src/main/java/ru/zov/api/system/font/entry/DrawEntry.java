package ru.zov.api.system.font.entry;

import ru.zov.api.system.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
