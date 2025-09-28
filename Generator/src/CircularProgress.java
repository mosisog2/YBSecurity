import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

public class CircularProgress extends JPanel {
    private static final long serialVersionUID = 1L;
    private double percentage = 0.0;
    private String label = "";
    private Color progressColor = new Color(102, 45, 145);  // Default purple
    
    public CircularProgress() {
        setPreferredSize(new Dimension(150, 150));
        setBackground(Color.WHITE);
    }
    
    public void setValue(double percentage, String label) {
        this.percentage = percentage;
        this.label = label;
        repaint();
    }
    
    public void setProgressColor(Color color) {
        this.progressColor = color;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - 20;
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        
        // Draw background circle
        g2d.setColor(new Color(240, 240, 240));
        g2d.setStroke(new BasicStroke(10));
        g2d.drawArc(x, y, size, size, 0, 360);
        
        // Draw progress arc
        g2d.setColor(progressColor);
        Arc2D.Double arc = new Arc2D.Double(x, y, size, size, 90, -360.0 * percentage, Arc2D.OPEN);
        g2d.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(arc);
        
        // Draw percentage text
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String percentText = String.format("%.0f%%", percentage * 100);
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (width - fm.stringWidth(percentText)) / 2;
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(percentText, textX, textY);
        
        // Draw label
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        fm = g2d.getFontMetrics();
        textX = (width - fm.stringWidth(label)) / 2;
        textY = textY + fm.getHeight() + 5;
        g2d.drawString(label, textX, textY);
        
        g2d.dispose();
    }
}