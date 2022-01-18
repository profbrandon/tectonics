package com.tectonics;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.awt.Point;

import com.tectonics.util.*;
import com.tectonics.plates.*;

public class Simulation {
    public static final Console console = new Console();

    public static final Length RUPTURE_THICKNESS = Length.fromKilometers(2.0f);
    public static final float BOUNDARY_THRESHOLD = 0.001f;
    public static final float SPRING_CONSTANT = 0.001f;
    public static final float MAX_INIT_VELOCITY = 0.029f;
    public static final float MANTLE_DENSITY = 4500f;
    public static final float DELTA_T = 0.1f;

    private final WrappedBox mWrappedBox;
    
    private final List<Plate> mPlates;

    private final Graph<Region, Pair<Boolean, Float>> mNeighborGraph;

    public Simulation(final int width, final int height, final int initialPlateCount) {
        mWrappedBox = new WrappedBox(width, height);
        
        console.startProgressBar("Splitting Area", 6);
        mPlates = splitArea(initialPlateCount);
        console.completeProgressBar();

        final List<Region> regions = getRegions();

        console.startProgressBar("Building Neighbor Graph", 10);
        mNeighborGraph = new Graph<>(regions);

        for (int i = 1; i < regions.size(); ++i) {
            final Region r1 = regions.get(i);

            console.postToProgessBar("Checking against region " + i + "...");

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
        console.completeProgressBar();
        
        reEvaluateHeightMaps();

        System.out.println("Finished creating sim");
    }

    public void update() {
        // Update positions and velocities
        final List<Pair<Region, Point>> regionMovements = new ArrayList<>();

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
            
            final Point oldPosition = position.truncate();
            final Point newPosition = region.getPosition().truncate();

            if (!oldPosition.equals(newPosition)) {
                regionMovements.add(new Pair<>(region, Util.subPoints(newPosition, oldPosition)));
            }
        }

        // TODO: Update neighbor graph

        // Handle Rift Zones
        for (final Pair<Region, Point> movement : regionMovements) {
            final Region movedRegion = movement.first;
            final List<Point> shadow = movedRegion.getGlobalShadow(movement.second);
            final List<Region> neighbors = getNeighboringRegions(movedRegion);

            // Fill empty points below the rupture thickness
            for (final Point shadowPoint : shadow) {
                TerrainGeneration.fillEmptyPoint(shadowPoint, mWrappedBox, getNeighboringRegions(shadowPoint));
            }

            final List<Pair<Region, List<Point>>> subdividedShadows = new ArrayList<>();

            for (final Region neighbor : neighbors) {
                final List<Point> subShadow = new ArrayList<>();

                for (final Point shadowPoint : shadow) {
                    if (mWrappedBox.contains(neighbor.getNeighbors(), shadowPoint)) {
                        subShadow.add(shadowPoint);
                    }
                }

                subdividedShadows.add(new Pair<>(neighbor, subShadow));
            }

            // TODO: Handle subdivided shadows
        }

        // Recompute Height Maps
        reEvaluateHeightMaps();
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

    /**
     * @return all of the regions in the simulation
     */
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
            final int rWidth = region.getWidth();
            final int rHeight = region.getHeight();
            final Boolean[][] regionPresent = region.toBooleanArray();

            for (int i = 0; i < rHeight; ++i) {
                for (int j = 0; j < rWidth; ++j) {
                    final Point wrappedGlobal = mWrappedBox.wrap(region.toGlobal(new Point(j, i)));
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
     * @param r1 the first region
     * @param r2 the second region
     * @return whether the two regions are neighbors
     */
    public boolean areNeighbors(final Region r1, final Region r2) {
        if (r1 == r2) return false;

        final BoundingBox box1 = r1.getBoundingBox().expandByOne();
        final BoundingBox box2 = r2.getBoundingBox();
        if (!mWrappedBox.boundingBoxesOverlap(box1, box2)) return false;

        for (final Point globalBound : r1.getGlobalBoundary()) {
            for (final Point globalNeighbor : r2.getGlobalNeighbors()) {
                if (mWrappedBox.pointEquals(globalBound, globalNeighbor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param point the point to retrieve the chunk from
     * @return potentially the chunk at that position
     */
    public Optional<Chunk> getChunk(final Point point) {
        final Optional<Region> maybeRegion = getRegionFromPoint(point);

        if (maybeRegion.isPresent()) {
            final Region region = maybeRegion.get();
            final Optional<Point> global = mWrappedBox.getUnwrapped(region.getBoundingBox(), point);

            if (global.isPresent()) {
                return region.getChunkAt(region.toLocal(global.get()));
            }
        }

        return Optional.empty();
    }

    /**
     * @param point boundary point in global (x,y) coordinate space
     * @return
     */
    public Optional<Region> getRegionFromPoint(final Point point) {
        for (final Region region : getRegions()) {
            final Optional<Point> maybePoint = mWrappedBox.getUnwrapped(region.getBoundingBox(), point);

            if (maybePoint.isPresent()) {
                return Optional.of(region);
            }
        }

        return Optional.empty();
    }

    /**
     * @param point the point to search around
     * @return a list of regions neighboring this point
     */
    public List<Region> getNeighboringRegions(final Point point) {
        final List<Region> regions = new ArrayList<>();
        final List<Point> neighborPoints = mWrappedBox.getNeighbors(point);

        for (final Point neighborPoint : neighborPoints) {
            getRegionFromPoint(neighborPoint).ifPresent(neighborRegion -> regions.add(neighborRegion));
        }

        return regions;
    }

    /**
     * @param region the target region
     * @return the neighbors of the given region
     */
    public List<Region> getNeighboringRegions(final Region region) {
        final List<Region> regions = getRegions();
        final List<Region> neighbors = new ArrayList<>();
        final List<Point> boundary = region.getGlobalBoundary();
        final BoundingBox box1 = region.getBoundingBox().expandByOne();

        // Eliminate regions that could not possibly border

        for (final Region otherRegion : regions) {
            if (otherRegion != region) {
                if (!box1.overlaps(otherRegion.getBoundingBox())) continue;

                final boolean adjacent = otherRegion.getGlobalNeighbors()
                    .stream()
                    .anyMatch(point1 -> 
                        boundary.stream()
                            .anyMatch(point2 -> mWrappedBox.pointEquals(point1, point2))
                    );

                if (adjacent) neighbors.add(otherRegion);
            }
        }

        return neighbors;
    }

    /**
     * Determines whether the region contains the point in the wrapped context.
     * @param region the region
     * @param point the point in global coordinates
     * @return whether the region contains the point
     */
    public boolean contains(final Region region, final Point point) {
        final Point local = region.toLocal(point);
        return mWrappedBox.getNonWrappedDuplicates(point).stream().anyMatch(wrapped -> {
            return region.contains(local);
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

    /**
     * Re-evaluates the height maps
     */
    private void reEvaluateHeightMaps() {
        float totalDisplacement = 0.0f;

        for (final Region region : getRegions()) {
            totalDisplacement += region.reEvaluateHeightMap(MANTLE_DENSITY);
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

        console.updateProgressBar("Initializing boolean array");

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                alreadyGenerated[i][j] = false;
            }
        }

        console.updateProgressBar("Generating initial plate points");

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

        console.updateProgressBar("Collecting plate points");

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

        console.updateProgressBar("Generating chunks");

        final List<Plate> plates = new ArrayList<>();
        final List<List<Chunk>> chunks = TerrainGeneration.generateChunks(
            (int) (width / 50.0),
            (int) (height / 50.0),
            50,
            Length.fromKilometers(0.5f).toMeters(),
            Length.fromKilometers(4.0f).toMeters());

        console.updateProgressBar("Building regions");

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
            final Vec position = initRegion.getPosition();
            initRegion.setPosition(Vec.sum(position, new Vec(0.49f, 0.49f)));

            final Vec initVelocity = Vec.randomDirection(MAX_INIT_VELOCITY * (float) Math.random() + 0.001f);
            final List<Region> regions = new ArrayList<>();

            for (final Region region : initRegion.divide()) {
                region.setVelocity(Vec.sum(initVelocity, Vec.ZERO));
                regions.add(region);
            }

            plates.add(new Plate(regions));
        }

        return plates;
    }
}
