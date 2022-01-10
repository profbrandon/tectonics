

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.JPanel;

public class SimulationPanel extends JPanel {
    
    public static enum SimulationRenderMode {
        DEFAULT("Default"),
        HEIGHT_MAP("Height Map"),
        DISTINCT_COLORS("Distinct Colors"),
        BOUNDARIES("Boundaries"),
        BOUNDARY_TYPES("Boundary Types"),
        DISTANCE_GRAPH("Distance Graph");

        final String mStringValue;

        SimulationRenderMode(final String stringValue) {
            mStringValue = stringValue;
        }

        @Override
        public String toString() {
            return mStringValue;
        }
    }

    private Optional<Simulation> mTargetSim = Optional.empty();

    private SimulationRenderMode mDisplayMode = SimulationRenderMode.DEFAULT;


    public SimulationPanel(final int width, final int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
    }

    public void setSim(final Simulation sim) {
        mTargetSim = Optional.of(sim);
        repaint();
    }

    public void setMode(final SimulationRenderMode mode) {
        mDisplayMode = mode;
        repaint();
    }

    public void update() {
        if(mTargetSim.isPresent()) {
            mTargetSim.get().update();
            repaint();
        }
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

        final List<Region> regions = sim.getRegions();

        for (final Region region : regions) {
            final Pair<Float, Float> pair = region.getElevationRange();
            if (pair.first > maxElevation) maxElevation = pair.first;
            if (pair.second < minElevation) minElevation = pair.second;
        }

        int counter = 0;

        for (final Region region : regions) {
            for (int i = 0; i < region.getHeight(); ++i) {
                for (int j = 0; j < region.getWidth(); ++j) {
                    final Point location = Vec.sum(region.getPosition(), Vec.extend(new Point(j, i))).truncate();
                    final Optional<Chunk> chunk = region.getChunkAt(j, i);

                    if (chunk.isPresent()) {
                        switch(mDisplayMode) {
                            case HEIGHT_MAP:
                                g.setColor(Util.heightColor(region.getElevationAt(j, i), maxElevation, minElevation));
                                break;
                            
                            case BOUNDARIES:
                            case BOUNDARY_TYPES:
                            case DEFAULT:
                                g.setColor(chunk.get().getTopRockType().mColor);
                                break;
                        
                            case DISTINCT_COLORS:
                                g.setColor(Color.getHSBColor(counter / (float) regions.size(), 1.0f, 1.0f));
                                break;

                            default:
                                break;
                        }

                        final Point wrapped = sim.getWrappedBox().wrap(location);

                        g.drawLine(wrapped.x, wrapped.y, wrapped.x, wrapped.y);
                    }
                }
            }

            ++counter;
        }

        if (mDisplayMode == SimulationRenderMode.BOUNDARIES) {
            for (final Region region : sim.getRegions()) {
                for (final Point point : region.getBoundary()) {
                    final Point location = 
                        sim.getWrappedBox().wrap(Vec.sum(region.getPosition(), Vec.extend(point)).truncate());

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

                        case STATIONARY:
                            g.setColor(Color.MAGENTA);
                            break;
                    }

                    final Point location =
                        sim.getWrappedBox().wrap(Vec.sum(region.getPosition(), Vec.extend(pair.first)).truncate());
                    g.drawLine(location.x, location.y, location.x, location.y);
                }

                final Vec centroid = region.getCentroid();
                region.getVelocity().paint(g, Color.ORANGE, 150f, centroid.truncate());
            }
        }
    
        if (mDisplayMode == SimulationRenderMode.DISTANCE_GRAPH) {
            final Graph<Region, Pair<Boolean, Float>> graph = sim.getGraph();

            final List<Region> nodes = graph.getNodes();
            final Collection<Pair<Integer, Integer>> edges = graph.getEdges();

            g.setColor(Color.CYAN);

            for (final Pair<Integer, Integer> edge : edges) {
                final Region r1 = nodes.get(edge.first);
                final Region r2 = nodes.get(edge.second);

                final Point c1 = r1.getCentroid().truncate();
                final Point c2 = r2.getCentroid().truncate();

                final Optional<Point> c0 = sim.getWrappedBox().getNonWrappedDuplicates(c2).stream().min((a, b) -> {
                    return Float.compare(Util.distance(a, c1), Util.distance(b, c1));
                });

                g.drawLine(c0.get().x, c0.get().y, c1.x, c1.y);
            }
        }
    }
}
