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
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Renders HTML-like text with styles and icons for the help dialog.
 */
public class HelpTextRenderer {

    /**
     * Inserts styled text with icons into the document.
     */
    public static void insertStyledTextWithIcons(StyledDocument doc, String htmlText, Style defaultStyle, Style boldStyle) {
        try {
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
            htmlText = htmlText.replaceAll("<code>", "[CODE]").replaceAll("</code>", "[/CODE]");
            htmlText = htmlText.replaceAll("<em>", "[I]").replaceAll("</em>", "[/I]");

            // Remove remaining HTML tags
            String text = htmlText.replaceAll("<[^>]+>", "").trim();

            // Split by lines and process each line
            String[] lines = text.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                    continue;
                }

                // Handle markers
                if (line.contains("[H1]") && line.contains("[/H1]")) {
                    String content = line.replace("[H1]", "").replace("[/H1]", "").trim();
                    Style h1Style = doc.addStyle("h1", null);
                    StyleConstants.setFontFamily(h1Style, "Arial");
                    StyleConstants.setFontSize(h1Style, 24);
                    StyleConstants.setBold(h1Style, true);
                    doc.insertString(doc.getLength(), content + "\n\n", h1Style);
                } else if (line.contains("[H2]") && line.contains("[/H2]")) {
                    String content = line.replace("[H2]", "").replace("[/H2]", "").trim();
                    Style h2Style = doc.addStyle("h2", null);
                    StyleConstants.setFontFamily(h2Style, "Arial");
                    StyleConstants.setFontSize(h2Style, 18);
                    StyleConstants.setBold(h2Style, true);
                    doc.insertString(doc.getLength(), content + "\n\n", h2Style);
                } else if (line.contains("[H3]") && line.contains("[/H3]")) {
                    String content = line.replace("[H3]", "").replace("[/H3]", "").trim();
                    Style h3Style = doc.addStyle("h3", null);
                    StyleConstants.setFontFamily(h3Style, "Arial");
                    StyleConstants.setFontSize(h3Style, 14);
                    StyleConstants.setBold(h3Style, true);
                    doc.insertString(doc.getLength(), content + "\n", h3Style);
                } else if (line.contains("[LI]") && line.contains("[/LI]")) {
                    String content = line.replace("[LI]", "").replace("[/LI]", "").trim();
                    // Handle bold text in list items
                    content = content.replace("[B]", "").replace("[/B]", "");
                    // Handle icons
                    if (content.startsWith("[RED_CLOCK_ICON]")) {
                        String after = content.substring("[RED_CLOCK_ICON]".length());
                        doc.insertString(doc.getLength(), "• ", defaultStyle);
                        insertIconAndText(doc, after, new ReminderClockIcon(9, 30, ReminderClockIcon.State.OVERDUE, false), defaultStyle);
                    } else if (content.startsWith("[YELLOW_CLOCK_ICON]")) {
                        String after = content.substring("[YELLOW_CLOCK_ICON]".length());
                        doc.insertString(doc.getLength(), "• ", defaultStyle);
                        insertIconAndText(doc, after, new ReminderClockIcon(9, 30, ReminderClockIcon.State.DUE_SOON, false), defaultStyle);
                    } else if (content.startsWith("[BLUE_CLOCK_ICON]")) {
                        String after = content.substring("[BLUE_CLOCK_ICON]".length());
                        doc.insertString(doc.getLength(), "• ", defaultStyle);
                        insertIconAndText(doc, after, new ReminderClockIcon(9, 30, ReminderClockIcon.State.FUTURE, false), defaultStyle);
                    } else {
                        doc.insertString(doc.getLength(), "• " + content, defaultStyle);
                    }
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                } else if (line.contains("[P]") && line.contains("[/P]")) {
                    String content = line.replace("[P]", "").replace("[/P]", "").trim();
                    // Handle bold text in paragraphs
                    content = content.replace("[B]", "").replace("[/B]", "");
                    doc.insertString(doc.getLength(), content, defaultStyle);
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                } else if (line.contains("[RED_CLOCK_ICON]") || line.contains("[YELLOW_CLOCK_ICON]") || line.contains("[BLUE_CLOCK_ICON]")) {
                    // Handle icon lines (fallback)
                    String processedLine = line;
                    if (line.contains("[RED_CLOCK_ICON]")) {
                        insertIconAndText(doc, processedLine.replace("[RED_CLOCK_ICON]", ""), new ReminderClockIcon(9, 30, ReminderClockIcon.State.OVERDUE, false), boldStyle);
                    } else if (line.contains("[YELLOW_CLOCK_ICON]")) {
                        insertIconAndText(doc, processedLine.replace("[YELLOW_CLOCK_ICON]", ""), new ReminderClockIcon(9, 30, ReminderClockIcon.State.DUE_SOON, false), boldStyle);
                    } else if (line.contains("[BLUE_CLOCK_ICON]")) {
                        insertIconAndText(doc, processedLine.replace("[BLUE_CLOCK_ICON]", ""), new ReminderClockIcon(9, 30, ReminderClockIcon.State.FUTURE, false), boldStyle);
                    }
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                } else {
                    // Regular paragraphs or other content
                    line = line.replace("[B]", "").replace("[/B]", "");
                    doc.insertString(doc.getLength(), line, defaultStyle);
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static void insertIconAndText(StyledDocument doc, String text, ReminderClockIcon icon, Style style) throws BadLocationException {
        // Insert the icon
        Style iconStyle = doc.addStyle("icon", null);
        StyleConstants.setIcon(iconStyle, icon);
        doc.insertString(doc.getLength(), " ", iconStyle);

        // Insert the text
        doc.insertString(doc.getLength(), text, style);
    }
}