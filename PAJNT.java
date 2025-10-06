import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class PAJNT extends JFrame {
    private DrawingPanel drawingPanel;

    public PAJNT() {
        setTitle("Aplikacja Do Rysowania");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton rectBtn = new JButton("Prostokąt");
        rectBtn.addActionListener(e -> drawingPanel.setMode(Mode.RECTANGLE));
        toolBar.add(rectBtn);

        JButton circleBtn = new JButton("Okrąg");
        circleBtn.addActionListener(e -> drawingPanel.setMode(Mode.CIRCLE));
        toolBar.add(circleBtn);

        JButton lineBtn = new JButton("Linia");
        lineBtn.addActionListener(e -> drawingPanel.setMode(Mode.LINE));
        toolBar.add(lineBtn);

        JButton polygonBtn = new JButton("Wielokąt");
        polygonBtn.addActionListener(e -> drawingPanel.setMode(Mode.POLYGON));
        toolBar.add(polygonBtn);

        JButton pencilBtn = new JButton("Ołówek");
        pencilBtn.addActionListener(e -> drawingPanel.setMode(Mode.PENCIL));
        toolBar.add(pencilBtn);

        JButton clearBtn = new JButton("Wyczyść");
        clearBtn.addActionListener(e -> drawingPanel.clear());
        toolBar.add(clearBtn);

        JButton saveBtn = new JButton("Zapisz");
        saveBtn.addActionListener(e -> drawingPanel.saveImage());
        toolBar.add(saveBtn);

        JButton loadBtn = new JButton("Wczytaj");
        loadBtn.addActionListener(e -> drawingPanel.loadImage());
        toolBar.add(loadBtn);

        add(toolBar, BorderLayout.NORTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PAJNT().setVisible(true);
        });
    }

    enum Mode {
        RECTANGLE, CIRCLE, LINE, POLYGON, PENCIL
    }

    class DrawingPanel extends JPanel {
        private List<Shape> shapes = new ArrayList<>();
        private Mode currentMode = Mode.LINE;

        private Point startPoint = null;
        private Point currentPoint = null;

        private List<Point> currentPencilPoints = null;

        private List<Point> polygonPoints = new ArrayList<>();
        private final int CLOSE_DISTANCE = 10;

        private BufferedImage loadedImage = null;

        public DrawingPanel() {
            setBackground(Color.WHITE);

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                    currentPoint = startPoint;

                    switch (currentMode) {
                        case PENCIL:
                            currentPencilPoints = new ArrayList<>();
                            currentPencilPoints.add(startPoint);
                            break;
                        case POLYGON:
                            if (!polygonPoints.isEmpty()) {
                                Point first = polygonPoints.get(0);
                                if (startPoint.distance(first) <= CLOSE_DISTANCE) {
                                    shapes.add(new PolygonShape(new ArrayList<>(polygonPoints)));
                                    polygonPoints.clear();
                                    repaint();
                                    return;
                                }
                            }
                            polygonPoints.add(startPoint);
                            repaint();
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (startPoint == null) return;

                    currentPoint = e.getPoint();

                    switch (currentMode) {
                        case RECTANGLE:
                            shapes.add(new RectangleShape(startPoint, currentPoint));
                            break;
                        case CIRCLE:
                            shapes.add(new CircleShape(startPoint, currentPoint));
                            break;
                        case LINE:
                            shapes.add(new LineShape(startPoint, currentPoint));
                            break;
                        case PENCIL:
                            if (currentPencilPoints != null && currentPencilPoints.size() > 1) {
                                shapes.add(new PencilShape(new ArrayList<>(currentPencilPoints)));
                            }
                            currentPencilPoints = null;
                            break;
                        case POLYGON:
                            break;
                    }
                    startPoint = null;
                    currentPoint = null;
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    currentPoint = e.getPoint();

                    if (currentMode == Mode.PENCIL && currentPencilPoints != null) {
                        currentPencilPoints.add(currentPoint);
                    }
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (currentMode == Mode.POLYGON && !polygonPoints.isEmpty()) {
                        currentPoint = e.getPoint();
                        repaint();
                    }
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        public void setMode(Mode mode) {
            currentMode = mode;
            polygonPoints.clear();
            startPoint = null;
            currentPoint = null;
            currentPencilPoints = null;
            repaint();
        }

        public void clear() {
            shapes.clear();
            polygonPoints.clear();
            startPoint = null;
            currentPoint = null;
            currentPencilPoints = null;
            loadedImage = null;
            repaint();
        }

        public void saveImage() {
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            paint(g2d);
            g2d.dispose();

            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    if (!file.getName().toLowerCase().endsWith(".png")) {
                        file = new File(file.getAbsolutePath() + ".png");
                    }
                    ImageIO.write(image, "png", file);
                    JOptionPane.showMessageDialog(this, "Obraz zapisany: " + file.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Błąd zapisu: " + ex.getMessage());
                }
            }
        }

        public void loadImage() {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    loadedImage = ImageIO.read(chooser.getSelectedFile());
                    repaint();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Błąd wczytywania: " + ex.getMessage());
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (loadedImage != null) {
                g.drawImage(loadedImage, 0, 0, this);
            }

            for (Shape s : shapes) {
                s.draw(g);
            }

            Graphics2D g2 = (Graphics2D) g;

            if (startPoint != null && currentPoint != null) {
                g2.setColor(Color.RED);
                switch (currentMode) {
                    case RECTANGLE:
                        drawRectangle(g2, startPoint, currentPoint);
                        break;
                    case CIRCLE:
                        drawEllipse(g2, startPoint, currentPoint);
                        break;
                    case LINE:
                        g2.drawLine(startPoint.x, startPoint.y, currentPoint.x, currentPoint.y);
                        break;
                    case PENCIL:
                        if (currentPencilPoints != null && currentPencilPoints.size() > 1) {
                            drawPencilPath(g2, currentPencilPoints);
                        }
                        break;
                    case POLYGON:
                        drawPolygon(g2, polygonPoints);
                        if (!polygonPoints.isEmpty()) {
                            Point last = polygonPoints.get(polygonPoints.size() - 1);
                            g2.drawLine(last.x, last.y, currentPoint.x, currentPoint.y);
                        }
                        break;
                }
            } else if (currentMode == Mode.POLYGON && !polygonPoints.isEmpty()) {
                drawPolygon((Graphics2D) g, polygonPoints);
            }
        }

        private void drawRectangle(Graphics g, Point p1, Point p2) {
            int x = Math.min(p1.x, p2.x);
            int y = Math.min(p1.y, p2.y);
            int width = Math.abs(p1.x - p2.x);
            int height = Math.abs(p1.y - p2.y);
            g.drawRect(x, y, width, height);
        }

        private void drawEllipse(Graphics g, Point p1, Point p2) {
            int x = Math.min(p1.x, p2.x);
            int y = Math.min(p1.y, p2.y);
            int width = Math.abs(p1.x - p2.x);
            int height = Math.abs(p1.y - p2.y);
            g.drawOval(x, y, width, height);
        }

        private void drawPolygon(Graphics2D g2, List<Point> points) {
            if (points.size() < 2) return;
            for (int i = 0; i < points.size() - 1; i++) {
                Point a = points.get(i);
                Point b = points.get(i + 1);
                g2.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        private void drawPencilPath(Graphics2D g2, List<Point> points) {
            if (points.size() < 2) return;
            for (int i = 0; i < points.size() - 1; i++) {
                Point a = points.get(i);
                Point b = points.get(i + 1);
                g2.drawLine(a.x, a.y, b.x, b.y);
            }
        }


        abstract class Shape {
            abstract void draw(Graphics g);
        }

        class RectangleShape extends Shape {
            private final Point p1, p2;

            RectangleShape(Point p1, Point p2) {
                this.p1 = p1;
                this.p2 = p2;
            }

            @Override
            void draw(Graphics g) {
                int x = Math.min(p1.x, p2.x);
                int y = Math.min(p1.y, p2.y);
                int width = Math.abs(p1.x - p2.x);
                int height = Math.abs(p1.y - p2.y);
                g.drawRect(x, y, width, height);
            }
        }

        class CircleShape extends Shape {
            private final Point p1, p2;

            CircleShape(Point p1, Point p2) {
                this.p1 = p1;
                this.p2 = p2;
            }

            @Override
            void draw(Graphics g) {
                int x = Math.min(p1.x, p2.x);
                int y = Math.min(p1.y, p2.y);
                int width = Math.abs(p1.x - p2.x);
                int height = Math.abs(p1.y - p2.y);
                g.drawOval(x, y, width, height);
            }
        }

        class LineShape extends Shape {
            private final Point p1, p2;

            LineShape(Point p1, Point p2) {
                this.p1 = p1;
                this.p2 = p2;
            }

            @Override
            void draw(Graphics g) {
                g.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        class PolygonShape extends Shape {
            private final List<Point> points;

            PolygonShape(List<Point> points) {
                this.points = points;
            }

            @Override
            void draw(Graphics g) {
                if (points.size() < 2) return;
                for (int i = 0; i < points.size(); i++) {
                    Point a = points.get(i);
                    Point b = points.get((i + 1) % points.size());
                    g.drawLine(a.x, a.y, b.x, b.y);
                }
            }
        }

        class PencilShape extends Shape {
            private final List<Point> points;

            PencilShape(List<Point> points) {
                this.points = points;
            }

            @Override
            void draw(Graphics g) {
                if (points.size() < 2) return;
                for (int i = 0; i < points.size() - 1; i++) {
                    Point a = points.get(i);
                    Point b = points.get(i + 1);
                    g.drawLine(a.x, a.y, b.x, b.y);
                }
            }
        }
    }
}
