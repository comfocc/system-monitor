package pl.comfo.monitor;

import oshi.SystemInfo;
import oshi.hardware.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends JFrame {
   private final JLabel cpuLabel = new JLabel();
   private final JLabel ramLabel = new JLabel();
   private final JLabel netLabel = new JLabel();

   private final ChartPanel cpuChart = new ChartPanel("CPU Usage");
   private final ChartPanel ramChart = new ChartPanel("RAM Usage");
   private final ChartPanel netChart = new ChartPanel("Network Activity");

   private final SystemInfo systemInfo = new SystemInfo();
   private final HardwareAbstractionLayer hal = systemInfo.getHardware();
   private final CentralProcessor processor = hal.getProcessor();
   private final GlobalMemory memory = hal.getMemory();
   private final List<NetworkIF> networkIFs = hal.getNetworkIFs();

   private long[] prevTicks = processor.getSystemCpuLoadTicks();
   private long prevBytesSent = 0;
   private long prevBytesRecv = 0;
   private boolean darkMode = true;

   public Main() {
      setTitle("System Monitor");
      setSize(960, 720);
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setLocationRelativeTo(null);
      setLayout(new BorderLayout());

      JMenuBar menuBar = new JMenuBar();
      JMenu viewMenu = new JMenu("View");
      JMenuItem toggleTheme = new JMenuItem("Toggle Dark/Light Mode");
      toggleTheme.addActionListener(e -> switchTheme());
      viewMenu.add(toggleTheme);
      menuBar.add(viewMenu);
      setJMenuBar(menuBar);

      JTabbedPane tabs = new JTabbedPane();
      tabs.setFont(new Font("Segoe UI", Font.PLAIN, 15));

      tabs.addTab("CPU", createPanel(cpuLabel, cpuChart));
      tabs.addTab("RAM", createPanel(ramLabel, ramChart));
      tabs.addTab("Network", createPanel(netLabel, netChart));

      add(tabs, BorderLayout.CENTER);

      Timer timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
         @Override
         public void run() {
            SwingUtilities.invokeLater(Main.this::updateStats);
         }
      }, 0, 1000);

      applyTheme();
   }

   private JPanel createPanel(JLabel label, ChartPanel chart) {
      JPanel panel = new JPanel(new BorderLayout());
      label.setFont(new Font("Consolas", Font.BOLD, 18));
      label.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
      panel.add(label, BorderLayout.NORTH);
      panel.add(chart, BorderLayout.CENTER);
      return panel;
   }

   private void updateStats() {
      double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks);
      prevTicks = processor.getSystemCpuLoadTicks();
      cpuLabel.setText("CPU Usage: " + formatPercent(cpuLoad));
      cpuChart.addDataPoint(cpuLoad);

      long totalMemory = memory.getTotal();
      long usedMemory = totalMemory - memory.getAvailable();
      double ramUsage = (double) usedMemory / totalMemory;
      ramLabel.setText("RAM Usage: " + formatPercent(ramUsage));
      ramChart.addDataPoint(ramUsage);

      long sent = 0, recv = 0;
      for (NetworkIF net : networkIFs) {
         net.updateAttributes();
         sent += net.getBytesSent();
         recv += net.getBytesRecv();
      }
      long deltaSent = sent - prevBytesSent;
      long deltaRecv = recv - prevBytesRecv;
      prevBytesSent = sent;
      prevBytesRecv = recv;

      double netActivity = Math.min(1.0, (deltaSent + deltaRecv) / (10.0 * 1024 * 1024));
      netLabel.setText("Network: " + formatBytes(deltaSent) + " ↑ / " + formatBytes(deltaRecv) + " ↓");
      netChart.addDataPoint(netActivity);
   }

   private String formatPercent(double value) {
      return new DecimalFormat("##0.0%").format(value);
   }

   private String formatBytes(long bytes) {
      if (bytes < 1024) return bytes + " B";
      int exp = (int) (Math.log(bytes) / Math.log(1024));
      char pre = "KMGTPE".charAt(exp - 1);
      return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
   }

   private void switchTheme() {
      darkMode = !darkMode;
      applyTheme();
      cpuLabel.repaint();
      ramLabel.repaint();
      netLabel.repaint();
   }

   private void applyTheme() {
      Color bg = darkMode ? new Color(18, 18, 30) : Color.WHITE;
      Color fg = darkMode ? new Color(220, 220, 230) : Color.BLACK;

      getContentPane().setBackground(bg);

      if (darkMode) {
         Color labelBg = new Color(35, 35, 55);
         Color labelFg = new Color(180, 180, 200);

         cpuLabel.setOpaque(true);
         cpuLabel.setBackground(labelBg);
         cpuLabel.setForeground(labelFg);
         cpuLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

         ramLabel.setOpaque(true);
         ramLabel.setBackground(labelBg);
         ramLabel.setForeground(labelFg);
         ramLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

         netLabel.setOpaque(true);
         netLabel.setBackground(labelBg);
         netLabel.setForeground(labelFg);
         netLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
      } else {
         cpuLabel.setOpaque(false);
         cpuLabel.setBackground(null);
         cpuLabel.setForeground(fg);
         cpuLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

         ramLabel.setOpaque(false);
         ramLabel.setBackground(null);
         ramLabel.setForeground(fg);
         ramLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

         netLabel.setOpaque(false);
         netLabel.setBackground(null);
         netLabel.setForeground(fg);
         netLabel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
      }

      cpuChart.setTheme(darkMode);
      ramChart.setTheme(darkMode);
      netChart.setTheme(darkMode);

      JMenuBar mb = getJMenuBar();
      if (mb != null) {
         mb.setBackground(bg);
         mb.setForeground(fg);
         for (MenuElement menuElement : mb.getSubElements()) {
            if (menuElement instanceof JMenu) {
               JMenu menu = (JMenu) menuElement;
               menu.setForeground(fg);
               for (Component comp : menu.getMenuComponents()) {
                  comp.setForeground(fg);
                  comp.setBackground(bg);
               }
               menu.setBackground(bg);
            }
         }
      }
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main().setVisible(true));
   }
}

