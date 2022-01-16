import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class CrossSectionViewer extends JPanel {

    private float mMantleDensity;
    
    private List<Chunk> mChunks = new ArrayList<>();

    public CrossSectionViewer(final float mantleDensity) {
        mMantleDensity = mantleDensity;
        setPreferredSize(new Dimension(840, 120));
    }

    public CrossSectionViewer(final List<Chunk> chunks, final float mantleDensity) {
        this(mantleDensity);
        mChunks.addAll(chunks);
        setVisible(true);
    }

    public void paintChunk(
        final Graphics g,
        final float vertScale,
        final float raised,
        final int index,
        final Chunk chunk) {

        final int wWidth = getWidth();
        final int wHeight = getHeight();

        final int width = wWidth / mChunks.size();
        final int x = index == 0 ? 0 : width * index + wWidth % width;
        final int baseY = (int) (raised * wHeight);

        float accumulator = 0.0f;

        for (final Chunk.Layer layer : chunk.getLayers()) {
            final float adjustedThickness = vertScale * layer.getThickness().toMeters();
            accumulator += adjustedThickness;

            final int y = (int) (wHeight * (1.0f - accumulator)) - baseY;
            final int height = (int) (wHeight * adjustedThickness);

            g.setColor(layer.mRockType.mColor);
            g.fillRect(x, y, width + (index == 0 ? wWidth % width : 0), height + 1);
        }
    }

    @Override
    public void paint(Graphics g) {
        float maxElevation = 0.0f;
        float maxDepth = 0.0f;

        for (final Chunk chunk : mChunks) {
            final float depth = Chunk.depthSunk(chunk, mMantleDensity).toMeters();
            final float elevation = chunk.getThickness().toMeters() - depth;

            if (depth > maxDepth) maxDepth = depth;
            if (elevation > maxElevation) maxElevation = elevation;
        }

        final float vertScale = 1f / (maxElevation + maxDepth);

        final int wWidth = getWidth();
        final int wHeight = getHeight();

        g.setColor(new Color(180, 180, 255));
        g.fillRect(0, 0, wWidth, (int) (wHeight * 0.25f));

        g.setColor(new Color(250, 120, 0));
        g.fillRect(0, (int) (wHeight * 0.25f), wWidth, (int) (wHeight * 0.75f));

        for (int i = 0; i < mChunks.size(); ++i) {
            final Chunk chunk = mChunks.get(i);
            final float depth = Chunk.depthSunk(chunk, mMantleDensity).toMeters();

            paintChunk(g, vertScale, vertScale * (maxDepth - depth), i, chunk);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final JFrame frame = new JFrame();
            
            final List<Chunk> chunks = new ArrayList<>();
            
            for (int i = 0; i < 50; ++i) {
                chunks.add(new Chunk ());
            }

            for (int i = 0; i < 10; ++i) {
                final Chunk.RockType rockType = Chunk.RockType.randomRockType();

                for (int j = 0; j < chunks.size(); ++j) {
                    chunks.get(j).deposit(new Chunk.Layer(rockType, 2000f + (float) (Math.random() * 100f)));
                }
            }

            frame.add(new CrossSectionViewer(chunks, 3500f));
            frame.pack();

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setVisible(true);
            frame.repaint();
        });
    }
}
