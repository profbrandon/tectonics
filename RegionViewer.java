
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;


public class RegionViewer extends JFrame {

    public RegionViewer() {
        super("Region Viewer");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        System.out.print("Loading Region... ");

        final Optional<Region> optional = Region.importRegionShapeFromPNG("region0.png");

        if (optional.isEmpty()) {
            System.out.println("Failed");
            dispose();
            return;
        }

        System.out.println("Success");

        final Region region = optional.get();
        final RegionPanel regionPanel = new RegionPanel(region);

        final JComboBox<RegionPanel.ViewingMode> comboBox = new JComboBox<>(RegionPanel.ViewingMode.values());
        comboBox.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                regionPanel.setViewingMode((RegionPanel.ViewingMode) comboBox.getSelectedItem());
            }
        });
        comboBox.setAlignmentX(0f);

        final JButton toggleCentroid = new JButton();
        toggleCentroid.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                regionPanel.toggleCentroid();
                toggleCentroid.setText(regionPanel.isShowingCentroid() ?
                    "Hide Centroid" :
                    "Show Centroid"
                );
            }
        });
        toggleCentroid.setText("Show Centroid");
        toggleCentroid.setAlignmentX(0f);

        final JButton toggleBoundingBox = new JButton();
        toggleBoundingBox.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                regionPanel.toggleBoundingBox();
                toggleBoundingBox.setText(regionPanel.isShowingBoundingBox() ?
                    "Hide Bounds" :
                    "Show Bounds"
                );
            }
        });
        toggleBoundingBox.setText("Show Bounds");
        toggleBoundingBox.setAlignmentX(0f);

        final JTextArea statusArea = new JTextArea();
        statusArea.setLineWrap(true);
        statusArea.setEditable(false);

        final PointSelectionPanel pointSelection = new PointSelectionPanel(point -> {
            final boolean contained = region.containsGlobal(point);
            final boolean onBoundary = region.onBoundary(region.toLocal(point));
            final boolean neighbors = region.neighbors(region.toLocal(point));
            final boolean withinBoundingBox = region.getBoundingBox().contains(point);
            final boolean nextToBoundingBox = region.getBoundingBox().nextTo(point);

            regionPanel.setSelectedPoint(point);

            statusArea.setText(
                "Contained: " + contained + ", " +
                "On Boundary: " + onBoundary + ", " +
                "Neighbors: " + neighbors + ", " + 
                "Within Bounding Box: " + withinBoundingBox + ", " +
                "Adjacent to Bounding Box: " + nextToBoundingBox
            );
        });

        final JPanel commandPanel = new JPanel();
        commandPanel.setPreferredSize(new Dimension(100, 300));
        commandPanel.setLayout(new GridLayout(4, 1));
        commandPanel.add(toggleCentroid);
        commandPanel.add(toggleBoundingBox);
        commandPanel.add(pointSelection);
        commandPanel.add(comboBox);

        setLayout(new BorderLayout());
        add(regionPanel, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.WEST);
        add(statusArea, BorderLayout.SOUTH);
        pack();

        //setResizable(false);
        setVisible(true);
    }

    private static class RegionPanel extends JPanel {

        private enum ViewingMode {
            DEFAULT,
            IS_PRESENT,
            BOUNDARY,
            ELEVATION,
            SHADOWS
        }

        private Optional<Point> mSelected = Optional.empty();

        private Region mRegion;

        private ViewingMode mViewingMode = ViewingMode.DEFAULT;

        private boolean mShowCentroid = false;

        private boolean mShowBoundingBox = false;
        
        public RegionPanel(final Region region) {
            setPreferredSize(new Dimension(region.getWidth() + 20, region.getHeight() + 20));
            setBackground(Color.BLACK);

            mRegion = region;
            mRegion.setPosition(new Vec(10f, 10f));
        }

        public boolean isShowingCentroid() { return mShowCentroid; }

        public boolean isShowingBoundingBox() { return mShowBoundingBox; }

        public void toggleCentroid() {
            mShowCentroid = !mShowCentroid;
            repaint();
        }

        public void toggleBoundingBox() {
            mShowBoundingBox = !mShowBoundingBox;
            repaint();
        }

        public void setSelectedPoint(final Point point) {
            mSelected = Optional.of(point);
            repaint();
        }

        public void setViewingMode(final ViewingMode viewingMode) {
            mViewingMode = viewingMode;
            repaint();
        }

        public void paintCrosshair(final Graphics g, final Point point) {
            final int crosshairSize = 3;
            g.drawLine(point.x - crosshairSize, point.y, point.x + crosshairSize, point.y);
            g.drawLine(point.x, point.y - crosshairSize, point.x, point.y + crosshairSize);
        }

        public void paintRegion(final Graphics g, final Region region) {
            final Color boundingBoxColor = new Color(100, 100, 100, 127);
            final Color centroidColor = new Color(255, 0, 0, 255);
            final Color selectedColor = Color.GREEN;

            for (final Point point : region.getPoints()) {
                final Optional<Chunk> optional = mRegion.getChunkAt(point.x, point.y);
                final int x = point.x + 10;
                final int y = point.y + 10;

                if (optional.isEmpty()) {
                    g.setColor(Color.MAGENTA);
                }
                else {
                    final Chunk chunk = optional.get(); 

                    switch (mViewingMode) {
                        case DEFAULT:
                            g.setColor(chunk.getTopRockType().mColor);
                            break;

                        case IS_PRESENT:
                            g.setColor(new Color(50, 100, 200));
                            break;

                        case BOUNDARY:
                            if (mRegion.onBoundary(point)) {
                                g.setColor(Color.YELLOW);
                                g.drawLine(x, y, x, y);
                            }
                            continue;

                        case ELEVATION:
                            g.setColor(Color.WHITE);
                            break;

                        case SHADOWS:
                            g.setColor(new Color(20, 20, 20));
                            break;
                    }
                }

                g.drawLine(x, y, x, y);
            }

            if (mViewingMode == ViewingMode.BOUNDARY) {
                g.setColor(new Color(255, 100, 0));
                
                for (final Point neighbor : mRegion.getNeighbors()) {
                    final int x = neighbor.x + 10;
                    final int y = neighbor.y + 10;

                    g.drawLine(x, y, x, y);
                }
            }

            if (mViewingMode == ViewingMode.SHADOWS) {
                final List<List<Point>> shadows = region.getGlobalShadows();

                final Color[] shadowColors = {
                    new Color(255,   0,   0, 84),
                    new Color(255, 255,   0, 84),
                    new Color(0,   255,   0, 84),
                    new Color(0,     0, 255, 84)
                };

                for (int i = 0; i < 4; ++i) {
                    g.setColor(shadowColors[i]);
                    
                    for (final Point shadowPoint : shadows.get(i)) {
                        final int x = shadowPoint.x;
                        final int y = shadowPoint.y;

                        g.drawLine(x, y, x, y);
                    }
                }
            }

            if (mShowBoundingBox) {
                g.setColor(boundingBoxColor);
                final BoundingBox box = mRegion.getBoundingBox();
                g.drawRect(box.mLocation.x, box.mLocation.y, box.mDimensions.x - 1, box.mDimensions.y - 1);

                g.setColor(boundingBoxColor.darker().darker());
                final BoundingBox outer = box.expandByOne();
                g.drawRect(outer.mLocation.x, outer.mLocation.y, outer.mDimensions.x - 1, outer.mDimensions.y - 1);
            }

            if (mShowCentroid) {
                g.setColor(centroidColor);
                paintCrosshair(g, mRegion.getCentroid().truncate());
            }

            if (mSelected.isPresent()) {
                g.setColor(selectedColor);
                paintCrosshair(g, mSelected.get());
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            paintRegion(g, mRegion);
        }
    }

    private static class PointSelectionPanel extends JPanel {
        
        public PointSelectionPanel(final Consumer<Point> consumer) {
            setPreferredSize(new Dimension(100, 50));

            final JSpinner xSpinner = new JSpinner();
            xSpinner.setName("x");

            final JSpinner ySpinner = new JSpinner();
            ySpinner.setName("y");

            final JPanel pointPanel = new JPanel();
            pointPanel.setLayout(new GridLayout(1, 2));
            pointPanel.add(xSpinner);
            pointPanel.add(ySpinner);

            final JButton submitButton = new JButton();
            submitButton.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    consumer.accept(new Point((int) xSpinner.getValue(), (int) ySpinner.getValue()));
                }
            });
            submitButton.setText("Submit");

            setLayout(new BorderLayout());
            add(new JLabel("Point:"), BorderLayout.NORTH);
            add(pointPanel, BorderLayout.CENTER);
            add(submitButton, BorderLayout.SOUTH);

            setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RegionViewer();
        });
    }
}
