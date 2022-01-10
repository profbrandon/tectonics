
import java.awt.Point;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class WrappedBox {

    /**
     * The width of the wrapped box
     */
    private final int mWidth;

    /**
     * The height of the wrapped box
     */
    private final int mHeight;

    /**
     * The area of the wrapped box
     */
    private final int mArea;

    /**
     * Constructor to build a new wrapped box.
     * @param width the width of the box
     * @param height the height of the box
     */
    public WrappedBox(final int width, final int height) {
        assert width > 0;
        assert height > 0;

        mWidth  = width;
        mHeight = height;
        mArea   = width * height;
    }

    /**
     * @return the width of the wrapped box
     */
    public int getWidth() { return mWidth; }

    /**
     * @return the height of the wrapped box
     */
    public int getHeight() { return mHeight; }

    /**
     * @return the area of the wrapped box
     */
    public int getArea() { return mArea; }

    /**
     * Builds a collection of points that represent the duplicates of the point
     * in normal (x,y) coordinate space (unwrapped).
     * @param point the point to duplicate
     * @return the duplicated points
     */
    public Set<Point> getNonWrappedDuplicates(final Point point) {
        final Point wrapped = wrap(point);
        final Set<Point> duplicates = new HashSet<>(9);
        duplicates.add(new Point(wrapped.x - mWidth, wrapped.y - mHeight));
        duplicates.add(new Point(wrapped.x,          wrapped.y - mHeight));
        duplicates.add(new Point(wrapped.x + mWidth, wrapped.y - mHeight));

        duplicates.add(new Point(wrapped.x - mWidth, wrapped.y));
        duplicates.add(wrapped);
        duplicates.add(new Point(wrapped.x + mWidth, wrapped.y));

        duplicates.add(new Point(wrapped.x - mWidth, wrapped.y + mHeight));
        duplicates.add(new Point(wrapped.x,          wrapped.y + mHeight));
        duplicates.add(new Point(wrapped.x + mWidth, wrapped.y + mHeight));
        return duplicates;
    }

    /**
     * Builds a collection of vectors that represent the duplicates of the vector
     * in normal (x,y) coordinate space (unwrapped).
     * @param vec the vector to duplicate
     * @return the duplicated vectors
     */
    public Set<Vec> getNonWrappedDuplicates(final Vec vec) {
        final Vec wrapped = wrap(vec);
        final Set<Vec> duplicates = new HashSet<>(9);
        duplicates.add(new Vec(wrapped.x - mWidth, wrapped.y - mHeight));
        duplicates.add(new Vec(wrapped.x,          wrapped.y - mHeight));
        duplicates.add(new Vec(wrapped.x + mWidth, wrapped.y - mHeight));

        duplicates.add(new Vec(wrapped.x - mWidth, wrapped.y));
        duplicates.add(wrapped);
        duplicates.add(new Vec(wrapped.x + mWidth, wrapped.y));

        duplicates.add(new Vec(wrapped.x - mWidth, wrapped.y + mHeight));
        duplicates.add(new Vec(wrapped.x,          wrapped.y + mHeight));
        duplicates.add(new Vec(wrapped.x + mWidth, wrapped.y + mHeight));
        return duplicates;
    }

    /**
     * Method to wrap a point into the box's dimensions.
     * @param point the point to wrap
     * @return the wrapped point to within x in [0,width) and y in [0,height)
     */
    public Point wrap(final Point point) {
        return new Point(Math.floorMod(point.x, mWidth), Math.floorMod(point.y, mHeight));
    }

    /**
     * Method to wrap a vector into the box's dimensions.
     * @param vec the vector to wrap
     * @return the wrapped point to within x in [0,width-1] and y in [0,height-1]
     */
    public Vec wrap(final Vec vec) {
        return new Vec(Util.mod(vec.x, mWidth), Util.mod(vec.y, mHeight));
    }
    
    /**
     * Sums points and wraps them back into the wrapped box context.
     * @param ps the points to sum
     * @return the wrapped sum
     */
    public Point wrapSum(final Point...ps) {
        return wrap(Util.sumPoints(ps));
    }

    /**
     * Determines the distance between two points in the wrapped context.
     * @param point1 the first point
     * @param point2 the second point
     * @return the distance between them
     */
    public float distance(final Point point1, final Point point2) {
        final Point wrapped2 = wrap(point2);
        return getNonWrappedDuplicates(point1).stream()
            .map(p -> Util.distance(p, wrapped2))
            .min(Float::compare)
            .get();
    }

    /**
     * Determines the distance between two vectors in the wrapped context.
     * @param vec1 the first vector
     * @param vec2 the second vector
     * @return the distance between them
     */
    public float distance(final Vec vec1, final Vec vec2) {
        final Vec wrapped2 = wrap(vec2);
        return getNonWrappedDuplicates(vec1).stream()
            .map(v -> Vec.sum(v, wrapped2.negate()).len())
            .min(Float::compare)
            .get();
    }

    /**
     * Method to retrieve the immediate neighbors of the point.
     * @param point the point whose neighbors are computed
     * @return the wrapped neighbors
     */
    public Set<Point> getNeighbors(final Point point) {
        final Set<Point> neighbors = new HashSet<>();
        neighbors.add(wrap(new Point(point.x + 1, point.y)));
        neighbors.add(wrap(new Point(point.x - 1, point.y)));
        neighbors.add(wrap(new Point(point.x, point.y + 1)));
        neighbors.add(wrap(new Point(point.x, point.y - 1)));
        return neighbors;
    }

    /**
     * Method to retrieve the 8 neighbors of the point.
     * @param point the point whose 8 neighbors are computed
     * @return the wrapped neighbors
     */
    public Set<Point> get8Neighbors(final Point point) {
        final Set<Point> neighbors = getNeighbors(point);
        neighbors.add(wrap(new Point(point.x + 1, point.y + 1)));
        neighbors.add(wrap(new Point(point.x + 1, point.y - 1)));
        neighbors.add(wrap(new Point(point.x - 1, point.y + 1)));
        neighbors.add(wrap(new Point(point.x - 1, point.y - 1)));
        return neighbors;
    }

    /**
     * Method to obtain all points within a certain radius of the given point.
     * @param point the point to get a cluster around
     * @return the cluster of points
     */
    public Set<Point> getCluster(final Point point, final int radius) {
        final Set<Point> neighbors = new HashSet<>();

        for (int i = -radius; i <= radius; ++i) {
            for (int j = -radius; j <= radius; ++j) {
                final Point test = new Point(point.x + j, point.y + i);
                if (distance(point, test) <= radius) {
                    neighbors.add(wrap(test));
                }
            }
        }

        neighbors.remove(wrap(point));
        return neighbors;
    }

    /**
     * <p>Method to retrieve the immediate neighbors of the collection of {@link Point}s,
     * excluding all of the original points, specifically, if P is the set of
     * points, then</p>
     * 
     * <p>ExclusiveNeighbors(P) = U{p in P | Neighbors(p)} \ P</p>
     * @param points the points to examine
     * @return the collection of neighbor points that are not contained in the
     *         original set of points
     */
    public Set<Point> getExclusiveNeighbors(final Collection<Point> points) {
        final Set<Point> neighbors = new HashSet<>();
        for (final Point point : points) {
            neighbors.addAll(getNeighbors(point));
        }
        neighbors.removeAll(points);
        return neighbors;
    }

    /**
     * Method to determine if a {@link Point} is within a certain wrapped vertical frame.
     * @param xMinFrame the leftmost {@code x} position of the vertical frame. Assumed
     *                  to be on the interval {@code [0,width)}.
     * @param frameWidth the width of the frame
     * @param point a point in the space
     * @return whether the point is within the frame
     */
    public boolean withinVerticalFrame(final int xMinFrame, final int frameWidth, final Point point) {
        // The frame encompasses the entire area
        if (frameWidth >= mWidth) return true;

        final int x = wrap(point).x;
        final int xMaxFrame = xMinFrame + frameWidth - 1;

        // The frame does not wrap
        if (xMaxFrame < mWidth) {
            return Util.onInterval(xMinFrame, xMaxFrame, x);
        }

        // The frame wraps
        return Util.onInterval(0, xMaxFrame % mWidth, x) || Util.onInterval(xMinFrame, mWidth, x);
    }

    /**
     * Method to determine if a {@link Point} is within a certain wrapped horizontal frame.
     * @param yMinFrame the uppermost {@code y} position of the horizontal frame. Assumed
     *                  to be on the interval {@code [0,height)}.
     * @param frameWidth the height of the frame
     * @param point a point in the space
     * @return whether the point is within the frame
     */
    public boolean withinHorizontalFrame(final int yMinFrame, final int frameHeight, final Point point) {
        // The frame encompasses the entire area
        if (frameHeight >= mHeight) return true;

        final int y = wrap(point).y;
        final int yMaxFrame = yMinFrame + frameHeight - 1;

        // The frame does not wrap
        if (yMaxFrame < mHeight) {
            return Util.onInterval(yMinFrame, yMaxFrame, y);
        }

        // The frame wraps
        return Util.onInterval(0, yMaxFrame % mHeight, y) || Util.onInterval(yMinFrame, mHeight, y);
    }

    /**
     * Method to determine if a {@link Point} is within a certain wrapped {@link BoundingBox}.
     * @param box the box
     * @param point the point to test
     * @return whether the point is within the box
     */
    public boolean withinBoundingBox(final BoundingBox box, final Point point) {
        final Point upperLeft = wrap(box.mLocation);
        return withinVerticalFrame(upperLeft.x, box.mDimensions.x, point)
            && withinHorizontalFrame(upperLeft.y, box.mDimensions.y, point);
    }

    /**
     * <p>Determines whether the two {@link BoundingBox}es overlap.</p>
     * 
     * <p>Note: Doesn't catch boxes that overlap without containing at least one corner of
     * the other.</p>
     * @param box1 the first box
     * @param box2 the second box
     * @return whether the bounding boxes overlap
     */
    public boolean boundingBoxesOverlap(final BoundingBox box1, final BoundingBox box2) {
        return box1.corners().stream().anyMatch(p -> withinBoundingBox(box2, wrap(p)))
            || box2.corners().stream().anyMatch(p -> withinBoundingBox(box1, wrap(p)));
    }

    /**
     * @param box the unwrapped bounding box
     * @param point the point to check
     * @return an optional of an unwrapped point that is in the box
     */
    public Optional<Point> getUnwrapped(final BoundingBox box, final Point point) {
        for (final Point duplicate : getNonWrappedDuplicates(point)) {
            if (box.contains(duplicate)) {
                return Optional.of(duplicate);
            }
        }

        return Optional.empty();
    }

    /**
     * Determines whether the point is out of the wrapped box's bounds.
     * @param point the point to test
     * @return whether the point is out of bounds
     */
    public boolean outOfBounds(final Point point) {
        return point.x < 0 || point.y < 0 || point.x >= mWidth || point.y >= mHeight;
    }

    /**
     * Determines whether the two points are neighbors in the wrapped context.
     * @param point1 the first point
     * @param point2 the second point
     * @return whether the points are neighbors
     */
    public boolean areNeighbors(final Point point1, final Point point2) {
        return getNeighbors(point1).contains(wrap(point2));
    }

    /**
     * Tests for wrapping equality between points.
     * @param point1 the first point
     * @param point2 the second point
     * @return whether the two points are wrap-equivalent
     */
    public boolean pointEquals(final Point point1, final Point point2) {
        return wrap(point1).equals(wrap(point2));
    }

    /**
     * Determines if the collection contains the point.
     * @param points the collection of points to search
     * @param target the point to find
     * @return whether the point was found in the collection
     */
    public boolean contains(final Collection<Point> points, final Point target) {
        for (final Point point : points) {
            if (pointEquals(point, target)) return true;
        }
        return false;
    }
}
