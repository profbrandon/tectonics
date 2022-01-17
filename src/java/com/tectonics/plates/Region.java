package com.tectonics.plates;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.tectonics.util.Vec;
import com.tectonics.util.BoolArrayUtil;
import com.tectonics.util.Pair;
import com.tectonics.util.Util;
import com.tectonics.util.BoundingBox;
import com.tectonics.util.Console;
import com.tectonics.util.Length;


/**
 * This class represents what is meant to be a small area of the simulation that
 * locally corresponds to similar movement behavior. The idea is that while plates
 * act (to some extent) as a single entity, it is the local behavior of "regions"
 * that dictate the behavior of the plate. There are several examples that inspire
 * a distinction between plates and regions:
 * 
 * - Plates can develop internal rift zones
 * - Islands on oceanic plates are accreted at subduction zones
 * - The same plate can be measured to move at different speeds in different places
 * - Folding requires that the rest of the plate helps compress the region directly
 *   adjacent to a collision zone
 */
public class Region {

    public static enum BoundaryType {
        CONVERGENT,
        DIVERGENT,
        TRANSFORM,
        STATIONARY
    }

    private static final float DIVISION_RATIO = 0.003f;

    /**
     * The x dimension of the region.
     */
    private int mWidth;

    /**
     * The y dimension of the region.
     */
    private int mHeight;

    /**
     * The region's position.
     */
    private Vec mPosition;

    /**
     * The region's velocity.
     */
    private Vec mVelocity = Vec.ZERO;

    /**
     * The chunks associated with this region.
     */
    private List<List<Optional<Chunk>>> mChunks;

    /**
     * The height map for the region, specifying the chunk's height
     * below the "mantle" in meters.
     */
    private float[][] mHeightMap;

    /**
     * Creates a region of the specified size 
     * @param width the width
     * @param height the height
     */
    public Region(final int width, final int height, final Vec position) {
        assert width >= 0;
        assert height >= 0;

        mHeight = height;
        mWidth = width;
        
        mPosition = position;

        mChunks = new ArrayList<>(mHeight);
        mHeightMap = new float[height][width];

        for (int i = 0; i < mHeight; ++i) {
            List<Optional<Chunk>> row = new ArrayList<>(mWidth);

            for (int j = 0; j < mWidth; ++j) {
                row.add(Optional.empty());
                mHeightMap[i][j] = 0f;
            }

            mChunks.add(row);
        }
    }

