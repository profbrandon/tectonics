import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Point;

public class BoolArrayUtil {
    
    public static boolean isMinimumSize(final Boolean[][] array, final int width, final int height) {
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

    public static boolean isContiguous(final Boolean[][] array, final int width, final int height) {
        return partition(array, width, height).size() == 1;
    }

    public static List<BoundingBox> partition(final Boolean[][] array, final int width, final int height) {
        // TODO: Rewrite this function to be more efficient. Note that for finding the blocks, we need only
        //       be looking at (width) points at a time
        final List<List<Point>> found = new ArrayList<>();

        // Find contiguous blocks
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                if (array[i][j]) {
                    final Point target = new Point(j, i);

                    final List<List<Point>> neighbors = found.stream().filter((block) -> {
                        return block.stream().filter((point) -> {
                            return Util.areNeighbors(point, target);
                        }).count() != 0;
                    }).collect(Collectors.toList());

                    found.removeAll(neighbors);

                    final List<Point> unionBlock = new ArrayList<>();
                    unionBlock.add(target);

                    for (final List<Point> block : neighbors) {
                        unionBlock.addAll(block);
                    }

                    found.add(unionBlock);
                }
            }
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
