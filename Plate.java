
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.Point;



public class Plate {
    
    private final List<Pair<Point, Region>> mRegions;

    public Plate() {
        mRegions = new ArrayList<>();
    }

    public Plate(final Region region) {
        mRegions = new ArrayList<>();
        mRegions.add(new Pair<>(new Point(), region));
    }

    public Plate(final List<Pair<Point, Region>> regions) {
        mRegions = regions;
    }

    public List<Pair<Point, Region>> getRegions() {
        return mRegions;
    }

    /**
     * @param width of the area to split
     * @param height of the area to split
     * @return the list of plates
     */
    public static List<Plate> splitArea(final int width, final int height, final int plateCount) {
        assert plateCount > 1;
        
        final Map<Integer, List<Point>> pointGroups = new HashMap<>();
        final Map<Integer, List<Point>> possibleGroups = new HashMap<>();

        final Boolean[][] alreadyGenerated = new Boolean[height][width];

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                alreadyGenerated[i][j] = false;
            }
        }

        for (int i = 0; i < plateCount; ++i) {
            final List<Point> points = new ArrayList<>();
            final int pointX = (int) (Math.random() * width);
            final int pointY = (int) (Math.random() * height);

            final Point p = new Point(pointX, pointY);

            System.out.println("Plate " + i + " has initial point " + p);

            points.add(p);
            pointGroups.put(i, points);
            alreadyGenerated[p.y][p.x] = true;
            // TODO: Detect immediate neighbors
            possibleGroups.put(i, Util.getWrappedNeighbors(p, new Point(), new Point(width - 1, height - 1)));
        }

        while (possibleGroups.values().stream().filter(list -> !list.isEmpty()).count() != 0) {
            for (int i = 0; i < plateCount; ++i) {
                final List<Point> possible = possibleGroups.get(i);

                if(possible.size() == 0) continue;

                final Point chosen = possible.get((int) (Math.random() * possible.size()));

                pointGroups.get(i).add(chosen);
                alreadyGenerated[chosen.y][chosen.x] = true;

                for (final List<Point> points : possibleGroups.values()) {
                    points.remove(chosen);
                }

                possible.addAll(
                    Util.getWrappedNeighbors(chosen, new Point(), new Point(width - 1, height - 1))
                        .stream()
                        .filter(point -> {
                            return !alreadyGenerated[point.y][point.x];
                        })
                        .collect(Collectors.toList()));
            }
        }

        final List<Plate> plates = new ArrayList<>();

        for (final List<Point> points : pointGroups.values()) {
            final Boolean[][] isPresent = new Boolean[height][width];

            for (int i = 0; i < height; ++i) {
                for (int j = 0; j < width; ++j) {
                    isPresent[i][j] = false;
                }
            }

            for (final Point point : points) {
                isPresent[point.y][point.x] = true;
            }

            final Region region = new Region(isPresent, width, height);
            
            plates.add(new Plate(region.divide()));
        }

        return plates;
    }
}
