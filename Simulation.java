

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.awt.Point;


public class Simulation {
    public static final Length RUPTURE_THICKNESS = Length.fromKilometers(1.0f);
    public static final float BOUNDARY_THRESHOLD = 0.0001f;
    public static final float SPRING_CONSTANT = 0.01f;
    public static final float DELTA_T = 0.01f;

    private final WrappedBox mWrappedBox;
    
    private final List<Plate> mPlates;

    private final Graph<Region, Pair<Boolean, Float>> mNeighborGraph;

    public Simulation(final int width, final int height, final int initialPlateCount) {
        mWrappedBox = new WrappedBox(width, height);
        mPlates = splitArea(initialPlateCount);

        final List<Region> regions = getRegions();

        mNeighborGraph = new Graph<>(regions);

        for (int i = 1; i < regions.size(); ++i) {
            final Region r1 = regions.get(i);

            for (int j = 0; j < i; ++j) {
                final Region r2 = regions.get(j);

                if (areNeighbors(r1, r2)) {
                    final Vec c1 = r1.getCentroid();
                    final Vec c2 = r2.getCentroid();

                    final boolean onSamePlate = getPlateFromRegion(r1) == getPlateFromRegion(r2);

                    mNeighborGraph.addEdge(j, i, new Pair<>(onSamePlate, mWrappedBox.distance(c1, c2)));
                }
            }
        }

        reEvaluateHeightMaps(3500f);
    }