    /**
     * Builds a region from an array of booleans, placing random chunks where
     * there is a true value in the array.
     * @param isPresent the array determining where chunks are present
     * @param width the width of the array
     * @param height the height of the array
     */
    public Region(final Boolean[][] isPresent, final int width, final int height, final Vec position) {
        this(width, height, position);

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                if (isPresent[i][j]) {
                    final Chunk chunk = new Chunk();
                    final Chunk.Layer layer = new Chunk.Layer(
                        Chunk.RockType.randomRockType(),
                        Length.fromKilometers(1f).toMeters());
                    chunk.deposit(layer);
                    setChunk(j, i, chunk);
                }
            }
        }
    }

    /**
     * @param global a point in global coordinates
     * @return the same point in local coordinates
     */
    public Point toLocal(final Point global) {
        return Util.subPoints(global, mPosition.truncate());
    }

    /**
     * @param local a point in local coordinates
     * @return the same point in global coordinates
     */
    public Point toGlobal(final Point local) {
        return Util.sumPoints(local, mPosition.truncate());
    }

    /**
     * @return whether this region contains more space than is necessary
     */
    public boolean isMinimumSize() {
        return BoolArrayUtil.isMinimumSize(toBooleanArray(), mWidth, mHeight);
    }

    /**
     * @return whether the region contains its centroid
     */
    public boolean isRoughlyConvex() {
        return contains(toLocal(getCentroid().truncate()));
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return whether the region contains a chunk at (x, y)
     */
    public boolean contains(final int x, final int y) {
        return getChunkAt(x, y).isPresent();
    }

    /**
     * @param local the point in local coordinates
     * @return whether the region contains the point
     */
    public boolean contains(final Point local) {
        return getChunkAt(local.x, local.y).isPresent();
    }

    /**
     * @param x the global x coordinate
     * @param y the global y coordinate
     * @return whether the region contains the point
     */
    public boolean containsGlobal(final int x, final int y) {
        return containsGlobal(new Point(x, y));
    }

    /**
     * @param global the point in global coordinates
     * @return whether the region contains the point
     */
    public boolean containsGlobal(final Point global) {
        return contains(toLocal(global));
    }

    /**
     * @param local a point in local coordinates
     * @return whether the point neighbors the region (but is not contained in it)
     */
    public boolean neighbors(final Point local) {
        return !contains(local)
            && Util.getNeighbors(local)
                .stream()
                .anyMatch(this::onBoundary);
    }

    /**
     * @param global a point in global coordinates
     * @return whether the point neighbors the region (but is not contained in it)
     */
    public boolean neighborsGlobal(final Point global) {
        return neighbors(toLocal(global));
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return whether the point (x, y) is on the boundary of this region
     */
    public boolean onBoundary(final int x, final int y) {
        return onBoundary(new Point(x, y));
    }

    /**
     * @param point the point in local coordinates
     * @return whether the point is on the boundary of this region
     */
    public boolean onBoundary(final Point local) {
        return contains(local)
            && Util.getNeighbors(local)
                .stream()
                .anyMatch(neighbor -> !contains(neighbor));
    }

    /**
     * @return the local x dimension (width)
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * @return the local y dimension (height)
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * @return the position in global coordinates of the region
     */
    public Vec getPosition() {
        return mPosition;
    }

    /**
     * @return the velocity of the region
     */
    public Vec getVelocity() {
        return mVelocity;
    }

    /**
     * @return the centroid in global coordinates
     */
    public Vec getCentroid() {
        final List<Point> points = getPoints();

        Vec sum = Vec.ZERO;

        for (final Point p : points) {
            sum = Vec.sum(sum, Vec.extend(p));
        }

        return Vec.sum(getPosition(), Vec.scale(sum, 1f / points.size()));
    }

    /**
     * @return a boolean array representation where a true corresponds to a present
     *         chunk and a false corresponds to an absent one. This is becuase many
     *         operations do not need to work with the actual chunk data.
     */
    public Boolean[][] toBooleanArray() {
        Boolean[][] rvalue = new Boolean[mHeight][mWidth];

        for (int i = 0; i < mHeight; ++i) {
            for (int j = 0; j < mWidth; ++j) {
                rvalue[i][j] = contains(j, i);
            }
        }

        return rvalue;
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return the potential chunk at that coordinate
     */
    public Optional<Chunk> getChunkAt(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mWidth || y >= mHeight) return Optional.empty();
        else return mChunks.get(y).get(x);
    }

    /**
     * @param local a point in local coordinates
     * @return the potential chunk at that local point
     */
    public Optional<Chunk> getChunkAt(final Point local) {
        return getChunkAt(local.x, local.y);
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return the height below the mantle in meters
     */
    public float getDepthAt(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mWidth || y >= mHeight) return 0f;
        else return mHeightMap[y][x];
    }

    /**
     * @param local a point in local coordinates
     * @return the height below the mantle in meters
     */
    public float getDepthAt(final Point local) {
        return getDepthAt(local.x, local.y);
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return the elevation in meters
     */
    public float getElevationAt(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mWidth || y >= mHeight) return 0f;

        final Optional<Chunk> chunk = getChunkAt(x, y);

        if (chunk.isPresent()) {
            return chunk.get().getThickness().toMeters() - getDepthAt(x, y);
        }
        
        return 0f;
    }

    /**
     * @param local a point in local coordinates
     * @return the elevation in meters
     */
    public float getElevationAt(final Point local) {
        return getElevationAt(local.x, local.y);
    }

    /**
     * @return the a pair of elevations (max, min) in meters
     */
    public Pair<Float, Float> getElevationRange() {
        final List<Float> elevations = getPoints()
            .stream()
            .map(this::getElevationAt)
            .collect(Collectors.toList());

        final Optional<Float> possibleMax = elevations.stream().max(Float::compare);
        final Optional<Float> possibleMin = elevations.stream().min(Float::compare);

        if (possibleMax.isPresent() && possibleMin.isPresent()) {
            return new Pair<>(possibleMax.get(), possibleMin.get());
        }

        return new Pair<>(0f, 0f);
    }

    /**
     * @return a collection of all of the present points paired with their chunks
     */
    public List<Pair<Point, Chunk>> getChunkPairs() {
        final List<Pair<Point, Chunk>> pairs = new ArrayList<>();

        for (int i = 0; i < mHeight; ++i) {
            for (int j = 0; j < mWidth; ++j) {
                final Point point = new Point(j, i);
                final Optional<Chunk> chunk = getChunkAt(point);

                if (chunk.isPresent()) {
                    pairs.add(new Pair<>(point, chunk.get()));
                }
            }
        }

        return pairs;
    }

    /**
     * Note: The points are in local coordinates
     * @return the points that make up this region
     */
    public List<Point> getPoints() {
        final List<Point> points = new ArrayList<>();
        final Boolean[][] isPresent = toBooleanArray();

        for (int i = 0; i < mHeight; ++i) {
            for (int j = 0; j < mWidth; ++j) {
                if (isPresent[i][j]) {
                    points.add(new Point(j, i));
                }
            }
        }

        return points;
    }

    /**
     * @return the points that make up this region (in global coordinates)
     */
    public List<Point> getGlobalPoints() {
        return getPoints()
            .stream()
            .map(this::toGlobal)
            .collect(Collectors.toList());
    }

    /**
     * Note: The points are in local coordinates
     * @return the points that make up the boundary of the region
     */
    public List<Point> getBoundary() {
        return getPoints()
            .stream()
            .filter(this::onBoundary)
            .collect(Collectors.toList());
    }

    /**
     * @return the points that make up the boundary (in local coordinates)
     */
    public List<Point> getGlobalBoundary() {
        return getBoundary()
            .stream()
            .map(this::toGlobal)
            .collect(Collectors.toList());
    }

    /**
     * Note: the points returned are in local coordinates
     * @return the points that make up the neighboring area of the region
     */
    public List<Point> getNeighbors() {
        return getBoundary()
            .stream()
            .flatMap(point -> Util.getNeighbors(point).stream())
            .filter(point -> !contains(point))
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * @return the points that make up the neighboring area of the region
     */
    public List<Point> getGlobalNeighbors() {
        return getNeighbors().stream().map(this::toGlobal).collect(Collectors.toList());
    }

    /**
     * See 'shadow-example.png'.
     * @return a list of shadows of the local boundary
     */
    public List<List<Point>> getShadows() {
        final List<List<Point>> shadows = new ArrayList<>(4);
        final List<Point> boundary = getBoundary();

        for (final Point direction : Util.DIRECTIONS) {
            final List<Point> shadow = new ArrayList<>();

            for (final Point boundaryPoint : boundary) {
                final Point shadowPoint = Util.subPoints(boundaryPoint, direction);

                if (!contains(shadowPoint)) {
                    shadow.add(shadowPoint);
                }
            }

            shadows.add(shadow);
        }

        return shadows;
    }

    /**
     * See 'shadow-example.png'.
     * @return a list of shadows of the global boundary
     */
    public List<List<Point>> getGlobalShadows() {
        return getShadows()
            .stream()
            .map(shadow -> shadow
                .stream()
                .map(this::toGlobal)
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    /**
     * @return The bounding box of this region in local coordinates
     */
    public BoundingBox getBoundingBox() {
        return new BoundingBox(mPosition.truncate(), new Point(mWidth, mHeight));
    }

    /**
     * Sets the chunk at the specified local position
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @param chunk the chunk to place at (x,y)
     */
    public void setChunk(final int x, final int y, final Chunk chunk) {
        if (x < 0 || y < 0 || x >= mWidth || y >= mHeight) {
            final List<Pair<Point, Chunk>> chunkPairs = getChunkPairs();
            chunkPairs.add(new Pair<>(new Point(x, y), chunk));
            overwrite(Region.buildRegion(chunkPairs, getPosition()));
        }
        else {
            final List<Optional<Chunk>> row = mChunks.get(y);
            row.remove(x);
            row.add(x, Optional.of(chunk));
        }
    }

    /**
     * Sets the chunk at the specified local position
     * @param local a point in local coordinates
     * @param chunk the chunk to place at the point
     */
    public void setChunk(final Point local, final Chunk chunk) {
        setChunk(local.x, local.y, chunk);
    }

    /**
     * Removes the chunk at the specified position
     * @param x the local x coordinate
     * @param y the local y coordinate
     */
    public void removeChunk(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mWidth || y >= mHeight) return;

        final List<Optional<Chunk>> row = mChunks.get(y);
        row.remove(x);
        row.add(x, Optional.empty());
    }

    /**
     * Removes the chunk at the specified position
     * @param local a point in local coordinates
     */
    public void removeChunk(final Point local) {
        removeChunk(local.x, local.y);
    }

    /**
     * Sets the height of the chunk below the mantle
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @param height the height below the mantle
     */
    private void setDepthAt(final int x, final int y, final float height) {
        if (x < 0 || y < 0 || x >= mWidth || y >= mHeight) return;
        else mHeightMap[y][x] = height;
    }

    /**
     * Sets the height of the chunk below the mantle
     * @param local a point in local coordiantes
     * @param height the height below the mantle
     */
    private void setDepthAt(final Point local, final float height) {
        setDepthAt(local.x, local.y, height);
    }

    /**
     * @param position the new position
     */
    public void setPosition(final Vec position) {
        mPosition = position;
    }

    /**
     * @param velocity the new velocity
     */
    public void setVelocity(final Vec velocity) {
        mVelocity = velocity;
    }

    /**
     * Lifts the region by the specified amount
     * @param dz the vertical displacement in meters
     */
    public void lift(final float dz) {
        for (int i = 0; i < mHeight; ++i) {
            for (int j = 0; j < mWidth; ++j) {
                if (contains(j, i)) {
                    mHeightMap[i][j] -= dz;
                }
            }
        }
    }

    /**
     * Replaces all of the internal information of this region with
     * that of the given region.
     * @param region the region to copy to this region
     */
    public void overwrite(final Region region) {
        mWidth     = region.mWidth;
        mHeight    = region.mHeight;
        mPosition  = region.mPosition;
        mVelocity  = region.mVelocity;
        mChunks    = region.mChunks;
        mHeightMap = region.mHeightMap;
    }

    /**
     * @param mantleDensity the density of the mantle in kg m^-3
     * @return the displacement of the region in cubic kilometers
     */
    public float reEvaluateHeightMap(final float mantleDensity) {
        float totalDepth = 0f;

        for (int i = 0; i < mHeight; ++i) {
            for (int j = 0; j < mWidth; ++j) {
                final Optional<Chunk> temp = getChunkAt(j, i);

                if (temp.isPresent()) {
                    final Chunk chunk = temp.get();
                    final float amountSunkMeters = Chunk.depthSunk(chunk, mantleDensity).toMeters();

                    mHeightMap[i][j] = amountSunkMeters;
                    totalDepth += amountSunkMeters; 
                }
            }
        }

        final float s = Chunk.WIDTH_IN_KM.toKilometers();

        return Length.fromMeters(totalDepth).toKilometers() * s * s;
    }

    /**
     * Partitions the region into contiguous regions
     * @return a collection of regions paired with their old local coordinates
     */
    public List<Region> partition() {
        final List<List<Point>> pointCollections = BoolArrayUtil.partition(toBooleanArray(), mWidth, mHeight);
        final List<Region> regions = new ArrayList<>(pointCollections.size());

        for (final List<Point> regionPoints : pointCollections) {
            final List<Pair<Point, Chunk>> pairs = new ArrayList<>();

            for (final Point point : regionPoints) {
                // Note the point is global with respect to the parent region
                final Optional<Chunk> chunk = getChunkAt(point);

                if (chunk.isPresent()) {
                    pairs.add(new Pair<>(point, chunk.get()));
                }
            }

            final Region region = Region.buildRegion(pairs, mPosition);
            
            for (final Point point : regionPoints) {
                if (getChunkAt(point).isPresent()) {
                    region.setDepthAt(region.toLocal(point), getDepthAt(point));
                }
            }

            regions.add(region);
        }

        return regions;
    }

    /**
     * Divides the region into voroni cells
     * @return a collection of regions paired with their old local coordinates
     */
    public List<Region> divide() {
        final List<Point> points = getPoints();
        final int numberOfCentroids = 1 + (int) (DIVISION_RATIO * points.size());

        final List<Pair<Integer, Point>> centroids = new ArrayList<>();
        final List<List<Pair<Point, Chunk>>> groups = new ArrayList<>(numberOfCentroids);

        // Pick "centroid" points
        for (int i = 0; i < numberOfCentroids; ++i) {
            final Point chosen = points.get((int) (Math.random() * points.size()));
            centroids.add(new Pair<>(i, chosen));
            points.remove(chosen);
            groups.add(new ArrayList<>());
            groups.get(i).add(new Pair<>(chosen, getChunkAt(chosen).get()));
        }

        // Collect points nearest each "centroid"
        for (final Point p : points) {
            final int tag = centroids.stream()
                .map(pair -> new Pair<>(pair.first, Util.distance(p, pair.second)))
                .min((x, y) -> Float.compare(x.second, y.second))
                .get().first;

            groups.get(tag).add(new Pair<>(p, getChunkAt(p).get()));
        }

        // Build initial regions
        final List<Region> regions = new ArrayList<>(numberOfCentroids);

        for (int i = 0; i < numberOfCentroids; ++i) {
            regions.add(Region.buildRegion(groups.get(i), mPosition));
        }

        return regions;
    }

    /**
     * Creates a pair of a region and its upper left corner in the old coordinate system.
     * @param chunkPairs the pairs to build a region from in old coordinates
     * @param position the position of the upper left corner of the original (0,0)
     * @return the generated region
     */
    public static Region buildRegion(final List<Pair<Point, Chunk>> chunkPairs, final Vec position) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final Pair<Point, Chunk> chunk : chunkPairs) {
            if (chunk.first.x < minX) minX = chunk.first.x;
            if (chunk.first.y < minY) minY = chunk.first.y;
            if (chunk.first.x > maxX) maxX = chunk.first.x;
            if (chunk.first.y > maxY) maxY = chunk.first.y;
        }

        final int dimX = maxX - minX + 1;
        final int dimY = maxY - minY + 1;
        final Region region = new Region(dimX, dimY, Vec.sum(position, new Vec(minX, minY)));

        for (final Pair<Point, Chunk> chunk : chunkPairs) {
            region.setChunk(chunk.first.x - minX, chunk.first.y - minY, chunk.second);
        }

        return region;
    }

    /**
     * Imports a shape from a png, converting it to a boolean matrix
     * @param filename the file to read from
     * @return the matrix and its dimensions
     */
    public static Optional<Region> importRegionShapeFromPNG(final String filename) {
        final Console console = new Console();
        console.startProgressBar("Loading Image From PNG", 4);

        try {
            console.updateProgressBar("Reading image into buffer");
            final BufferedImage image = ImageIO.read(new File(filename));

            console.updateProgressBar("Constructing chunks");
            final int width  = image.getWidth();
            final int height = image.getHeight();

            final List<Pair<Point, Chunk>> chunkPairs = new ArrayList<>();

            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    final int rgb = image.getRGB(j, i);
                    final int total = rgb & 0xFF + (rgb >> 8) & 0xFF + (rgb >> 16) & 0xFF;

                    if (Math.round(((float) total) / 3f) != 0) {
                        final Chunk chunk = new Chunk();
                        chunk.deposit(new Chunk.Layer(Chunk.RockType.randomRockType(), 1));
                        chunkPairs.add(new Pair<>(new Point(j, i), chunk));
                    }
                }
            }

            console.updateProgressBar("Building region from chunks");
            return Optional.of(buildRegion(chunkPairs, Vec.ZERO));

        } catch (final IOException exception) {
            console.failProgressBar("Could not open image.");
            return Optional.empty();
        } finally {
            console.completeProgressBar();
        }
    }

    /**
     * Exports the region's shape to a png file
     * @param filePath the relative file path to export to
     * @param region the region to write
     * @return whether the write was successful
     */
    public static boolean exportRegionShapeToPNG(final String filePath, final Region region) {
        final int width  = region.getWidth();
        final int height = region.getHeight();
        
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics      g     = image.getGraphics();
        final Boolean[][]   array = region.toBooleanArray();

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                g.setColor(array[i][j] ? Color.WHITE : Color.BLACK);
                g.drawLine(j, i, j, i);
            }
        }

        try {
            ImageIO.write(image, "png", new File(filePath));
        } catch(final Exception exception) {
            return false;
        }

        return true;
    }
}
