import java.awt.Point;

public class Util {
    

    public static boolean areNeighbors(final Point p0, final Point p1) {
        final int dx = Math.abs(p1.x - p0.x);
        final int dy = Math.abs(p1.y - p0.y);

        return dx + dy == 1;
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
}
