
import java.util.ArrayList;
import java.util.List;

import java.awt.Point;


public class Util {
    

    public static boolean areNeighbors(final Point p0, final Point p1) {
        final int dx = Math.abs(p1.x - p0.x);
        final int dy = Math.abs(p1.y - p0.y);

        return dx + dy == 1;
    }

    public static List<Point> getWrappedNeighbors(final Point p, final Point upperLeft, final Point lowerRight) {
        final List<Point> neighbors = new ArrayList<>(4);

        final int width  = lowerRight.x - upperLeft.x + 1;
        final int height = lowerRight.y - upperLeft.y + 1;

        final int baseX = wrapCoord(p.x - upperLeft.x, width) + upperLeft.x;
        final int left  = wrapCoord(p.x - upperLeft.x - 1, width) + upperLeft.x;
        final int right = wrapCoord(p.x - upperLeft.x + 1, width) + upperLeft.x;

        final int baseY = wrapCoord(p.y - upperLeft.y, height) + upperLeft.y;
        final int above = wrapCoord(p.y - upperLeft.y - 1, height) + upperLeft.y;
        final int below = wrapCoord(p.y - upperLeft.y + 1, height) + upperLeft.y;

        neighbors.add(new Point(baseX, above));
        neighbors.add(new Point(baseX, below));
        neighbors.add(new Point(left,  baseY));
        neighbors.add(new Point(right, baseY));

        return neighbors;
    }

    public static List<Point> getNeighbors(final Point p) {
        final List<Point> neighbors = new ArrayList<>();
        neighbors.add(new Point(p.x + 1, p.y));
        neighbors.add(new Point(p.x - 1, p.y));
        neighbors.add(new Point(p.x, p.y + 1));
        neighbors.add(new Point(p.x, p.y - 1));
        return neighbors;
    }

    public static int wrapCoord(final int value, final int length) {
        if (value < 0) return value + length;
        else if (value >= length) return value - length;
        else return value;
    }

    public static Point sumPoints(final Point...ps) {
        int totalX = 0;
        int totalY = 0;

        for (final Point p : ps) {
            totalX += p.x;
            totalY += p.y;
        }

        return new Point (totalX, totalY);
    }

    public static Float distance(final Point p0, final Point p1) {
        final int dx = p1.x - p0.x;
        final int dy = p1.y - p0.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
