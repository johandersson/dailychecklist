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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import javax.swing.Icon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Renders HTML-like text with styles and icons for the help dialog.
 */
public class HelpTextRenderer {

    private static final Map<String, BiFunction<String, Style, Icon>> ICON_CREATORS = new HashMap<>();

    static {
        ICON_CREATORS.put("[RED_CLOCK_ICON]", (content, style) -> IconCache.getReminderClockIcon(9, 30, ReminderClockIcon.State.OVERDUE, false));
        ICON_CREATORS.put("[YELLOW_CLOCK_ICON]", (content, style) -> IconCache.getReminderClockIcon(9, 30, ReminderClockIcon.State.DUE_SOON, false));
        ICON_CREATORS.put("[BLUE_CLOCK_ICON]", (content, style) -> IconCache.getReminderClockIcon(9, 30, ReminderClockIcon.State.FUTURE, false));
        ICON_CREATORS.put("[ZZZ_ICON]", (content, style) -> IconCache.getZzzIcon());
    }

    /**
     * Inserts styled text with icons into the document.
     */
    public static void insertStyledTextWithIcons(StyledDocument doc, String htmlText, Style defaultStyle, Style boldStyle) {
        try {
            String processedText = preprocessHtml(htmlText);

            // Split by lines and process each line
            String[] lines = processedText.split("\n");
            boolean lastWasEmpty = false;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    if (!lastWasEmpty) {
                        doc.insertString(doc.getLength(), "\n", defaultStyle);
                    }
                    lastWasEmpty = true;
                    continue;
                }

                lastWasEmpty = false;
                processLine(doc, line, defaultStyle, boldStyle);
            }
        } catch (BadLocationException e) {
            java.util.logging.Logger.getLogger(HelpTextRenderer.class.getName()).log(java.util.logging.Level.SEVERE, "Failed to load help text", e);
        }
    }

    private static String preprocessHtml(String htmlText) {
        // Remove the <style> block to avoid displaying CSS as text
        htmlText = htmlText.replaceAll("(?s)<style[^>]*>.*?</style>", "");

        // Replace HTML tags with markers for parsing
        htmlText = htmlText.replaceAll("<h1>", "[H1]").replaceAll("</h1>", "[/H1]");
        htmlText = htmlText.replaceAll("<h2>", "[H2]").replaceAll("</h2>", "[/H2]");
        htmlText = htmlText.replaceAll("<h3>", "[H3]").replaceAll("</h3>", "[/H3]");
        htmlText = htmlText.replaceAll("<li>", "[LI]").replaceAll("</li>", "[/LI]");
        htmlText = htmlText.replaceAll("<p>", "[P]").replaceAll("</p>", "[/P]");
        htmlText = htmlText.replaceAll("<ul>", "").replaceAll("</ul>", "");
        htmlText = htmlText.replaceAll("<ol>", "").replaceAll("</ol>", "");
        htmlText = htmlText.replaceAll("<b>", "[B]").replaceAll("</b>", "[/B]");
        htmlText = htmlText.replaceAll("<em>", "[I]").replaceAll("</em>", "[/I]");

        // Remove remaining HTML tags
        htmlText = htmlText.replaceAll("<[^>]+>", "").trim();

        // Collapse multiple blank lines to at most two
        htmlText = htmlText.replaceAll("\n\n\n+", "\n\n");

        return htmlText;
    }

    private static void processLine(StyledDocument doc, String line, Style defaultStyle, Style boldStyle) throws BadLocationException {
        if (line.contains("[H1]") && line.contains("[/H1]")) {
            insertHeader(doc, line, "[H1]", "[/H1]", 24);
        } else if (line.contains("[H2]") && line.contains("[/H2]")) {
            insertHeader(doc, line, "[H2]", "[/H2]", 18);
        } else if (line.contains("[H3]") && line.contains("[/H3]")) {
            insertHeader(doc, line, "[H3]", "[/H3]", 14);
        } else if (line.contains("[LI]") && line.contains("[/LI]")) {
            processListItem(doc, line, defaultStyle, boldStyle);
        } else if (line.contains("[P]") && line.contains("[/P]")) {
            insertParagraph(doc, line, defaultStyle, boldStyle);
        } else if (line.contains("[RED_CLOCK_ICON]") || line.contains("[YELLOW_CLOCK_ICON]") || 
                    line.contains("[BLUE_CLOCK_ICON]") || line.contains("[ZZZ_ICON]")) {
            processIconLine(doc, line, boldStyle);
        } else {
            insertRegularText(doc, line, defaultStyle);
        }
    }

    private static void insertHeader(StyledDocument doc, String line, String startMarker, String endMarker, int fontSize) throws BadLocationException {
        String content = line.replace(startMarker, "").replace(endMarker, "").trim();
        Style headerStyle = createHeaderStyle(doc, fontSize);
        // Use a single newline after headers to reduce excessive spacing
        String suffix = "\n";
        doc.insertString(doc.getLength(), content + suffix, headerStyle);
    }

    private static Style createHeaderStyle(StyledDocument doc, int fontSize) {
        Style style = doc.addStyle("h" + fontSize, null);
        StyleConstants.setFontFamily(style, FontManager.FONT_NAME);
        StyleConstants.setFontSize(style, fontSize);
        StyleConstants.setBold(style, true);
        return style;
    }

    private static void processListItem(StyledDocument doc, String line, Style defaultStyle, Style boldStyle) throws BadLocationException {
        String content = line.replace("[LI]", "").replace("[/LI]", "").trim();

        boolean hasNested = content.contains("[LI]");
        if (hasNested) {
            content = content.replace("[LI]", "• ").replace("[/LI]", "");
        }

        // Leave icon placeholders like [BLUE_CLOCK_ICON] as literal text in help lists
        content = content.replace("[B]", "").replace("[/B]", "");

        // reference boldStyle to avoid unused-parameter warnings in some builds
        if (boldStyle == null) {
            // no-op
        }

        // Regular list item — preserve bracketed placeholders instead of rendering icons
        doc.insertString(doc.getLength(), (hasNested ? "" : "• ") + content, defaultStyle);
        doc.insertString(doc.getLength(), "\n", defaultStyle);
    }

    private static void insertParagraph(StyledDocument doc, String line, Style defaultStyle, Style boldStyle) throws BadLocationException {
        String content = line.replace("[P]", "").replace("[/P]", "").trim();

        content = content.replace("[B]", "").replace("[/B]", "");
        // reference boldStyle to avoid unused-parameter warnings in some builds
        if (boldStyle == null) {
            // no-op
        }
        doc.insertString(doc.getLength(), content, defaultStyle);
        doc.insertString(doc.getLength(), "\n", defaultStyle);
    }

    private static void processIconLine(StyledDocument doc, String line, Style boldStyle) throws BadLocationException {
        for (Map.Entry<String, BiFunction<String, Style, Icon>> entry : ICON_CREATORS.entrySet()) {
            if (line.contains(entry.getKey())) {
                String processedLine = line.replace(entry.getKey(), "");
                insertIconAndText(doc, processedLine, entry.getValue().apply(line, boldStyle), boldStyle);
                doc.insertString(doc.getLength(), "\n", boldStyle);
                return;
            }
        }
    }

    private static void insertRegularText(StyledDocument doc, String line, Style defaultStyle) throws BadLocationException {
        line = line.replace("[B]", "").replace("[/B]", "");
        doc.insertString(doc.getLength(), line, defaultStyle);
        doc.insertString(doc.getLength(), "\n", defaultStyle);
    }

    private static void insertIconAndText(StyledDocument doc, String text, Icon icon, Style style) throws BadLocationException {
        // Insert the icon
        Style iconStyle = doc.addStyle("icon", null);
        StyleConstants.setIcon(iconStyle, icon);
        doc.insertString(doc.getLength(), " ", iconStyle);

        // Insert the text
        doc.insertString(doc.getLength(), text, style);
    }
}