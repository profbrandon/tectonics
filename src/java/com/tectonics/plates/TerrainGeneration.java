package com.tectonics.plates;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.awt.Point;

import com.tectonics.util.Util;
import com.tectonics.util.WrappedBox;
import com.tectonics.Simulation;
import com.tectonics.util.BoundingBox;
import com.tectonics.util.Length;
import com.tectonics.util.Vec;

public class TerrainGeneration {
    
    /**
     * Precondition:  Assumes neighbors is non-empty
     * @param point the point in global coordinates
     */
    public static void fillEmptyPoint(final Point point, final WrappedBox wrappedBox, final List<Region> neighbors) {
        final Region selected = Util.randomElement(neighbors);
        
        final BoundingBox selectedBox = selected.getBoundingBox();

        final List<Chunk> chunks = new ArrayList<>();

        for (final Point neighborPoint : wrappedBox.getNeighbors(point)) {
            for (final Region region : neighbors) {
                final Optional<Point> unwrapped = wrappedBox.getUnwrapped(region.getBoundingBox(), neighborPoint);

                if (unwrapped.isPresent()) {
                    final Point unwrappedNeighbor = unwrapped.get();

                    region.getChunkAt(region.toLocal(unwrappedNeighbor)).ifPresent(chunks::add);
                }
            }
        }

        final Length averageThickness = Length.fromMeters(Util.averageComputedValue(chunks, chunk -> chunk.getThickness().toMeters()));

        if (averageThickness.lessThanEquals(Simulation.RUPTURE_THICKNESS)) {
            final Chunk chunk = new Chunk();
            chunk.deposit(new Chunk.Layer(Chunk.RockType.BASALT, averageThickness.scale(0.9f).toMeters()));

            final Point bordering = selected.toLocal(wrappedBox.getUnwrapped(selectedBox.expandByOne(), point).get());

            selected.setChunk(bordering, chunk);
        }
    }

    /**
     * @param width in pixels
     * @param height in pixels
     * @param pixelSize the size of a pixel on a side
     * @param minHeight the min height in meters
     * @param maxHeight the max height in meters
     * @return a matrix of chunks
     */
    public static List<List<Chunk>> generateChunks(
        final int width,
        final int height,
        final int pixelSize,
        final float minHeight,
        final float maxHeight) {

        final Float[][] pixelHeightMap = new Float[height][width];
        final List<List<Chunk>> chunks = new ArrayList<>(height * pixelSize);

        for (int i = 0; i < height * pixelSize; ++i) {
            chunks.add(new ArrayList<>(width * pixelSize));
        }

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                pixelHeightMap[i][j] = (float) Math.random() * (maxHeight - minHeight) + minHeight;
            }
        }

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                final int ii = (i + 1) % height;
                final int jj = (j + 1) % width;

                final float h00 = pixelHeightMap[i][j];
                final float h01 = pixelHeightMap[i][jj];
                final float h10 = pixelHeightMap[ii][j];
                final float h11 = pixelHeightMap[ii][jj];

                for (int y = 0; y < pixelSize; ++y) {
                    for (int x = 0; x < pixelSize; ++x) {
                        final Chunk chunk = new Chunk();
                        final float h0 = Util.interpolate(h01, h00, x / (float) pixelSize);
                        final float h1 = Util.interpolate(h11, h10, x / (float) pixelSize);
                        final float h  = Util.interpolate(h1, h0, y / (float) pixelSize);
                        final float delta = (float) (((Math.random() * 2) - 1) * 0.05 * (maxHeight - minHeight));

                        chunk.deposit(new Chunk.Layer(Chunk.RockType.BASALT, h + delta));
                        chunks.get(i * pixelSize + y).add(chunk);
                    }
                }
            }
        }

        return chunks;
    }
}