class ChartPanel extends JPanel {
   private static final int MAX_POINTS = 60;
   private final LinkedList<Double> data = new LinkedList<>();
   private final String title;
   private boolean darkMode = true;

   public ChartPanel(String title) {
      this.title = title;
      setPreferredSize(new Dimension(800, 220));
      setBackground(new Color(18, 18, 30));
      setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 20));
   }

   public void addDataPoint(double value) {
      if (data.size() >= MAX_POINTS) data.removeFirst();
      data.add(value);
      repaint();
   }

   public void setTheme(boolean darkMode) {
      this.darkMode = darkMode;
      setBackground(darkMode ? new Color(18, 18, 30) : Color.WHITE);
      repaint();
   }

   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();

      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int width = getWidth();
      int height = getHeight();

      if (darkMode) {
         GradientPaint gp = new GradientPaint(0, 0, new Color(22, 22, 42), 0, height, new Color(12, 12, 28));
         g2.setPaint(gp);
         g2.fillRect(0, 0, width, height);
      } else {
         g2.setColor(Color.WHITE);
         g2.fillRect(0, 0, width, height);
      }

      int leftMargin = 50;
      int bottomMargin = 30;
      int topMargin = 30;
      int plotWidth = width - leftMargin - 20;
      int plotHeight = height - topMargin - bottomMargin;

      Color gridColor = darkMode ? new Color(50, 50, 70) : new Color(200, 200, 200);
      Color textColor = darkMode ? new Color(210, 210, 230) : Color.BLACK;
      Color lineColor = darkMode ? new Color(170, 90, 255) : new Color(100, 50, 200);

      g2.setColor(gridColor);
      g2.setStroke(new BasicStroke(1f));

      for (int i = 0; i <= 10; i++) {
         int y = topMargin + (plotHeight * i) / 10;
         g2.drawLine(leftMargin, y, leftMargin + plotWidth, y);
         String label = (100 - i * 10) + "%";
         int strWidth = g2.getFontMetrics().stringWidth(label);
         g2.setColor(textColor);
         g2.drawString(label, leftMargin - strWidth - 8, y + 5);
         g2.setColor(gridColor);
      }

      g2.setColor(darkMode ? new Color(200, 180, 255) : new Color(80, 20, 200));
      g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
      g2.drawString(title, leftMargin, topMargin - 10);

      if (data.isEmpty()) {
         g2.dispose();
         return;
      }

      g2.setColor(lineColor);
      g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      int pointCount = data.size();
      double xStep = plotWidth / (double) MAX_POINTS;
      double prevX = leftMargin;
      double prevY = topMargin + plotHeight * (1 - data.get(0));

      for (int i = 1; i < pointCount; i++) {
         double x = leftMargin + i * xStep;
         double y = topMargin + plotHeight * (1 - data.get(i));
         g2.draw(new Line2D.Double(prevX, prevY, x, y));
         prevX = x;
         prevY = y;
      }

      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
      Path2D.Double area = new Path2D.Double();
      area.moveTo(leftMargin, topMargin + plotHeight);
      for (int i = 0; i < pointCount; i++) {
         double x = leftMargin + i * xStep;
         double y = topMargin + plotHeight * (1 - data.get(i));
         area.lineTo(x, y);
      }
      area.lineTo(leftMargin + (pointCount - 1) * xStep, topMargin + plotHeight);
      area.closePath();
      g2.fill(area);

      g2.dispose();
   }
}
