import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.awt.Point;

public class BoolArrayUtil {
    
    /**
     * Determines if this array is the minimum size necessary for storing the truth data.
     * @param array of booleans
     * @param width of the array
     * @param height of the array
     * @return whether the array is the minimum size necessary
     */
    public static boolean isMinimumSize(
        final Boolean[][] array,
        final int width,
        final int height) {

        boolean row0 = false;
        boolean rowM = false;

        for (int j = 0; j < width; ++j) {
            row0 |= array[0][j];
            rowM |= array[height - 1][j];
        }
        
        boolean column0 = false;
        boolean columnN = false;

        for (int i = 0; i < height; ++i) {
            column0 |= array[i][0];
            columnN |= array[i][width - 1];
        }

        return row0 && rowM && column0 && columnN;
    }

    /**
     * Determines if the given array is contiguous
     * @param array of booleans
     * @param width of the array
     * @param height of the array
     * @return whether the array is contiguous
     */
    public static boolean isContiguous(final Boolean[][] array, final int width, final int height) {
        return partition(array, width, height).size() == 1;
    }

    /**
     * Partitions the given boolean array into bounding boxes for contiguous sections.
     * @param array of booleans to parse
     * @param width of the array
     * @param height of the array
     * @return The bounding boxes for the partitions
     */
    public static List<BoundingBox> partition(
        final Boolean[][] array, 
        final int width, 
        final int height) {
        
        final Map<Point, Integer> pointMap = new HashMap<>();

        // Find contiguous blocks
        int blockCounter = 0;

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                if (array[i][j]) {
                    final Point   target  = new Point(j, i);
                    final boolean aboveOn = (i != 0) ? array[i - 1][j] : false;
                    final boolean leftOn  = (j != 0) ? array[i][j - 1] : false;

                    if (aboveOn && leftOn) {
                        // In this case, this point is potentially a bridge between two
                        // contiguous blocks, meaning we must connect them.
                        final int blockIndex1 = pointMap.get(new Point(j, i - 1));
                        final int blockIndex2 = pointMap.get(new Point(j - 1, i));
                        final int minBlockIndex = Math.min(blockIndex1, blockIndex2);
                        final int maxBlockIndex = Math.max(blockIndex1, blockIndex2);

                        pointMap.put(target, minBlockIndex);

                        if (minBlockIndex != maxBlockIndex) {
                            pointMap.replaceAll((point, value) -> {
                                if (value == maxBlockIndex) {
                                    return minBlockIndex;
                                }
                                else if (value > maxBlockIndex) {
                                    return value - 1;
                                }
                                else return value;
                            });
                            --blockCounter;
                        }
                    }
                    else if (aboveOn) {
                        pointMap.put(target, pointMap.get(new Point(j, i - 1)));
                    }
                    else if (leftOn) {
                        pointMap.put(target, pointMap.get(new Point(j - 1, i)));
                    }
                    else {
                        // We've discovered a new block.
                        pointMap.put(target, blockCounter);
                        ++blockCounter;
                    }
                }
            }
        }

        // Organize Points
        final List<List<Point>> found = new ArrayList<>(blockCounter);
        for (int i = 0; i < blockCounter; ++i) found.add(new ArrayList<>());
        for (final Entry<Point, Integer> entry : pointMap.entrySet()) {
            found.get(entry.getValue()).add(entry.getKey());
        }

        // Find bounding box of blocks
        final List<BoundingBox> boxes = new ArrayList<>(found.size());

        for (final List<Point> block : found) {
            int minX = width - 1;
            int maxX = 0;
            int minY = height - 1;
            int maxY = 0;

            for (final Point p : block) {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }

            boxes.add(new BoundingBox(minX, minY, maxX - minX + 1, maxY - minY + 1));
        }

        return boxes;
    }
}
