/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache of FontMetrics per Font. If a FontMetrics is not present,
 * it is created using a temporary BufferedImage Graphics context.
 */
public final class FontMetricsCache {
    private static final ConcurrentHashMap<Font, FontMetrics> CACHE = new ConcurrentHashMap<>();

    private FontMetricsCache() {}

    public static FontMetrics get(Font font) {
        FontMetrics fm = CACHE.get(font);
        if (fm != null) return fm;
        // Create a temporary graphics context to obtain FontMetrics
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setFont(font);
            FontMetrics created = g2.getFontMetrics(font);
            FontMetrics prev = CACHE.putIfAbsent(font, created);
            return prev != null ? prev : created;
        } finally {
            g2.dispose();
        }
    }
}
