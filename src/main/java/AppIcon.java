import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public final class AppIcon {
    private static volatile Image cachedIcon;

    private AppIcon() {}

    public static Image getAppIcon() {
        Image img = cachedIcon;
        if (img != null) return img;
        synchronized (AppIcon.class) {
            if (cachedIcon != null) return cachedIcon;
            cachedIcon = createIcon();
            return cachedIcon;
        }
    }

    private static Image createIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();

        // Enable anti-aliasing
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Clear background to transparent
        g2.setComposite(java.awt.AlphaComposite.Clear);
        g2.fillRect(0, 0, size, size);
        g2.setComposite(java.awt.AlphaComposite.SrcOver);

        // Define checkbox dimensions (centered)
        int checkboxSize = 24;
        int checkboxX = (size - checkboxSize) / 2;
        int checkboxY = (size - checkboxSize) / 2;

        // Draw subtle shadow
        g2.setColor(new Color(200, 200, 200, 100));
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize, checkboxSize, 6, 6);

        // Draw checkbox outline
        g2.setColor(new Color(120, 120, 120));
        g2.drawRoundRect(checkboxX, checkboxY, checkboxSize, checkboxSize, 6, 6);

        // Fill checkbox with white
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(checkboxX + 1, checkboxY + 1, checkboxSize - 2, checkboxSize - 2, 6, 6);

        // Draw checkmark
        g2.setColor(new Color(76, 175, 80)); // Material green
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int offsetX = checkboxX + 3;
        int offsetY = checkboxY + 6;
        g2.drawLine(offsetX + 2, offsetY + 6, offsetX + 7, offsetY + 11);
        g2.drawLine(offsetX + 7, offsetY + 11, offsetX + 15, offsetY + 1);

        g2.dispose();
        return image;
    }
}
