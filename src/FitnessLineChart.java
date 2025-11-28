import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class FitnessLineChart {

    private static final boolean SHOW_POINT_VALUES = true; // hiện số fitness trên từng điểm

    private FitnessLineChart() {}

    public static void show(List<Integer> generations, List<Integer> fitness) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Best Fitness theo Generation (Line)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setSize(1200, 650);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new ChartPanel(generations, fitness));
            frame.setVisible(true);
        });
    }

    public static void savePng(List<Integer> generations, List<Integer> fitness, String filePath) {
        ChartPanel panel = new ChartPanel(generations, fitness);
        panel.setSize(1200, 650);

        BufferedImage image = new BufferedImage(1200, 650, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        try {
            ImageIO.write(image, "png", new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Không lưu được PNG: " + filePath, e);
        }
    }

    private static final class ChartPanel extends JPanel {
        private final List<Integer> gens;
        private final List<Integer> fits;

        ChartPanel(List<Integer> gens, List<Integer> fits) {
            this.gens = gens;
            this.fits = fits;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (gens == null || fits == null || gens.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int left = 80;
            int right = 40;
            int top = 60;
            int bottom = 80;

            int chartW = w - left - right;
            int chartH = h - top - bottom;

            int x0 = left;
            int y0 = top + chartH;

            // ===== TÍNH MIN/MAX FITNESS =====
            int minFit = Integer.MAX_VALUE;
            int maxFit = Integer.MIN_VALUE;
            for (int v : fits) {
                minFit = Math.min(minFit, v);
                maxFit = Math.max(maxFit, v);
            }
            if (minFit == Integer.MAX_VALUE) return;

            int padding = 1; // thấy rõ các mức 41-42
            int yMin = minFit - padding;
            int yMax = maxFit + padding;
            if (yMin == yMax) { yMin--; yMax++; }

            // ===== AXES =====
            g2.setColor(Color.BLACK);
            g2.drawLine(x0, top, x0, y0);
            g2.drawLine(x0, y0, x0 + chartW, y0);

            // ===== TITLE =====
            g2.drawString("Best Fitness theo the he (Line) - Auto Scale Y", left, 30);

            // ===== GRID + Y TICKS (mỗi 1 fitness) =====
            g2.setFont(g2.getFont().deriveFont(12f));
            for (int yTick = yMin; yTick <= yMax; yTick++) {
                int y = mapY(yTick, yMin, yMax, top, chartH);

                // grid line
                g2.setColor(new Color(230, 230, 230));
                g2.drawLine(x0, y, x0 + chartW, y);

                // label
                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(yTick), x0 - 35, y + 4);
            }

            // ===== X LABELS (mỗi 5 gen) =====
            int n = gens.size();
            for (int i = 0; i < n; i++) {
                int gen = gens.get(i);
                if (gen % 5 == 0 || gen == 1 || gen == gens.get(n - 1)) {
                    int x = mapX(i, n, x0, chartW);
                    g2.setColor(new Color(230, 230, 230));
                    g2.drawLine(x, top, x, y0);

                    g2.setColor(Color.BLACK);
                    g2.drawString(String.valueOf(gen), x - 6, y0 + 20);
                }
            }

            // ===== VẼ LINE + POINTS =====
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(220, 20, 60)); // đỏ

            int prevX = -1, prevY = -1;
            for (int i = 0; i < n; i++) {
                int x = mapX(i, n, x0, chartW);
                int y = mapY(fits.get(i), yMin, yMax, top, chartH);

                if (i > 0) g2.drawLine(prevX, prevY, x, y);

                // point
                g2.fillOval(x - 4, y - 4, 8, 8);

                // value label (tùy chọn)
                if (SHOW_POINT_VALUES) {
                    g2.setColor(Color.BLACK);
                    g2.drawString(String.valueOf(fits.get(i)), x - 6, y - 10);
                    g2.setColor(new Color(220, 20, 60));
                }

                prevX = x;
                prevY = y;
            }

            // ===== AXIS TITLES =====
            g2.setColor(Color.BLACK);
            g2.drawString("Generation", x0 + chartW - 70, y0 + 50);
            g2.drawString("Fitness", 20, top + 20);

            // ===== SUMMARY TOP-RIGHT =====
            int last = fits.get(n - 1);
            g2.drawString("Min=" + minFit + " | Max=" + maxFit + " | Last=" + last,
                    x0 + chartW - 220, top - 20);
        }

        private int mapX(int index, int n, int x0, int chartW) {
            if (n <= 1) return x0;
            double t = index / (double) (n - 1);
            return x0 + (int) Math.round(t * chartW);
        }

        private int mapY(int value, int yMin, int yMax, int top, int chartH) {
            double t = (value - yMin) / (double) (yMax - yMin);
            return top + chartH - (int) Math.round(t * chartH);
        }
    }
}
