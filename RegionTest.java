
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.Color;
import java.awt.Dimension;


public class RegionTest extends JFrame {

    private final int width = 300;
    private final int height = 300;

    private final RegionPanel mRegionPanel = new RegionPanel(width, height);

    public RegionTest() {
        /*
        Optional<Pair<Boolean[][], Point>> shape = Region.importRegionShapeFromPNG("region0.png");
        if (shape.isEmpty()) return;

        final Region region = new Region(shape.get().first, shape.get().second.x, shape.get().second.y);
        final Point location = new Point(0, 0);

        final float displacement = region.reEvaluateHeightMap(3400f);

        System.out.println(displacement + " km^3");
        final float chunkWidth = Chunk.WIDTH_IN_KM.toKilometers();

        region.lift(Length.fromKilometers(displacement / region.getDimX() / region.getDimY() / chunkWidth / chunkWidth).toMeters());

        final List<Pair<Point, Region>> regions = region.partition();

        System.out.println("# of Subregions: " + regions.size());

        for (final Pair<Point, Region> subRegion : regions) {
            //subRegion.second.refit();
            mRegionPanel.addRegion(subRegion.second, Util.sumPoints(location, subRegion.first));
        }
        */
        //mRegionPanel.addRegion(region, location);

        for (final Plate plate : Plate.splitArea(width, height, 6)) {
 
            final List<Pair<Point, Region>> pairs = plate.getRegions();

            for (final Pair<Point, Region> pair : pairs) {
                pair.second.reEvaluateHeightMap(3400f);
                mRegionPanel.addRegion(pair.second, pair.first);
            }
        }

        setLayout(new BoxLayout(this.getContentPane(), BoxLayout.PAGE_AXIS));

        final JButton button = new JButton("DEFAULT"); 
        button.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button.setText(mRegionPanel.nextMode().toString());
                mRegionPanel.repaint();
                RegionTest.this.pack();
            }
        });

        add(mRegionPanel);
        add(button);
        pack();
        setVisible(true);
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RegionTest();
        });
    }

    private static class RegionPanel extends JPanel {

        public enum RegionPanelMode {
            DEFAULT,
            HEIGHT_MAP,
            DISTINCT_COLOR,
            REGION_BORDERS
        }

        private boolean paintBoundingBoxes = false;
        private RegionPanelMode mode = RegionPanelMode.REGION_BORDERS;

        private float maxElevation = 0f;
        private float minElevation = 0f;

        private final List<Pair<Point, Region>> mRegions = new ArrayList<>();

        public RegionPanel(final int width, final int height) {
            setPreferredSize(new Dimension(width, height));
            setBackground(new Color(0, 0, 0));
            setVisible(true);
        }

        public void addRegion(final Region region, final Point location) {
            mRegions.add(new Pair<>(location, region));
        }

        public RegionPanelMode nextMode() {
            switch(mode) {
                case DEFAULT:
                    mode = RegionPanelMode.HEIGHT_MAP;
                    break;
                
                case HEIGHT_MAP:
                    mode = RegionPanelMode.DISTINCT_COLOR;
                    break;

                case DISTINCT_COLOR:
                    mode = RegionPanelMode.REGION_BORDERS;
                    break;

                case REGION_BORDERS:
                    mode = RegionPanelMode.DEFAULT;
                    break;
            }

            return mode;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            maxElevation = Float.NEGATIVE_INFINITY;
            minElevation = Float.POSITIVE_INFINITY;

            for (final Pair<Point, Region> pair : mRegions) {
                final Region region = pair.second;

                for (int i = 0; i < region.getDimY(); ++i) {
                    for (int j = 0; j < region.getDimX(); ++j) {
                        final Optional<Chunk> chunk = region.getChunkAt(j, i);
    
                        if (chunk.isPresent()) {
                            final float elevation = chunk.get().getThickness().toMeters() - region.getHeightAt(j, i);
                            if (elevation > maxElevation) maxElevation = elevation;
                            if (elevation < minElevation) minElevation = elevation;
                        }
                    }
                }
            }

            for (int i = 0; i < mRegions.size(); ++i) {
                final Pair<Point, Region> pair = mRegions.get(i);
                paintRegion(g, pair.first, pair.second, i);
            }
        }

        private void paintRegion(final Graphics g, final Point location, final Region region, final int id) {
            final Color prevColor = g.getColor();

            final int width = region.getDimX();
            final int height = region.getDimY();

            g.setColor(Color.GRAY);
            if (paintBoundingBoxes) {
                g.drawRect(location.x, location.y, width - 1, height - 1);
            }

            final Color color = Color.getHSBColor(id / (float) mRegions.size(), 1.0f, 1.0f);
            final List<Point> border = region.getBoundary();

            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    final Optional<Chunk> chunk = region.getChunkAt(j, i);

                    if (chunk.isPresent()) {
                        final int x = location.x + j;
                        final int y = location.y + i;
                        
                        switch(mode) {
                            case HEIGHT_MAP:
                                final float elevation = chunk.get().getThickness().toMeters() - region.getHeightAt(j, i);
                                final float temp = 0.1f + 0.9f * (elevation - minElevation) / (maxElevation - minElevation);
                                g.setColor(Color.getHSBColor(temp, 1.0f, 1.0f));
                                g.drawLine(x, y, x, y);
                                break;

                            case DISTINCT_COLOR:
                                g.setColor(color);
                                g.drawLine(x, y, x, y);
                                break;

                            case REGION_BORDERS:
                                g.setColor(Color.RED);
                                if (border.contains(new Point(j, i))) {
                                    g.drawLine(x, y, x, y);
                                }
                                break;

                            case DEFAULT:
                            default:
                                paintChunk(g, new Point(x, y), chunk.get());
                        }
                    }
                }
            }

            g.setColor(prevColor);
        }

        private void paintChunk(final Graphics g, final Point location, final Chunk chunk) {
            final Color prevColor = g.getColor();

            switch(chunk.getTopRockType()) {
                case SEDIMENT:
                    g.setColor(new Color(96, 48, 0));
                    break;
                case SEDIMENTARY:
                    g.setColor(new Color(240, 200, 128));
                    break;
                case IGNEOUS:
                    g.setColor(new Color(20, 20, 20));
                    break;
                case METAMORPHIC:
                    g.setColor(new Color(60, 50, 40));
                    break;
            }

            final int x = location.x;
            final int y = location.y;

            g.drawLine(x, y, x, y);

            g.setColor(prevColor);
        }
    }
}
