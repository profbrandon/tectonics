
import java.awt.Point;


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
}
