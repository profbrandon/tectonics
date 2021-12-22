
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Color;
import java.awt.Dimension;


public class RegionTest extends JFrame {

    private final RegionPanel mRegionPanel = new RegionPanel();

    public RegionTest() {
        Optional<Pair<Boolean[][], Point>> shape = Region.importRegionShapeFromPNG("region0.png");
        if (shape.isEmpty()) return;

        final Region region = new Region(shape.get().first, shape.get().second.x, shape.get().second.y);
        final Point location = new Point(50, 50);

        final List<Pair<Point, Region>> regions = region.partition();

        System.out.println("# of Subregions: " + regions.size());

        for (final Pair<Point, Region> subRegion : regions) {
            //subRegion.second.refit();
            mRegionPanel.addRegion(subRegion.second, Util.sumPoints(location, subRegion.first));
        }


        add(mRegionPanel);
        pack();
        setVisible(true);
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RegionTest();
        });
    }

    private class RegionPanel extends JPanel {

        private boolean paintBoundingBoxes = true;

        private final List<Pair<Point, Region>> mRegions = new ArrayList<>();

        public RegionPanel() {
            setPreferredSize(new Dimension(400, 400));
            setBackground(new Color(0, 0, 0));
            setVisible(true);
        }

        public void addRegion(final Region region, final Point location) {
            mRegions.add(new Pair<>(location, region));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            for (final Pair<Point, Region> pair : mRegions) {
                paintRegion(g, pair.first, pair.second);
            }
        }

        private void paintRegion(final Graphics g, final Point location, final Region region) {
            final Color prevColor = g.getColor();

            final int width = region.getDimX();
            final int height = region.getDimY();

            g.setColor(Color.GRAY);
            if (paintBoundingBoxes) g.drawRect(location.x, location.y, width - 1, height - 1);

            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    final Optional<Chunk> chunk = region.getChunkAt(j, i);
                    if (chunk.isPresent()) {
                        paintChunk(g, new Point(location.x + j, location.y + i), chunk.get());
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
