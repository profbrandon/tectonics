package com.tectonics.util;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;


public class BoundingBox {

    /**
     * The location of the upper-left corner of the box
     */
    public final Point mLocation;

    /**
     * The dimensions of the box
     */
    public final Point mDimensions;

    /**
     * @param location the location of the upper-left corner of the box
     * @param dimensions the dimensions of the box
     */
    public BoundingBox(final Point location, final Point dimensions) {
        assert dimensions.x > 0;
        assert dimensions.y > 0;

        mLocation = location;
        mDimensions = dimensions;
    }

    /**
     * @param locX the left most x coordinate of the box
     * @param locY the upper most y coordinate of the box
     * @param width the width of the box
     * @param height the height of the box
     */
    public BoundingBox(final int locX, final int locY, final int width, final int height) {
        this(new Point(locX, locY), new Point(width, height));
    }

    /**
     * Constructs a bounding box at (0, 0).
     * @param dimensions the dimensions of the box
     */
    public BoundingBox(final Point dimensions) {
        this(new Point(), dimensions);
    }

    /**
     * @param point the point to check
     * @return whether the point is contained within the bounding box
     */
    public boolean contains(final Point point) {
        return overlaps(new BoundingBox(point, new Point(1, 1)));
    }

    /**
     * @param point the point to check
     * @return whether the point is next to (but not contained in) the bounding box
     */
    public boolean nextTo(final Point point) {
        return expandByOne().contains(point) && !contains(point) && !corners().contains(point);
    }

    /**
     * @param box the box to check against
     * @return whether this bounding box overlaps the other
     */
    public boolean overlaps(final BoundingBox box) {
        final Pair<Point, Point> pair1 = getIntervals();
        final Pair<Point, Point> pair2 = box.getIntervals();

        final Point xInt1 = pair1.first;
        final Point xInt2 = pair2.first;
        final Point yInt1 = pair1.second;
        final Point yInt2 = pair2.second;

        return Util.intervalsOverlap(xInt1.x, xInt1.y, xInt2.x, xInt2.y)
            && Util.intervalsOverlap(yInt1.x, yInt1.y, yInt2.x, yInt2.y);
    }

    /**
     * @return the intervals making up the box in ((x0,x1),(y0,y1)) form
     */
    public Pair<Point, Point> getIntervals() {
        return new Pair<>(
            new Point(mLocation.x, mLocation.x + mDimensions.x - 1),
            new Point(mLocation.y, mLocation.y + mDimensions.y - 1));
    }

    /**
     * @return the corners
     */
    public List<Point> corners() {
        final List<Point> cs = new ArrayList<>();
        cs.add(mLocation);
        cs.add(Util.sumPoints(mLocation, mDimensions, new Point(-1, -1)));
        cs.add(Util.sumPoints(mLocation, new Point(mDimensions.x - 1, 0)));
        cs.add(Util.sumPoints(mLocation, new Point(0, mDimensions.y - 1)));
        return cs;
    }

    /**
     * @return The bounding box where all sides are expanded by 1 outwards.
     */
    public BoundingBox expandByOne() {
        return new BoundingBox(
            Util.sumPoints(mLocation, new Point(-1, -1)),
            Util.sumPoints(mDimensions, new Point(2, 2))
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BoundingBox) {
            final BoundingBox b = (BoundingBox) obj;
            return mLocation.equals(b.mLocation) && mDimensions.equals(b.mDimensions);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Box@(" + mLocation.x + ", " + mLocation.y + ") of size (" + mDimensions.x + ", " + mDimensions.y + ")";
    }
}
