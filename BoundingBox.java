
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;


public class BoundingBox {
    public final Point mLocation;
    public final Point mDimensions;    

    public BoundingBox(final Point location, final Point dimensions) {
        this.mLocation = location;
        this.mDimensions = dimensions;
    }

    public BoundingBox(final int locX, final int locY, final int width, final int height) {
        this.mLocation = new Point(locX, locY);
        this.mDimensions = new Point(width, height);
    }

    public boolean contains(final Point p) {
        return Util.onInterval(mLocation.x, mLocation.x + mDimensions.x - 1, p.x)
            && Util.onInterval(mLocation.y, mLocation.y + mDimensions.y - 1, p.y);
    }

    public List<Point> corners() {
        final List<Point> cs = new ArrayList<>();
        cs.add(mLocation);
        cs.add(Util.sumPoints(mLocation, mDimensions, new Point(-1, -1)));
        cs.add(Util.sumPoints(mLocation, new Point(mDimensions.x - 1, 0)));
        cs.add(Util.sumPoints(mLocation, new Point(0, mDimensions.y - 1)));
        return cs;
    }

    public static boolean overlaps(final BoundingBox b0, final BoundingBox b1) {
        for (final Point corner : b0.corners()) {
            if (b1.contains(corner)) {
                return true;
            }
        }
        return false;
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