    public void update() {
        // Update positions and velocities
        for (final Region region : getRegions()) {
            final int index0 = mNeighborGraph.getIndex(region);
            final Vec c0 = region.getCentroid();
            final List<Integer> neighbors = mNeighborGraph.getNeighbors(index0);

            Vec acceleration = Vec.ZERO;

            for (final Integer index1 : neighbors) {
                final Region neighbor = mNeighborGraph.getNode(index1).get();
                final Vec c1 = neighbor.getCentroid();
                final Pair<Boolean, Float> value = mNeighborGraph.getEdgeValue(index0, index1).get();
                final float base = value.second;
                final float actual = mWrappedBox.distance(c0, c1);
                final float delta = value.first ? actual - base : Math.max(0, actual - base);

                final Vec c10 = mWrappedBox.getNonWrappedDuplicates(c1).stream().min((a, b) -> {
                    return Float.compare(Vec.sum(a, c0.negate()).len(), Vec.sum(b, c0.negate()).len());
                }).get();

                final Vec a = Vec.scale(Vec.sum(c10, c0.negate()).normal(), SPRING_CONSTANT * delta);

                acceleration = Vec.sum(acceleration, a);
            }

            final Vec position = region.getPosition();
            region.setPosition(mWrappedBox.wrap(Vec.sum(position, Vec.scale(region.getVelocity(), DELTA_T))));
            region.setVelocity(Vec.sum(region.getVelocity(), Vec.scale(acceleration, DELTA_T)));
        }

        // Handle Rift Zones
        for (final Point empty : getEmptyPoints()) {
            
            Util.choose(List.of(
                new Pair<>(1.0f, (Util.Procedure) () -> {
                    final List<Region> regions = getNeighboringRegions(empty);
                    final Region selected = regions.get((int) (Math.random() * regions.size()));
                    final Point offset = selected.getPosition().negate().truncate();
                    final Chunk chunk = new Chunk();
                    chunk.deposit(new Chunk.Layer(
                        Chunk.RockType.BASALT,
                        Length.fromKilometers((float) (4.0f + 0.5 * Math.random())).toMeters()));
                    
                    // TODO: Resize region when a chunk is added.
                    //final Point target = mWrappedBox.getNonWrappedDuplicates(empty).stream().filter(point -> 
                    //    selected.contains(Util.sumPoints(point, offset))).findFirst().get();
                        
                    //System.out.println("Here");
                    final Point local = Util.sumPoints(empty, offset);
                    
                    selected.setChunk(local.x, local.y, chunk);
                })));
        }
    
        // Handle Collision Zones
        final int width = mWrappedBox.getWidth();
        final int height = mWrappedBox.getHeight();

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                final Set<Region> overlappingRegions = new HashSet<>(4);

                for (final Region region : getRegions()) {
                    if (contains(region, new Point(j, i))) {
                        overlappingRegions.add(region);
                    }
                }

                final Region chosen = overlappingRegions.stream()
                    .collect(Collectors.toList()).get((int) (overlappingRegions.size() * Math.random()));

                for (final Region region : overlappingRegions) {
                    if (region != chosen) {
                        final Point offset = region.getPosition().negate().truncate();
                        final Point local = Util.sumPoints(new Point(j, i), offset);
                        region.removeChunk(local.x, local.y);
                    }
                }
            }
        }

        // Recompute Height Maps
        reEvaluateHeightMaps(3500f);
    }

    public WrappedBox getWrappedBox() {
        return mWrappedBox;
    }

    public Graph<Region, Pair<Boolean, Float>> getGraph() {
        return mNeighborGraph;
    }
    
    public Plate getPlateFromRegion(final Region region) {
        for (final Plate plate : mPlates) {
            if (plate.contains(region)) {
                return plate;
            }
        }
        return null;
    }

    public List<Region> getRegions() {
        final List<Region> regions = new ArrayList<>();
        for (final Plate plate : mPlates) {
            regions.addAll(plate.getRegions());
        }
        return regions;
    }

    /**
     * @return a boolean array representing where there are chunks present
     */
    public Boolean[][] toBooleanArray() {
        final int width = mWrappedBox.getWidth();
        final int height = mWrappedBox.getHeight();

        final Boolean[][] isPresent = new Boolean[height][width];

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                isPresent[i][j] = false;
            }
        }

        for (final Region region : getRegions()) {
            final int rWidth = region.getDimX();
            final int rHeight = region.getDimY();
            final Point offset = region.getPosition().truncate();
            final Boolean[][] regionPresent = region.toBooleanArray();

            for (int i = 0; i < rHeight; ++i) {
                for (int j = 0; j < rWidth; ++j) {
                    final Point wrappedGlobal = mWrappedBox.wrap(Util.sumPoints(new Point(j, i), offset));
                    isPresent[wrappedGlobal.y][wrappedGlobal.x] |= regionPresent[i][j];
                }
            }
        }

        return isPresent;
    }

    /**
     * @return the collection of points that do not currently contain chunks
     */
    public List<Point> getEmptyPoints() {
        final int width = mWrappedBox.getWidth();
        final int height = mWrappedBox.getHeight();
        final Boolean[][] isPresent = toBooleanArray();
        final List<Point> emptyPoints = new ArrayList<>();

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                if (!isPresent[i][j]) {
                    emptyPoints.add(new Point(j, i));
                }
            }
        }

        return emptyPoints;
    }


    /**
     * Determines if the two region's bounding boxes overlap.
     * @param r1 the first region
     * @param r2 the second region
     * @return whether the two regions have overlapping bounding boxes.
     */
    public boolean boundingBoxesOverlap(final Region r1, final Region r2) {
        return mWrappedBox.boundingBoxesOverlap(r1.getBoundingBox(), r2.getBoundingBox());
    }

    /**
     * @param r1 the first region
     * @param r2 the second region
     * @return whether the two regions are neighbors.
     */
    public boolean areNeighbors(final Region r1, final Region r2) {
        if (r1 == r2) return false;

        final Point offset = r1.getPosition().truncate();

        for (final Point target : r1.getBoundary()) {
            final Point adjusted = Util.sumPoints(target, offset);

            for (final Point neighbor : mWrappedBox.getNeighbors(adjusted)) {
                if (contains(r2, neighbor)) {
                    return true;
                }
            }
        }
        return false;
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

                if (mWrappedBox.getNonWrappedDuplicates(transformed).stream().anyMatch(
                    duplicate -> region.contains(duplicate))) {

                    return Optional.of(region);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * @param point the point to search around
     * @return a list of regions neighboring this point
     */
    public List<Region> getNeighboringRegions(final Point point) {
        return mWrappedBox.getNeighbors(point).stream()
            .map(this::getRegionFromPoint)
            .filter(opt -> opt.isPresent())
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * Determines whether the region contains the point in the wrapped context.
     * @param region the region
     * @param point the point in global coordinates
     * @return whether the region contains the point
     */
    public boolean contains(final Region region, final Point point) {
        final Point offset = region.getPosition().negate().truncate();
        return mWrappedBox.getNonWrappedDuplicates(point).stream().anyMatch(wrapped -> {
            return region.contains(Util.sumPoints(wrapped, offset));
        });
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

    private void reEvaluateHeightMaps(final float mantleDensity) {
        float totalDisplacement = 0.0f;

        for (final Region region : getRegions()) {
            totalDisplacement += region.reEvaluateHeightMap(mantleDensity);
        }

        final float chunkWidth = Chunk.WIDTH_IN_KM.toKilometers();
        final float liftHeight = totalDisplacement / (mWrappedBox.getArea() * chunkWidth * chunkWidth) * 1000f;

        for (final Region region : getRegions()) {
            region.lift(liftHeight);
        }
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
            final Vec initVelocity = Vec.randomDirection(0.029f * (float) Math.random() + 0.001f);
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
