
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

import java.io.File;


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
    private int mDimX;

    /**
     * The y dimension of the region.
     */
    private int mDimY;

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

        mDimY = height;
        mDimX = width;
        
        mPosition = position;

        mChunks = new ArrayList<>(mDimY);
        mHeightMap = new float[height][width];

        for (int i = 0; i < mDimY; ++i) {
            List<Optional<Chunk>> row = new ArrayList<>(mDimX);

            for (int j = 0; j < mDimX; ++j) {
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

        final Chunk.Layer layer = new Chunk.Layer(
            Chunk.RockType.randomRockType(),
            Length.fromKilometers(10f).toMeters());

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                if (isPresent[i][j]) {
                    final Chunk chunk = new Chunk();
                    chunk.deposit(layer);
                    setChunk(j, i, chunk);
                }
            }
        }
    }

    /**
     * @return whether this region contains more space than is necessary
     */
    public boolean isMinimumSize() {
        return BoolArrayUtil.isMinimumSize(toBooleanArray(), mDimX, mDimY);
    }

    /**
     * Refits the region to the minimum size
     * @return the point that is the new top left corner of the array in old coordinates
     */
    public Point refit() {
        if (isMinimumSize()) return new Point(0,0);

        final Boolean[][] array = toBooleanArray();

        int minX = 0;

        for (int j = 0; j < mDimX; ++j, ++minX) {
            boolean somethingInCol = false;

            for (int i = 0; i < mDimY; ++i) {
                somethingInCol |= array[i][j];
            }

            if (somethingInCol) break;
        }

        int minY = 0;

        for (int i = 0; i < mDimY; ++i, ++minY) {
            boolean somethingInRow = false;
            
            for (int j = 0; j < mDimX; ++j) {
                somethingInRow |= array[i][j];
            }

            if (somethingInRow) break;
        }

        int maxX = mDimX - 1;

        for (int j = maxX; j >= 0; --j, --maxX) {
            boolean somethingInCol = false;

            for (int i = 0; i < mDimX; ++i) {
                somethingInCol |= array[i][j];
            }

            if (somethingInCol) break;
        }

        int maxY = mDimY - 1;

        for (int i = maxY; i >= 0; --i, --maxY) {
            boolean somethingInRow = false;

            for (int j = 0; j < mDimX; ++j) {
                somethingInRow |= array[i][j];
            }

            if (somethingInRow) break;
        }

        resize(minX, minY, maxX, maxY);

        return new Point(minX, minY);
    }

    /**
     * Partitions the region into contiguous regions
     * @return a collection of regions paired with their old local coordinates
     */
    public List<Region> partition() {
        final List<List<Point>> pointCollections = BoolArrayUtil.partition(toBooleanArray(), mDimX, mDimY);
        final List<Region> regions = new ArrayList<>(pointCollections.size());

        for (final List<Point> regionPoints : pointCollections) {
            final List<Pair<Point, Chunk>> pairs = new ArrayList<>();

            for (final Point point : regionPoints) {
                final Optional<Chunk> chunk = getChunkAt(point.x, point.y);

                if(chunk.isPresent()) {
                    pairs.add(new Pair<>(point, chunk.get()));
                }
            }

            final Region region = Region.buildRegion(pairs, mPosition);
            
            for (final Point point : regionPoints) {
                if (getChunkAt(point.x, point.y).isPresent()) {
                    final Point pos = region.getPosition().truncate();

                    region.setHeightAt(
                        point.x - pos.x,
                        point.y - pos.y,
                        getHeightAt(point.x, point.y));
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
        final int numberOfCentroids = (int) (DIVISION_RATIO * points.size());

        final List<Pair<Integer, Point>> centroids = new ArrayList<>();
        final List<List<Pair<Point, Chunk>>> groups = new ArrayList<>(numberOfCentroids);

        for (int i = 0; i < numberOfCentroids; ++i) {
            final Point chosen = points.get((int) (Math.random() * points.size()));
            centroids.add(new Pair<>(i, chosen));
            points.remove(chosen);
            groups.add(new ArrayList<>());
            groups.get(i).add(new Pair<>(chosen, getChunkAt(chosen.x, chosen.y).get()));
        }

        for (final Point p : points) {
            final int tag = centroids.stream()
                .map(pair -> new Pair<>(pair.first, Util.distance(p, pair.second)))
                .min((x, y) -> Float.compare(x.second, y.second))
                .get().first;

            groups.get(tag).add(new Pair<>(p, getChunkAt(p.x, p.y).get()));
        }

        final List<Region> regions = new ArrayList<>(numberOfCentroids);

        for (int i = 0; i < numberOfCentroids; ++i) {
            regions.add(Region.buildRegion(groups.get(i), mPosition));
        }

        return regions;
    }

    /**
     * @param mantleDensity the density of the mantle in kg m^-3
     * @return the displacement of the region in cubic kilometers
     */
    public float reEvaluateHeightMap(final float mantleDensity) {
        float totalDepth = 0f;

        for (int i = 0; i < mDimY; ++i) {
            for (int j = 0; j < mDimX; ++j) {
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
     * Lifts the region by the specified amount
     * @param dz the vertical displacement
     */
    public void lift(final float dz) {
        System.out.println("Lifting the region by " + dz + " m");
        for (int i = 0; i < mDimY; ++i) {
            for (int j = 0; j < mDimX; ++j) {
                if (getChunkAt(j, i).isPresent()) {
                    mHeightMap[i][j] -= dz;
                }
            }
        }
    }

    /**
     * Sets the chunk at the specified position
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @param chunk the chunk to place at (x,y)
     */
    public void setChunk(final int x, final int y, final Chunk chunk) {
        List<Optional<Chunk>> row = mChunks.get(y);
        row.remove(x);
        row.add(x, Optional.of(chunk));
    }

    /**
     * Sets the height of the chunk below the mantle
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @param height the height below the mantle
     */
    private void setHeightAt(final int x, final int y, final float height) {
        if (x < 0 || y < 0 || x >= mDimX || y >= mDimY) return;
        else mHeightMap[y][x] = height;
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
     * @param point the point in local coordinates
     * @return whether the region contains the point
     */
    public boolean contains(final Point point) {
        return getChunkAt(point.x, point.y).isPresent();
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return the potential chunk at that coordinate
     */
    public Optional<Chunk> getChunkAt(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mDimX || y >= mDimY) return Optional.empty();
        else return mChunks.get(y).get(x);
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return the height below the mantle in meters
     */
    public float getHeightAt(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mDimX || y >= mDimY) return 0f;
        else return mHeightMap[y][x];
    }

    /**
     * @param x the local x coordinate
     * @param y the local y coordinate
     * @return the elevation in meters
     */
    public float getElevationAt(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mDimX || y >= mDimY) return 0f;

        final Optional<Chunk> chunk = getChunkAt(x, y);

        if (chunk.isPresent()) {
            return chunk.get().getThickness().toMeters() - getHeightAt(x, y);
        }
        
        return 0f;
    }

    /**
     * @return the a pair of elevations (max, min) in meters
     */
    public Pair<Float, Float> getElevationRange() {
        final List<Float> elevations = getPoints().stream().map(point -> {
            return getElevationAt(point.x, point.y);
        }).collect(Collectors.toList());

        final Optional<Float> possibleMax = elevations.stream().max(Float::compare);
        final Optional<Float> possibleMin = elevations.stream().min(Float::compare);

        if (possibleMax.isPresent() && possibleMin.isPresent()) {
            return new Pair<>(possibleMax.get(), possibleMin.get());
        }

        return new Pair<>(0f, 0f);
    }

    /**
     * @return the local x dimension (width)
     */
    public int getDimX() {
        return mDimX;
    }

    /**
     * @return the local y dimension (height)
     */
    public int getDimY() {
        return mDimY;
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
     * Note: The points are in local coordinates
     * @return the points that make up this region
     */
    public List<Point> getPoints() {
        final List<Point> points = new ArrayList<>();
        final Boolean[][] isPresent = toBooleanArray();

        for (int i = 0; i < mDimY; ++i) {
            for (int j = 0; j < mDimX; ++j) {
                if (isPresent[i][j]) {
                    points.add(new Point(j, i));
                }
            }
        }

        return points;
    }

    /**
     * Note: The points are in local coordinates
     * @return the points that make up the boundary of the region
     */
    public List<Point> getBoundary() {
        return getPoints()
            .stream()
            .filter(point -> {
                return Util.getNeighbors(point)
                    .stream().anyMatch(neighbor -> getChunkAt(neighbor.x, neighbor.y).isEmpty());
            })
            .collect(Collectors.toList());
    }

    /**
     * @return The bounding box of this region in local coordinates
     */
    public BoundingBox getBoundingBox() {
        return new BoundingBox(mPosition.truncate(), new Point(mDimX, mDimY));
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
        Boolean[][] rvalue = new Boolean[mDimY][mDimX];

        for (int i = 0; i < mDimY; ++i) {
            for (int j = 0; j < mDimX; ++j) {
                rvalue[i][j] = getChunkAt(j, i).isPresent();
            }
        }

        return rvalue;
    }

    /**
     * Resizes the region to the specified coordinates
     * @param x0 the leftmost x coordinate
     * @param y0 the uppermost y coordinate
     * @param x1 the rightmost x coordinate
     * @param y1 the bottommost y coordinate
     */
    private void resize(final int x0, final int y0, final int x1, final int y1) {
        final int width  = x1 + 1 - x0;
        final int height = y1 + 1 - y0;

        assert width > 0;
        assert height > 0;

        final List<List<Optional<Chunk>>> chunks = new ArrayList<>(height);
        final float[][] heightMap = new float[height][width];

        for (int i = y0; i <= y1; ++i) {
            List<Optional<Chunk>> row = new ArrayList<>(width);

            for (int j = x0; j <= x1; ++j) {
                row.add(getChunkAt(j, i));
                heightMap[i - y0][j - x0] = mHeightMap[i][j];
            }

            chunks.add(row);
        }

        mChunks = chunks;
        mHeightMap = heightMap;
        mDimX = width;
        mDimY = height;
        mPosition = Vec.sum(mPosition, new Vec(x0, y0));
    }

    /**
     * TODO: test this method
     */
    public static Pair<Point, Region> absorb(final Region r0, final Region r1) {
        final Point relative = Vec.sum(r0.getPosition(), r1.getPosition().negate()).truncate();

        final int minX = Math.min(0, relative.x);
        final int minY = Math.min(0, relative.y);
        final int maxX = Math.max(r0.getDimX() - 1, r1.getDimX() - 1 + relative.x);
        final int maxY = Math.max(r0.getDimY() - 1, r1.getDimY() - 1 + relative.y);

        final int newWidth  = maxX - minX + 1;
        final int newHeight = maxY - minY + 1;

        final Boolean[][] isPresent = new Boolean[newHeight][newWidth];
        final Boolean[][] bs0 = r0.toBooleanArray();
        final Boolean[][] bs1 = r1.toBooleanArray();
        
        for (int i = 0; i < newHeight; ++i) {
            for (int j = 0; j < newWidth; ++j) {
                isPresent[i][j] = false;
            }
        }

        for (int i = 0; i < r0.getDimY(); ++i) {
            for (int j = 0; j < r0.getDimX(); ++j) {
                isPresent[i - minY][j - minX] |= bs0[i][j];
            }
        }

        final int relX = Math.max(0, relative.x);
        final int relY = Math.max(0, relative.y);

        for (int i = 0; i < r1.getDimY(); ++i) {
            for (int j = 0; j < r1.getDimX(); ++j) {
                isPresent[i + relY][j + relX] |= bs1[i][j];
            }
        }

        // TODO: Fix the position
        return new Pair<>(new Point(minX, minY), new Region(isPresent, newWidth, newHeight, Vec.ZERO));
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
     * Renders a region into a collection of pixels
     * @param region the region to render
     * @param sideChunksPerPixel the amount of chunks per one side of the pixel
     * @param pixelType the type of the pixel
     * @return a pair consisting of the size of the array and the color array
     */
    public static Pair<Point, Color[][]> renderRegion(final Region region, final int sideChunksPerPixel, final String pixelType) {
        assert sideChunksPerPixel > 0;

        final int width  = region.getDimX();
        final int height = region.getDimY();

        final int pWidth  = width / sideChunksPerPixel;
        final int pHeight = height / sideChunksPerPixel;

        final int square = sideChunksPerPixel * sideChunksPerPixel;

        final Pair<Float, Float> elevationRange = region.getElevationRange();

        final Color[][] pixels = new Color[pHeight][pWidth];
        
        for (int i = 0; i < pHeight; ++i) {
            for (int j = 0; j < pWidth; ++j) {
                int totalR = 0;
                int totalG = 0;
                int totalB = 0;

                for (int ii = 0; ii < sideChunksPerPixel; ++ii) {
                    for (int jj = 0; jj < sideChunksPerPixel; ++jj) {
                        final int x = sideChunksPerPixel * j + jj;
                        final int y = sideChunksPerPixel * i + ii;

                        final Optional<Chunk> chunk = region.getChunkAt(x, y);

                        if (chunk.isPresent()) {
                            if (pixelType.equals("single color")) {
                                totalR += 255;
                                totalG += 255;
                                totalB += 255;
                            }
                            else if (pixelType.equals("height")) {
                                final float fraction = 1f - 0.9f * (region.getElevationAt(x, y) - elevationRange.second)
                                    / (elevationRange.first - elevationRange.second);

                                final Color color = Color.getHSBColor(fraction, 1.0f, 1.0f);
                                
                                totalR += color.getRed();
                                totalG += color.getGreen();
                                totalB += color.getBlue();
                            }
                            else if (pixelType.equals("boundaries")) {
                                if (region.getBoundary().contains(new Point(x, y))) {
                                    totalR += 255;
                                }
                            }
                            else {
                                final Color color = chunk.get().getTopRockType().mColor;

                                totalR += color.getRed();
                                totalG += color.getGreen();
                                totalB += color.getBlue();
                            }
                        }
                    }
                }

                pixels[i][j] = new Color(totalR / square, totalG / square, totalB / square);
            }
        }

        return new Pair<>(new Point(pWidth, pHeight), pixels);
    }

    /**
     * Imports a shape from a png, converting it to a boolean matrix
     * @param filename the file to read from
     * @return the matrix and its dimensions
     */
    public static Optional<Pair<Boolean[][], Point>> importRegionShapeFromPNG(String filename) {
        try {
            final BufferedImage image = ImageIO.read(new File(filename));

            final int width  = image.getWidth();
            final int height = image.getHeight();

            final Boolean[][] shape = new Boolean[width][height];

            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    final int rgb = image.getRGB(j, i);
                    final int total = rgb & 0xFF + (rgb >> 8) & 0xFF + (rgb >> 16) & 0xFF;
                    shape[i][j] = Math.round(((float) total) / 3f) == 0 ? false : true;
                }
            }

            return Optional.of(new Pair<>(shape, new Point(width, height)));

        } catch(final Exception exception) {
            return Optional.empty();
        }
    }

    /**
     * Exports the region's shape to a png file
     * @param filePath the relative file path to export to
     * @param region the region to write
     * @return whether the write was successful
     */
    public static boolean exportRegionShapeToPNG(final String filePath, final Region region) {
        final int width  = region.getDimX();
        final int height = region.getDimY();
        
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
