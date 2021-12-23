import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;

import java.io.File;


public class Region {

    /**
     * The x dimension of the region.
     */
    private int mDimX;

    /**
     * The y dimension of the region.
     */
    private int mDimY;

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
    public Region(final int width, final int height) {
        assert width >= 0;
        assert height >= 0;

        mDimY = height;
        mDimX = width;

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
    public Region(final Boolean[][] isPresent, final int width, final int height) {
        this(width, height);

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                if (isPresent[i][j]) {
                    final Chunk chunk = new Chunk();
                    chunk.deposit(new Chunk.Layer(Chunk.RockType.randomRockType(), 100f));
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
    public List<Pair<Point, Region>> partition() {
        final List<BoundingBox> boxes = BoolArrayUtil.partition(toBooleanArray(), mDimX, mDimY);
        final List<Pair<Point, Region>> regions = new ArrayList<>(boxes.size());

        for (final BoundingBox box : boxes) {
            final Region region = new Region(box.mDimensions.x, box.mDimensions.y);

            System.out.println(box);

            for (int i = 0; i < box.mDimensions.y; ++i) {
                final int origI = i + box.mLocation.y;

                for (int j = 0; j < box.mDimensions.x; ++j) {
                    final int origJ = j + box.mLocation.x;
                    final Optional<Chunk> chunk = getChunkAt(origJ, origI);
                    if (chunk.isPresent()) {
                        region.setChunk(j, i, chunk.get());
                        region.setHeightAt(j, i, getHeightAt(origJ, origI));
                    }
                }
            }

            regions.add(new Pair<>(box.mLocation, region));
        }

        return regions;
    }

    /**
     * @param mantleDensity the density of the mantle in kg m^-3
     * @return the displacement of the region in cubic kilometers
     */
    public float reEvaluateHeightMap(final float mantleDensity) {
        float totalDepth = 0f;

        System.out.println(mDimX + ", " + mDimY);
        
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
     * @return the height below the mantle
     */
    public float getHeightAt(final int x, final int y) {
        if (x < 0 || y < 0 || x >= mDimX || y >= mDimY) return 0f;
        else return mHeightMap[y][x];
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
