

import java.util.Optional;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JPanel;

public class SimulationPanel extends JPanel {
    
    public static enum SimulationRenderMode {
        DEFAULT,
        HEIGHT_MAP,
        BOUNDARIES,
        BOUNDARY_TYPES
    }

    private Optional<Simulation> mTargetSim = Optional.empty();

    private SimulationRenderMode mDisplayMode = SimulationRenderMode.BOUNDARY_TYPES;


    public SimulationPanel(final int width, final int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
    }

    public void setSim(final Simulation sim) {
        mTargetSim = Optional.of(sim);
        mDisplayMode = SimulationRenderMode.DEFAULT;
        repaint();
    }

    public void setMode(final SimulationRenderMode mode) {
        mDisplayMode = mode;
        repaint();
    }

    public SimulationRenderMode getMode() {
        return mDisplayMode;
    }

    public boolean hasSim() {
        return mTargetSim.isPresent();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (!hasSim()) return;

        Simulation sim = mTargetSim.get();

        float maxElevation = Float.NEGATIVE_INFINITY;
        float minElevation = Float.POSITIVE_INFINITY;

        for (final Region region : sim.getRegions()) {
            final Pair<Float, Float> pair = region.getElevationRange();
            if (pair.first > maxElevation) maxElevation = pair.first;
            if (pair.second < minElevation) minElevation = pair.second;
        }

        for (final Region region : sim.getRegions()) {
            for (int i = 0; i < region.getDimY(); ++i) {
                for (int j = 0; j < region.getDimX(); ++j) {
                    final Point location = Vec.sum(region.getPosition(), Vec.extend(new Point(j, i))).truncate();
                    final Optional<Chunk> chunk = region.getChunkAt(j, i);

                    if (chunk.isPresent()) {
                        switch(mDisplayMode) {
                            case HEIGHT_MAP:
                                g.setColor(Util.heightColor(region.getElevationAt(j, i), maxElevation, minElevation));
                                break;
                            
                            case DEFAULT:
                                g.setColor(chunk.get().getTopRockType().getColor());
                                break;
                            
                            default:
                                break;
                        }

                        g.drawLine(location.x, location.y, location.x, location.y);
                    }
                }
            }
        }

        if (mDisplayMode == SimulationRenderMode.BOUNDARIES) {
            for (final Region region : sim.getRegions()) {
                for (final Point point : region.getBoundary()) {
                    final Point location = Vec.sum(region.getPosition(), Vec.extend(point)).truncate();

                    g.setColor(Color.MAGENTA);
                    g.drawLine(location.x, location.y, location.x, location.y);
                }
            }
        }

        if (mDisplayMode == SimulationRenderMode.BOUNDARY_TYPES) {
            for (final Region region : sim.getRegions()) {
                for (final Pair<Point, Region.BoundaryType> pair : sim.getClassifiedBoundary(region)) {
                    switch(pair.second) {
                        case CONVERGENT:
                            g.setColor(Color.GREEN);
                            break;

                        case DIVERGENT:
                            g.setColor(Color.RED);
                            break;

                        case TRANSFORM:
                            g.setColor(Color.YELLOW);
                            break;

                        default:
                            g.setColor(Color.MAGENTA);
                            break;
                    }

                    final Point location = Vec.sum(region.getPosition(), Vec.extend(pair.first)).truncate();
                    g.drawLine(location.x, location.y, location.x, location.y);
                }
            }
        }
    }
}
