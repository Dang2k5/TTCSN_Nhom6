import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class RunTimeLineChart {

    // Hiển thị số giây ngay trên mỗi điểm (bật/tắt tùy bạn)
    private static final boolean SHOW_POINT_VALUES = true;

    private RunTimeLineChart() {}

    public static void show(List<Integer> runs, List<Double> timesSec) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Execution Time theo so lan chay (Run)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setSize(1200, 650);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new ChartPanel(runs, timesSec));
            frame.setVisible(true);
        });
    }

    public static void savePng(List<Integer> runs, List<Double> timesSec, String filePath) {
        ChartPanel panel = new ChartPanel(runs, timesSec);
        panel.setSize(1200, 650);

        BufferedImage image = new BufferedImage(1200, 650, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        try {
            ImageIO.write(image, "png", new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Khong luu duoc PNG: " + filePath, e);
        }
    }

    private static final class ChartPanel extends JPanel {
        private final List<Integer> runs;
        private final List<Double> times;

        ChartPanel(List<Integer> runs, List<Double> times) {
            this.runs = runs;
            this.times = times;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (runs == null || times == null || runs.isEmpty() || times.isEmpty()) return;

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

            // ===== TITLE =====
            g2.setColor(Color.BLACK);
            g2.drawString("Execution Time theo Run (Auto Scale Y)", left, 30);

            int n = Math.min(runs.size(), times.size());
            if (n <= 1) return;

            // ===== MIN/MAX TIME =====
            double minT = Double.POSITIVE_INFINITY;
            double maxT = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < n; i++) {
                double t = times.get(i);
                minT = Math.min(minT, t);
                maxT = Math.max(maxT, t);
            }
            if (!Double.isFinite(minT) || !Double.isFinite(maxT)) return;

            // Padding để đẹp hơn
            double pad = Math.max(0.05, (maxT - minT) * 0.10);
            double yMin = Math.max(0.0, minT - pad);
            double yMax = maxT + pad;
            if (Math.abs(yMax - yMin) < 1e-9) {
                yMin = Math.max(0.0, yMin - 0.1);
                yMax = yMax + 0.1;
            }

            // ===== AXES =====
            g2.setColor(Color.BLACK);
            g2.drawLine(x0, top, x0, y0);
            g2.drawLine(x0, y0, x0 + chartW, y0);

            // ===== GRID + Y TICKS (5 mức) =====
            int yTicks = 5;
            g2.setFont(g2.getFont().deriveFont(12f));

            for (int i = 0; i <= yTicks; i++) {
                double tv = yMin + (yMax - yMin) * (i / (double) yTicks);
                int y = mapY(tv, yMin, yMax, top, chartH);

                g2.setColor(new Color(230, 230, 230));
                g2.drawLine(x0, y, x0 + chartW, y);

                g2.setColor(Color.BLACK);
                g2.drawString(String.format("%.2f", tv), x0 - 55, y + 4);
            }

            // ===== X GRID + LABEL (mỗi 5 run) =====
            for (int i = 0; i < n; i++) {
                int run = runs.get(i);
                if (run % 5 == 0 || run == runs.get(0) || run == runs.get(n - 1)) {
                    int x = mapX(i, n, x0, chartW);

                    g2.setColor(new Color(230, 230, 230));
                    g2.drawLine(x, top, x, y0);

                    g2.setColor(Color.BLACK);
                    g2.drawString(String.valueOf(run), x - 6, y0 + 20);
                }
            }

            // ===== LINE + POINTS =====
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(46, 139, 87)); // xanh lá đậm

            int prevX = -1, prevY = -1;
            for (int i = 0; i < n; i++) {
                int x = mapX(i, n, x0, chartW);
                double t = times.get(i);
                int y = mapY(t, yMin, yMax, top, chartH);

                if (i > 0) g2.drawLine(prevX, prevY, x, y);

                g2.fillOval(x - 4, y - 4, 8, 8);

                if (SHOW_POINT_VALUES) {
                    g2.setColor(Color.BLACK);
                    g2.drawString(String.format("%.2f", t), x - 12, y - 10);
                    g2.setColor(new Color(46, 139, 87));
                }

                prevX = x;
                prevY = y;
            }

            // ===== AXIS TITLES =====
            g2.setColor(Color.BLACK);
            g2.drawString("Run", x0 + chartW - 20, y0 + 50);
            g2.drawString("Time (sec)", 10, top + 20);

            // ===== SUMMARY =====
            g2.drawString("Min=" + String.format("%.3f", minT)
                            + " | Max=" + String.format("%.3f", maxT)
                            + " | Last=" + String.format("%.3f", times.get(n - 1)),
                    x0 + chartW - 260, top - 20);
        }

        private int mapX(int index, int n, int x0, int chartW) {
            if (n <= 1) return x0;
            double t = index / (double) (n - 1);
            return x0 + (int) Math.round(t * chartW);
        }

        private int mapY(double value, double yMin, double yMax, int top, int chartH) {
            double t = (value - yMin) / (yMax - yMin);
            return top + chartH - (int) Math.round(t * chartH);
        }
    }
}
