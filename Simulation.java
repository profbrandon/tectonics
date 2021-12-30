

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.awt.Point;


public class Simulation {

    public static final float BOUNDARY_THRESHOLD = 0.00001f;
    public static final float DELTA_T = 1.0f;

    private final WrappedBox mWrappedBox;
    
    private final List<Plate> mPlates;

    public Simulation(final int width, final int height, final int initialPlateCount) {
        mWrappedBox = new WrappedBox(width, height);
        mPlates = splitArea(initialPlateCount);
    }

    public WrappedBox getWrappedBox() {
        return mWrappedBox;
    }

    public List<Region> getRegions() {
        final List<Region> regions = new ArrayList<>();
        for (final Plate plate : mPlates) {
            regions.addAll(plate.getRegions());
        }
        return regions;
    }

    public void update() {
        for (Region region : getRegions()) {
            final Vec position = region.getPosition();
            region.setPosition(mWrappedBox.wrap(Vec.sum(position, Vec.scale(region.getVelocity(), DELTA_T))));
        }
    }

    /**
     * Determines if the two region's bounding boxes overlap.
     * @param r1 the first region
     * @param r2 the second region
     * @return
     */
    public boolean boundingBoxesOverlap(final Region r1, final Region r2) {
        return mWrappedBox.boundingBoxesOverlap(r1.getBoundingBox(), r2.getBoundingBox());
    }

    /**
     * @param point boundary point in global (x,y) coordinate space
     * @return
     */
    public Optional<Region> getRegionFromPoint(final Point point) {
        for (final Region region : getRegions()) {
            final BoundingBox box = region.getBoundingBox();

            if (mWrappedBox.withinBoundingBox(box, point)) {
                final Point offset = region.getPosition().truncate();
                final Point transformed = Util.subPoints(point, offset);

                if (transformed.x < 0 && transformed.y < 0) continue;

                if (mWrappedBox.getNonWrappedDuplicates(transformed).stream().anyMatch(
                    duplicate -> region.contains(duplicate))) {

                    return Optional.of(region);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * @param region the region to examine
     * @return the boundary with extra classifications for boundary type
     */
    public List<Pair<Point, Region.BoundaryType>> getClassifiedBoundary(final Region region) {
        final List<Pair<Point, Region.BoundaryType>> classified = new ArrayList<>();

        final Vec offsetVelocity = region.getVelocity().negate();
        final Vec offsetPosition = region.getPosition().negate();

        for (final Point target : region.getBoundary()) {
            Region.BoundaryType type = Region.BoundaryType.STATIONARY;

            final List<Point> neighbors = mWrappedBox.get8Neighbors(target)
                .stream()
                .filter(neighbor -> !region.contains(neighbor))
                .collect(Collectors.toList());

            final Vec averagePos = Vec.sum(
                Vec.extend(target).negate(), 
                Vec.scale(Vec.extend(neighbors.stream().reduce(new Point(), Util::sumPoints)), 1f / neighbors.size()));

            Vec averageVel = Vec.ZERO;
            int regionCount = 0;

            for (final Point neighbor : neighbors) {
                final Point globalNeighbor = mWrappedBox.wrap(Vec.sum(offsetPosition.negate(), Vec.extend(neighbor)).truncate());
                final Optional<Region> maybeRegion = getRegionFromPoint(globalNeighbor);

                if (maybeRegion.isPresent()) {
                    averageVel = Vec.sum(averageVel, maybeRegion.get().getVelocity());
                    ++regionCount;
                }
            }

            averageVel = Vec.scale(averageVel, 1f / (float) regionCount);

            final Vec relativeVelocity = Vec.sum(averageVel, offsetVelocity);
            final Vec relativePosition = averagePos;

            final float lateralIndicator = Vec.project(relativeVelocity.independent(), relativePosition);
            final float axialIndicator = Vec.project(relativeVelocity, relativePosition);

            if (axialIndicator < -BOUNDARY_THRESHOLD) type = Region.BoundaryType.CONVERGENT;
            else if (axialIndicator > BOUNDARY_THRESHOLD) type = Region.BoundaryType.DIVERGENT;
            else if (Math.abs(lateralIndicator) > BOUNDARY_THRESHOLD) type = Region.BoundaryType.TRANSFORM;

            classified.add(new Pair<>(target, type));
        }

        return classified;
    }

    /**
     * @param width of the area to split
     * @param height of the area to split
     * @return the list of plates
     */
    private List<Plate> splitArea(final int plateCount) {
        assert plateCount > 1;
        
        final Map<Integer, List<Point>> pointGroups = new HashMap<>();
        final Map<Integer, List<Point>> possibleGroups = new HashMap<>();

        final int width  = mWrappedBox.getWidth();
        final int height = mWrappedBox.getHeight();

        final Boolean[][] alreadyGenerated = new Boolean[height][width];

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                alreadyGenerated[i][j] = false;
            }
        }

        for (int i = 0; i < plateCount; ++i) {
            final List<Point> points = new ArrayList<>();
            int pointX = 0;
            int pointY = 0;

            // Only look at fresh points
            do {
                pointX = (int) (Math.random() * width);
                pointY = (int) (Math.random() * height);
            }
            while(alreadyGenerated[pointY][pointX]);

            final Point p = new Point(pointX, pointY);
            System.out.println("Plate " + i + " has initial point " + p);

            points.add(p);
            pointGroups.put(i, points);
            alreadyGenerated[p.y][p.x] = true;

            // Remove already found points
            final Collection<Point> neighbors = mWrappedBox.getNeighbors(p);
            for (final List<Point> group : pointGroups.values()) {
                neighbors.removeAll(group);
            }

            possibleGroups.put(i, neighbors.stream().collect(Collectors.toList()));
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
                    mWrappedBox.getNeighbors(chosen)
                        .stream()
                        .filter(point -> {
                            return !alreadyGenerated[point.y][point.x];
                        })
                        .collect(Collectors.toList()));
            }
        }

        final List<Plate> plates = new ArrayList<>();
        final List<List<Chunk>> chunks = Util.generateChunks(
            (int) (width / 50.0),
            (int) (height / 50.0),
            50,
            Length.fromKilometers(4.0f).toMeters(),
            Length.fromKilometers(10.0f).toMeters());

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

            final Region initRegion = Region.buildRegion(Util.mask(chunks, isPresent), Vec.ZERO);
            final Vec initVelocity = Vec.randomDirection(0.09f * (float) Math.random() + 0.01f);
            final List<Region> regions = new ArrayList<>();

            for (final Region region : initRegion.divide()) {
                region.setVelocity(Vec.sum(initVelocity, Vec.ZERO)); // Vec.randomDirection(0.001f * (float) Math.random())));
                regions.add(region);
            }

            plates.add(new Plate(regions));
        }

        return plates;
    }
}
