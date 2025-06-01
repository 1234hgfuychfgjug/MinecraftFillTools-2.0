import javax.swing.*;
import java.awt.*;

public class AcrylicPanel extends JPanel {
    public AcrylicPanel() {
        setBackground(new Color(255, 255, 255, 100));
        setOpaque(false);
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        g2d.dispose();
    }
}