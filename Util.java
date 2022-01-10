
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.awt.Point;
import java.awt.Color;


public class Util {
    
    /**
     * @param point1 the first point
     * @param point2 the second point
     * @return whether the two points are neighbors in normal euclidean space
     */
    public static boolean areNeighbors(final Point point1, final Point point2) {
        final int dx = Math.abs(point2.x - point1.x);
        final int dy = Math.abs(point2.y - point1.y);

        return dx + dy == 1;
    }

    /**
     * @param point the point
     * @return the neighbors of the point
     */
    public static List<Point> getNeighbors(final Point point) {
        final List<Point> neighbors = new ArrayList<>();
        neighbors.add(new Point(point.x + 1, point.y));
        neighbors.add(new Point(point.x - 1, point.y));
        neighbors.add(new Point(point.x, point.y + 1));
        neighbors.add(new Point(point.x, point.y - 1));
        return neighbors;
    }

    /**
     * @param ps the points to sum
     * @return the summed points
     */
    public static Point sumPoints(final Point...ps) {
        int totalX = 0;
        int totalY = 0;

        for (final Point p : ps) {
            totalX += p.x;
            totalY += p.y;
        }

        return new Point (totalX, totalY);
    }

    /**
     * @param minuend the point to subtract from
     * @param ps the points to subtract
     * @return the difference
     */
    public static Point subPoints(final Point minuend, final Point...ps) {
        final Point subtrahend = sumPoints(ps);
        return new Point(minuend.x - subtrahend.x, minuend.y - subtrahend.y);
    }

    /**
     * Calculates the distance between two points.
     * @param p1 the first point
     * @param p2 the second point
     * @return the distance between the points
     */
    public static Float distance(final Point p1, final Point p2) {
        final int dx = p2.x - p1.x;
        final int dy = p2.y - p1.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Determines if the point x is on the interval [a,b] or [b,a].
     * @param a the first interval point
     * @param b the second interval point
     * @param x the point to classify
     * @return whether the point x is on the interval
     */
    public static boolean onInterval(final float a, final float b, final float x) {
        return (a <= x && x <= b) || (b <= x && x <= a);
    }

    /**
     * Determines if the point x is on the interval [a,b] or [b,a].
     * @param a the first interval point
     * @param b the second interval point
     * @param x the point to classify
     * @return whether the point x is on the interval
     */
    public static boolean onInterval(final int a, final int b, final int x) {
        return (a <= x && x <= b) || (b <= x && x <= a);
    }

    /**
     * Determines if the two intervals [a1,b1] and [a2,b2] overlap.
     * @param a1 the first-first interval point
     * @param b1 the second-first interval point
     * @param a2 the first-second interval point
     * @param b2 the second-second interval point
     * @return whether the two intervals overlap
     */
    public static boolean intervalsOverlap(final int a1, final int b1, final int a2, final int b2) {
        return onInterval(a1, b1, a2) || onInterval(a1, b1, b2);
    }

    /**
     * Returns the modulus of two numbers
     * @param a the first number
     * @param b the modulus
     * @return the remainder
     */
    public static float mod(final float a, final float b) {
        final float adjustedB = Math.abs(b);

        float result = a;

        while (result < 0f) result += adjustedB;
        while (result > b) result -= adjustedB;

        return result;
    }

    /**
     * @param a the first value
     * @param b the second value
     * @param t a value from [0,1]
     * @return the interpolated value
     */
    public static float interpolate(final float a, final float b, final float t) {
        return (a - b) * t + b;
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
                        final float h0 = interpolate(h01, h00, x / (float) pixelSize);
                        final float h1 = interpolate(h11, h10, x / (float) pixelSize);
                        final float h  = interpolate(h1, h0, y / (float) pixelSize);
                        final float delta = (float) (((Math.random() * 2) - 1) * 0.05 * (maxHeight - minHeight));

                        chunk.deposit(new Chunk.Layer(Chunk.RockType.BASALT, h + delta));
                        chunks.get(i * pixelSize + y).add(chunk);
                    }
                }
            }
        }

        return chunks;
    }

    /**
     * Filters out the chunks using a mask
     * @param chunks the chunks to filter
     * @param mask the mask to create the filter
     * @return the filtered chunks
     */
    public static List<Pair<Point, Chunk>> mask(final List<List<Chunk>> chunks, final Boolean[][] mask) {
        final List<Pair<Point, Chunk>> filtered = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); ++i) {
            final List<Chunk> row = chunks.get(i);

            for (int j = 0; j < row.size(); ++j) {
                if (mask[i][j]) {
                    filtered.add(new Pair<>(new Point(j, i), row.get(j)));
                }
            }
        }

        return filtered;
    }

    /**
     * Produces a color based off of a height value. The heighth of colors is, from low to
     * high, Black, Purple, Blue, Aqua, Green, Yellow, Orange, Red, White
     * @param value the height value
     * @param max the maximum height value
     * @param min the minimum height value
     * @return the color
     */
    public static Color heightColor(final float value, final float max, final float min) {
        final float ratio = 1.0f - (value - min) / (max - min); // 0 when max height, 1 when min

        final float upperVal = 0.75f;
        final float lowerVal = 0.1f;

        if (ratio >= upperVal) {
            return Color.getHSBColor(upperVal, 1.0f, 1.0f - (ratio - upperVal) / (1.0f - upperVal));
        }
        else if (ratio <= lowerVal) {
            return Color.getHSBColor(ratio, ratio / lowerVal, 1.0f);
        }
        else {
            return Color.getHSBColor(ratio, 1.0f, 1.0f);
        }
    }

    /**
     * Obtains a randomly chosen element from a list.
     * Precondition:  Assumes the list is non-empty
     * @param values the list to choose from
     * @return the randomly chosen value.
     */
    public static <T> T randomElement(final List<T> values) {
        return values.get((int) (values.size() * Math.random()));
    }

    /**
     * Compute the average computed value given a list of arguments.
     * @param args the list of arguments
     * @param evaluator the evaluator to run on the arguments
     * @return the average value computed by the evaluator on the list
     */
    public static <T> float averageComputedValue(final Collection<T> args, final Function<T, Float> evaluator) {
        float total = 0.0f;
        for (final T t : args) {
            total += evaluator.apply(t);
        }
        return total / args.size();
    }
}
